/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.jus.trf2.assijus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.json.JSONException;
import org.json.JSONObject;

import com.crivano.restservlet.RestUtils;

//HELLO (Browser, Exe -> Site): Identificação do CPF e do tipo de APP, logo depois da conexão
//BYE (Browser, Exe -> Site): Logo antes de interromper a conexão
//START (Browser -> Exe): Solicitação para iniciar lote de assinaturas
//CANCEL (Browser -> Exe): Solicitação de interromper o lote corrente
//STARTED (Exe -> Browser): Confirmação do início do lote
//PROGRESS (Exe -> Browser): Status do lote
//SIGNED (Exe -> Browser): Status de uma assinatura
//FINISHED (Exe -> Browser): Conclusão do lote

@ServerEndpoint("/websocket/server")
public class WebSocketImpl {
	private static Set<IdentifiedSession> clients = Collections
			.synchronizedSet(new HashSet<IdentifiedSession>());

	private class IdentifiedSession implements Comparable<IdentifiedSession> {
		Session session;
		String cpf;
		String app;

		public IdentifiedSession(Session session) {
			this.session = session;
		}

		@Override
		public int compareTo(IdentifiedSession other) {
			return session.getId().compareTo(other.session.getId());
		}
	}

	@OnMessage
	public String msg(Session session, String payload) throws Exception {
		try {
			System.out.println("WebSocket message received: '" + payload + "'");
			JSONObject json = new JSONObject(payload);
			String kind = json.optString("kind", "NULL");
			try {
				String resp = processMessage(session, payload, kind, json);
				System.out
						.println("WebSocket response sent:    '" + resp + "'");
				return resp;
			} catch (Exception ex) {
				json.put("errormsg", ex.getMessage());
				String s = json.toString();
				System.out.println("WebSocket exception sent:   '" + s + "'");
				return s;
			}
		} catch (Exception ex) {
			String s = "{\"errormsg\":\"" + ex.getMessage() + "\"}";
			System.out.println("WebSocket exception sent:   '" + s + "'");
			return s;
		}
	}

	private String processMessage(Session session, String payload, String kind,
			JSONObject json) throws Exception, JSONException, IOException {
		IdentifiedSession is = getIS(session);
		if (is == null)
			throw new Exception("Sessão não localizada.");

		switch (kind) {
		case "HELLO":
			String cert = json.optString("certificate", null);
			if (cert != null) {
				// Call bluc-server certificate webservice
				JSONObject blucreq = new JSONObject();
				blucreq.put("certificate", cert);
				JSONObject blucresp = RestUtils.restPost("bluc-certificate",
						null, Utils.getUrlBluCServer() + "/certificate",
						blucreq);
				is.cpf = blucresp.getString("cpf");
			}
			is.app = json.optString("app", null);
			if (is.app != null && is.cpf != null) {
				// removeOtherSessions(is);
				return ("{\"kind\":\"HELLO_RESP\", \"status\":\"OK\", \"msg\":\"CPF registrado.\"}");
			} else
				throw new Exception(
						"Não foi possível registrar o CPF e o tipo de aplicação.");
		case "PING":
		case "PONG":
			List<IdentifiedSession> list = getISs(is.cpf,
					"signer".equals(is.app) ? "browser" : "signer");
			if (list.size() == 0)
				throw new Exception("Não foi possível encaminhar PING/PONG.");
			for (IdentifiedSession other : list) {
				try {
					other.session.getBasicRemote().sendText(payload);
				} catch (Exception ex) {
					System.out.println("WebSocket exception while sending:'"
							+ ex.getMessage() + "'");
				}
			}
			break;
		case "BYE":
			break;
		case "START":
		case "CANCEL":
			IdentifiedSession isExe = getIS(is.cpf, "signer");
			if (isExe == null) {
				return ("{\"kind\":\"FAILED\", \"response\": {\"errormsg\":\"Assijus.Exe não está conectado\", \"errordetails\":[{\"context\":\"notificar assijus.exe\"}]}}");
			}
			try {
				isExe.session.getBasicRemote().sendText(payload);
			} catch (Exception ex) {
				return ("{\"kind\":\"FAILED\", \"response\": {\"errormsg\":\""
						+ ex.getMessage() + "\", \"errordetails\":[{\"context\":\"notificar assijus.exe\"}]}}");
			}
			break;
		case "STARTED":
		case "PROGRESS":
		case "SIGNED":
		case "FINISHED":
		case "FAILED":
			IdentifiedSession isBrowser = getIS(is.cpf, "browser");
			if (isBrowser == null)
				throw new Exception("Site do Assijus não está conectado.");
			isBrowser.session.getBasicRemote().sendText(payload);
			break;
		default:
			return ("{\"errormsg\":\"Tipo de mensagem não reconhecido.\"}");
		}

		return ("{\"kind\":\"" + kind + "_RESP\",\"status\":\"OK\"}");
	}

	private void removeOtherSessions(IdentifiedSession is) {
		List<IdentifiedSession> l = new ArrayList<>();
		for (IdentifiedSession s : clients)
			if (s != is && is.cpf.equals(s.cpf) && is.app.equals(s.app))
				l.add(s);
		clients.removeAll(l);
	}

	private IdentifiedSession getIS(Session session) {
		for (IdentifiedSession is : clients) {
			if (session.getId().equals(is.session.getId()))
				return is;
		}
		return null;
	}

	private IdentifiedSession getIS(String cpf, String app) {
		for (IdentifiedSession is : clients) {
			if (cpf.equals(is.cpf) && app.equals(is.app))
				return is;
		}
		return null;
	}

	private List<IdentifiedSession> getISs(String cpf, String app) {
		List<IdentifiedSession> list = new ArrayList<>();
		for (IdentifiedSession is : clients) {
			if (cpf.equals(is.cpf) && app.equals(is.app))
				list.add(is);
		}
		return list;
	}

	@OnOpen
	public void onOpen(Session session) {
		System.out.println("WebSocket opened: " + session.getId());
		clients.add(new IdentifiedSession(session));
		System.out.println(clients.size());
		for (IdentifiedSession s : clients) {
			System.out.println(s.cpf + " - " + s.app);
		}
	}

	@OnClose
	public void onClose(Session session, CloseReason reason) {
		System.out.println("Closing a WebSocket due to "
				+ reason.getReasonPhrase());

		IdentifiedSession is = getIS(session);
		clients.remove(is);
		System.out.println(clients.size());
		for (IdentifiedSession s : clients) {
			System.out.println(s.cpf + " - " + s.app);
		}
	}
}

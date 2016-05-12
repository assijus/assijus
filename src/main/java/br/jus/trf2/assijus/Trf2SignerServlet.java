package br.jus.trf2.assijus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class Trf2SignerServlet extends HttpServlet {
	protected String urlapolo;
	protected String urlsiga;
	protected String urlblucserver;

	@Override
	protected void doOptions(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		JSONObject resp = new JSONObject();
		try {
			resp.put("status", "OK");
			Utils.writeJsonResp(response, resp, getContext());
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	};

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		try {

			JSONObject req = Utils.getJsonReq(request, getContext());
			urlapolo = Utils.getUrlBaseApolo();
			urlsiga = Utils.getUrlBaseSigaDoc();
			urlblucserver = Utils.getUrlBaseBluCServer();

			JSONObject resp = new JSONObject();
			run(request, response, req, resp);

			Utils.writeJsonResp(response, resp, getContext());

		} catch (Exception e) {
			Utils.writeJsonError(response, e, getContext());
		}
	}

	abstract protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception;

	abstract protected String getContext();
}

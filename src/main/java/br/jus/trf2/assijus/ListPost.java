package br.jus.trf2.assijus;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.RestUtils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;

public class ListPost implements IRestAction {

	@Override
	public void run(HttpServletRequest request, HttpServletResponse response,
			JSONObject req, final JSONObject resp) throws Exception {
		// Parse certificate
		String certificate = req.getString("certificate");
		JSONObject blucreq = new JSONObject();
		blucreq.put("certificate", certificate);

		String token = req.getString("token");
		String cpf = Utils.assertValidToken(token, Utils.getUrlBluCServer());

		// Call bluc-server hash webservice
		// JSONObject blucresp = RestUtils.getJsonObjectFromJsonPost(new URL(
		// urlblucserver + "/certificate"), blucreq, "bluc-certificate");
		// String cpf = blucresp.getString("cpf");

		final JSONArray arrtmp = new JSONArray();

		String[] systems = Utils.getSystems();

		final CountDownLatch responseWaiter = new CountDownLatch(systems.length);
		Map<String, Future<HttpResponse<JsonNode>>> map = new HashMap<>();

		// Call Each System
		for (String system : systems) {
			final String context = system.replace("signer", "");
			String urlsys = Utils.getUrl(system);
			JSONObject reqsys = new JSONObject();
			reqsys.put("cpf", cpf);
			reqsys.put("urlapi", urlsys);
			reqsys.put("password", Utils.getPassword(system));
			Future<HttpResponse<JsonNode>> future = RestUtils
					.getJsonObjectFromJsonGetAsync(
							new URL(urlsys + "/doc/list"), reqsys, system
									+ "-list");
			map.put(system, future);
		}

		for (String system : systems) {
			final String context = system.replace("signer", "");
			try {
				HttpResponse<JsonNode> futureresponse = map.get(system).get();
				JSONObject o = futureresponse.getBody().getObject();
				String error = o.optString("error", null);
				if (error != null) {
					resp.put("status-" + context, "Error");
					resp.put("error-" + context, error);
					resp.put("stacktrace-" + context,
							o.optString("stacktrace", null));
					continue;
				}
				resp.put("status-" + context, "OK");
				for (int i = 0; i < o.getJSONArray("list").length(); i++) {
					JSONObject doc = o.getJSONArray("list").getJSONObject(i);
					arrtmp.put(doc);
				}
			} catch (Exception ex) {
				resp.put("status-" + context, "Error");
				resp.put("error-" + context, RestUtils.messageAsString(ex));
				resp.put("stacktrace-" + context, RestUtils.stackAsString(ex));
			}
		}

		// for (int i = 0; i < 25; i++) {
		// responseWaiter.await(1, TimeUnit.SECONDS);
		// boolean completed = true;
		// for (String system : systems) {
		// if (!map.get(system).isDone())
		// completed = false;
		// }
		// if (completed)
		// break;
		// }

		// Produce response
		JSONArray arr = new JSONArray();
		resp.put("list", arr);

		for (int i = 0; i < arrtmp.length(); i++) {
			JSONObject o = arrtmp.getJSONObject(i);
			String id = o.getString("id");
			String code = o.getString("code");
			String descr = o.optString("descr", "<nenhuma>");
			String kind = o.optString("kind");
			String origin = o.optString("origin");
			String urlView = o.optString("urlView", null);
			String urlHash = o.getString("urlHash");
			String urlSave = o.optString("urlSave", null);
			String status = o.optString("status", null);

			JSONObject doc = new JSONObject();
			doc.put("id", id);
			doc.put("code", code);
			doc.put("descr", descr);
			doc.put("kind", kind);
			doc.put("origin", origin);
			if (urlView != null) {
				doc.put("urlView", urlView);
				// Acrescenta essa informação na tabela para permitir a
				// posterior visualização.
				Utils.cacheStore(cpf + "-" + urlView, new byte[] { 1 });
			}
			doc.put("urlHash", urlHash);
			Utils.cacheStore(cpf + "-" + urlHash, new byte[] { 1 });
			if (urlSave != null) {
				doc.put("urlSave", urlSave);
				Utils.cacheStore(cpf + "-" + urlSave, new byte[] { 1 });
			}
			doc.put("status", status);
			// for (int j = 0; j < 50; j++)
			arr.put(doc);

		}
	}

	@Override
	public String getContext() {
		return "listar documentos";
	}
}

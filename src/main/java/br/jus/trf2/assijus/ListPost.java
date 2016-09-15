package br.jus.trf2.assijus;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.RestAsyncResponse;
import com.crivano.restservlet.RestUtils;

public class ListPost implements IRestAction {

	@Override
	public void run(JSONObject req, final JSONObject resp) throws Exception {
		// Parse certificate
		String certificate = req.getString("certificate");
		JSONObject blucreq = new JSONObject();
		blucreq.put("certificate", certificate);

		String listkey = req.optString("key", null);

		String authkey = req.getString("authkey");
		String cpf = Utils
				.assertValidAuthKey(authkey, Utils.getUrlBluCServer());

		final JSONArray arrtmp = new JSONArray();

		if (listkey != null) {
			// Read list from cache
			String payload = null;
			if (listkey != null)
				payload = Utils.dbRetrieve(listkey, false);

			JSONObject o = new JSONObject(payload);
			for (int i = 0; i < o.getJSONArray("list").length(); i++) {
				JSONObject doc = o.getJSONArray("list").getJSONObject(i);
				arrtmp.put(doc);
			}
		} else {
			// Read list from connected systems
			String[] systems = Utils.getSystems();

			final CountDownLatch responseWaiter = new CountDownLatch(
					systems.length);
			Map<String, Future<RestAsyncResponse>> map = new HashMap<>();

			// Call Each System
			for (String system : systems) {
				final String context = system.replace("signer", "");
				String urlsys = Utils.getUrl(system);
				JSONObject reqsys = new JSONObject();
				reqsys.put("cpf", cpf);
				reqsys.put("urlapi", urlsys);
				if (system.equals("sigadocsigner"))
					reqsys.put("password", Utils.getPassword(system));
				Future<RestAsyncResponse> future = RestUtils.restGetAsync(
						system + "-list", Utils.getPassword(system), urlsys
								+ "/doc/list", reqsys);
				map.put(system, future);
			}

			for (String system : systems) {
				final String context = system.replace("signer", "");
				try {
					RestAsyncResponse futureresponse = map.get(system).get();
					JSONObject o = futureresponse.getJSONObject();
					String error = o.optString("errormsg", null);
					if (error == null) // Nato: Remover isso quando a nova
										// versão do
										// Siga-Doc for para a produção
						error = o.optString("error", null);
					if (error != null) {
						resp.put("status-" + context, "Error");
						resp.put("errormsg-" + context, error);
						resp.put("stacktrace-" + context,
								o.optString("stacktrace", null));
						continue;
					}
					resp.put("status-" + context, "OK");
					for (int i = 0; i < o.getJSONArray("list").length(); i++) {
						JSONObject doc = o.getJSONArray("list")
								.getJSONObject(i);
						doc.put("system", system);
						arrtmp.put(doc);
					}
				} catch (Exception ex) {
					resp.put("status-" + context, "Error");
					resp.put("errormsg-" + context,
							RestUtils.messageAsString(ex));
					resp.put("stacktrace-" + context,
							RestUtils.stackAsString(ex));
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
		}

		// Produce response
		JSONArray arr = new JSONArray();
		resp.put("list", arr);

		for (int i = 0; i < arrtmp.length(); i++) {
			JSONObject o = arrtmp.getJSONObject(i);
			String id = o.getString("id");
			String code = o.getString("code");
			String descr = o.optString("descr", "<nenhuma>");
			String kind = o.optString("kind");
			String system = o.getString("system");
			String origin = o.optString("origin");
			String status = o.optString("status", null);
			String extra = o.optString("extra", null);

			JSONObject doc = new JSONObject();
			doc.put("id", id);
			doc.put("code", code);
			doc.put("descr", descr);
			doc.put("kind", kind);
			doc.put("system", system);
			doc.put("origin", origin);
			// Acrescenta essa informação na tabela para permitir a
			// posterior visualização.
			Utils.cacheStore(cpf + "-" + system + "-" + id, new byte[] { 1 });
			if (extra != null) {
				doc.put("extra", extra);
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

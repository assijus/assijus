package br.jus.trf2.assijus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import org.json.JSONArray;
import org.json.JSONObject;

import br.jus.trf2.assijus.IAssijus.Document;
import br.jus.trf2.assijus.IAssijus.IListPost;
import br.jus.trf2.assijus.IAssijus.ListPostRequest;
import br.jus.trf2.assijus.IAssijus.ListPostResponse;
import br.jus.trf2.assijus.IAssijus.ListStatus;

import com.crivano.restservlet.RestAsyncResponse;
import com.crivano.restservlet.RestUtils;
import com.crivano.swaggerservlet.SwaggerUtils;

public class ListPost implements IListPost {

	@Override
	public void run(ListPostRequest req, ListPostResponse resp)
			throws Exception {
		String certificate = SwaggerUtils.base64Encode(req.certificate);

		JSONObject blucreq = new JSONObject();
		blucreq.put("certificate", certificate);

		String listkey = req.key;

		String authkey = req.authkey;
		String cpf = Utils
				.assertValidAuthKey(authkey, Utils.getUrlBluCServer());

		final JSONArray arrtmp = new JSONArray();

		resp.status = new ArrayList<IAssijus.ListStatus>();

		if (listkey != null) {
			// Read list from cache
			String payload = null;
			if (listkey != null)
				payload = RestUtils.dbRetrieve(listkey, false);

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
					ListStatus ls = new ListStatus();
					ls.system = system;
					resp.status.add(ls);
					if (error != null) {
						ls.errormsg = error;
						JSONArray details = o.optJSONArray("errordetails");
						if (details != null && details.length() > 0)
							ls.stacktrace = details.getJSONObject(0).optString(
									"stacktrace", null);
						continue;
					}
					for (int i = 0; i < o.getJSONArray("list").length(); i++) {
						JSONObject doc = o.getJSONArray("list")
								.getJSONObject(i);
						doc.put("system", system);
						arrtmp.put(doc);
					}
				} catch (Exception ex) {
					ListStatus ls = new ListStatus();
					ls.system = system;
					ls.errormsg = RestUtils.messageAsString(ex);
					ls.stacktrace = RestUtils.stackAsString(ex);
					resp.status.add(ls);
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
		resp.list = new ArrayList<IAssijus.Document>();

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

			Document doc = new Document();
			doc.id = id;
			doc.code = code;
			doc.descr = descr;
			doc.kind = kind;
			doc.system = system;
			doc.origin = origin;
			doc.extra = extra;

			// Acrescenta essa informação na tabela para permitir a
			// posterior visualização.
			RestUtils.memCacheStore(cpf + "-" + system + "-" + id,
					new byte[] { 1 });
			// for (int j = 0; j < 50; j++)
			resp.list.add(doc);
		}
	}

	@Override
	public String getContext() {
		return "listar documentos";
	}

}

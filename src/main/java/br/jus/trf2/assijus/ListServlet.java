package br.jus.trf2.assijus;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import com.crivano.restservlet.RestAsyncCallback;
import com.crivano.restservlet.RestUtils;

@SuppressWarnings("serial")
public class ListServlet extends AssijusServlet {

	@Override
	protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, final JSONObject resp)
			throws Exception {
		// Parse certificate
		String certificate = req.getString("certificate");
		JSONObject blucreq = new JSONObject();
		blucreq.put("certificate", certificate);

		String token = req.getString("token");
		String cpf = Utils.assertValidToken(token, urlblucserver);

		// Call bluc-server hash webservice
		// JSONObject blucresp = RestUtils.getJsonObjectFromJsonPost(new URL(
		// urlblucserver + "/certificate"), blucreq, "bluc-certificate");
		// String cpf = blucresp.getString("cpf");

		final JSONArray arrtmp = new JSONArray();
		final CountDownLatch responseWaiter = new CountDownLatch(3);

		// Call Siga
		JSONObject reqsiga = new JSONObject();
		reqsiga.put("cpf", cpf);
		reqsiga.put("urlapi", urlsiga);
		reqsiga.put("password", Utils.getSigaDocPassword());
		Future futureSiga = RestUtils.getJsonObjectFromJsonPostAsync(new URL(
				urlsiga + "/list"), reqsiga, "siga-list",
				new RestAsyncCallback() {

					@Override
					public void completed(JSONObject obj) throws Exception {
						try {
							for (int i = 0; i < obj.getJSONArray("list")
									.length(); i++) {
								JSONObject o = obj.getJSONArray("list")
										.getJSONObject(i);
								synchronized (arrtmp) {
									arrtmp.put(o);
								}
							}
						} finally {
							responseWaiter.countDown();
						}
					}

					@Override
					public void failed(Exception ex) throws Exception {
						try {
							resp.put("error-sigadoc",
									RestUtils.messageAsString(ex));
							resp.put("stacktrace-sigadoc",
									RestUtils.stackAsString(ex));
						} finally {
							responseWaiter.countDown();
						}
					}

					@Override
					public void cancelled() {
						responseWaiter.countDown();
					}
				});

		// Call Apolo
		JSONObject reqapolo = new JSONObject();
		reqapolo.put("cpf", cpf);
		reqapolo.put("password", Utils.getApoloPassword());
		Future futureApolo = RestUtils.getJsonObjectFromJsonGetAsync(new URL(
				urlapolo + "/doc/list"), reqapolo, "apolo-list",
				new RestAsyncCallback() {

					@Override
					public void completed(JSONObject obj) throws Exception {
						try {
							for (int i = 0; i < obj.getJSONArray("list")
									.length(); i++) {
								JSONObject o = obj.getJSONArray("list")
										.getJSONObject(i);
								synchronized (arrtmp) {
									arrtmp.put(o);
								}
							}
						} finally {
							responseWaiter.countDown();
						}
					}

					@Override
					public void failed(Exception ex) throws Exception {
						try {
							resp.put("error-apolo",
									RestUtils.messageAsString(ex));
							resp.put("stacktrace-apolo",
									RestUtils.stackAsString(ex));
						} finally {
							responseWaiter.countDown();
						}
					}

					@Override
					public void cancelled() {
						responseWaiter.countDown();
					}
				});

		// Call TextoWeb
		JSONObject reqtextoweb = new JSONObject();
		reqapolo.put("cpf", cpf);
		reqapolo.put("password", Utils.getTextoWebPassword());
		Future futureTextoWeb = RestUtils.getJsonObjectFromJsonGetAsync(
				new URL(urltextoweb + "/doc/list"), reqapolo, "textoweb-list",
				new RestAsyncCallback() {

					@Override
					public void completed(JSONObject obj) throws Exception {
						try {
							for (int i = 0; i < obj.getJSONArray("list")
									.length(); i++) {
								JSONObject o = obj.getJSONArray("list")
										.getJSONObject(i);
								synchronized (arrtmp) {
									arrtmp.put(o);
								}
							}
						} finally {
							responseWaiter.countDown();
						}
					}

					@Override
					public void failed(Exception ex) throws Exception {
						try {
							resp.put("error-textoweb",
									RestUtils.messageAsString(ex));
							resp.put("stacktrace-textoweb",
									RestUtils.stackAsString(ex));
						} finally {
							responseWaiter.countDown();
						}
					}

					@Override
					public void cancelled() {
						responseWaiter.countDown();
					}
				});

		responseWaiter.await();

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
		System.out.println("retornou");

	}

	@Override
	protected String getContext() {
		return "listar documentos";
	}
}

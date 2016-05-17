package br.jus.trf2.assijus;

import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import br.jus.trf2.restservlet.RestAsyncCallback;
import br.jus.trf2.restservlet.RestUtils;

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
		Utils.assertValidToken(token, urlblucserver);

		// Call bluc-server hash webservice
		JSONObject blucresp = RestUtils.getJsonObjectFromJsonPost(new URL(
				urlblucserver + "/certificate"), blucreq, "bluc-certificate");
		req.put("certificate", certificate);
		req.put("subject", blucresp.get("subject"));
		req.put("cn", blucresp.get("cn"));
		req.put("name", blucresp.get("name"));
		req.put("cpf", blucresp.get("cpf"));
		req.put("certdetails", blucresp.get("certdetails"));

		final JSONArray arrtmp = new JSONArray();
		final CountDownLatch responseWaiter = new CountDownLatch(2);

		// Call Siga
		JSONObject reqsiga = new JSONObject(req, JSONObject.getNames(req));
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
								o.put("status", "Siga-Doc");
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
		JSONObject reqapolo = new JSONObject(req, JSONObject.getNames(req));
		reqapolo.put("urlapi", urlapolo);
		reqapolo.put("password", Utils.getApoloPassword());
		Future futureApolo = RestUtils.getJsonObjectFromJsonPostAsync(new URL(
				urlapolo + "/list"), reqapolo, "apolo-list",
				new RestAsyncCallback() {

					@Override
					public void completed(JSONObject obj) throws Exception {
						try {
							for (int i = 0; i < obj.getJSONArray("list")
									.length(); i++) {
								JSONObject o = obj.getJSONArray("list")
										.getJSONObject(i);
								o.put("status", "Apolo");
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
			if (urlView != null)
				doc.put("urlView", urlView);
			doc.put("urlHash", urlHash);
			if (urlSave != null)
				doc.put("urlSave", urlSave);
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

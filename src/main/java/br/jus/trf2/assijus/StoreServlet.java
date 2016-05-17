package br.jus.trf2.assijus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class StoreServlet extends AssijusServlet {

	private static final String CONTEXT = "store servlet";

	@Override
	protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception {
		// Parse request
		String payload = req.getString("payload");

		// Call
		String key = Utils.dbStore(payload);

		// Produce response
		resp.put("status", "OK");
		resp.put("key", key);
	}

	@Override
	protected String getContext() {
		return "armazenar a assinatura";
	}
}

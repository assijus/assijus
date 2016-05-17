package br.jus.trf2.assijus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class RetrieveServlet extends AssijusServlet {

	private static final String CONTEXT = "store servlet";

	@Override
	protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception {
		// Parse request
		String key = req.getString("key");
		boolean remove = req.optBoolean("remove", false);
		String password = req.optString("password", null);
		
		if (Utils.getRetrievePassword() != null) 
			if (!Utils.getRetrievePassword().equals(password))
				throw new Exception("acesso negado");

		// Call
		String payload = Utils.dbRetrieve(key, remove);
		if (payload != null)
			resp.put("payload", payload);

		// Produce response
		resp.put("status", "OK");
		resp.put("key", key);
		resp.put("remove", remove);
	}

	@Override
	protected String getContext() {
		return "recuperar a assinatura";
	}
}

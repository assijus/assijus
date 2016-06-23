package br.jus.trf2.assijus;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;

public class RetrievePost implements IRestAction {

	@Override
	public void run(JSONObject req, final JSONObject resp) throws Exception {
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
	public String getContext() {
		return "recuperar a assinatura";
	}
}

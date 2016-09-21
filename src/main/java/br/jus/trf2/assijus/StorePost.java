package br.jus.trf2.assijus;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;

public class StorePost implements IRestAction {

	@Override
	public void run(JSONObject req, JSONObject resp) throws Exception {
		// Parse request
		String payload = req.getString("payload");

		// Call
		String key = MemCacheRedis.dbStore(payload);

		// Produce response
		resp.put("status", "OK");
		resp.put("key", key);
	}

	@Override
	public String getContext() {
		return "armazenar a assinatura";
	}
}
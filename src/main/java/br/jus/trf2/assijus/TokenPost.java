package br.jus.trf2.assijus;

import java.util.Date;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;

public class TokenPost implements IRestAction {

	@Override
	public void run(JSONObject req, JSONObject resp) throws Exception {
		String time = Utils.format(new Date());

		// Produce response
		String policy = "AD-RB";
		resp.put("token", "TOKEN-" + time);
		resp.put("policy", policy);
	}

	@Override
	public String getContext() {
		return "obter o token";
	}
}

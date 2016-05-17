package br.jus.trf2.assijus;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

@SuppressWarnings("serial")
public class TokenServlet extends AssijusServlet {

	@Override
	protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception {
		// Parse request
		String certificate = req.optString("certificate", null);

		String time = Utils.format(new Date());

		// Produce response
		String policy = "AD-RB";
		resp.put("token", "TOKEN-" + time);
		resp.put("policy", policy);
	}

	@Override
	protected String getContext() {
		return "obter o token";
	}
}

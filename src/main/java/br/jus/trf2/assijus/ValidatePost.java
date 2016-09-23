package br.jus.trf2.assijus;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.RestUtils;

public class ValidatePost implements IRestAction {

	@Override
	public void run(JSONObject req, final JSONObject resp) throws Exception {
		// Parse request
		String envelope = req.getString("envelope");
		String time = req.getString("time");
		String sha1 = req.getString("sha1");
		String sha256 = req.getString("sha256");

		// Call bluc-server validate webservice. If there is an error,
		// Utils will throw an exception.
		JSONObject blucreq = new JSONObject();
		blucreq.put("envelope", envelope);
		blucreq.put("time", time);
		blucreq.put("sha1", sha1);
		blucreq.put("sha256", sha256);
		blucreq.put("crl", true);
		JSONObject blucresp = RestUtils.restPost("bluc-validate", null,
				Utils.getUrlBluCServer() + "/validate", blucreq);

		String policy = blucresp.getString("policy");
		String policyversion = blucresp.getString("policyversion");
		String policyoid = blucresp.getString("policyoid");
		String cn = blucresp.getString("cn");
		String cpf = blucresp.getJSONObject("certdetails").getString("cpf0");
		String status = blucresp.getString("status");

		resp.put("policy", policy);
		resp.put("policyversion", policyversion);
		resp.put("policyoid", policyoid);
		resp.put("cn", cn);
		resp.put("cpf", cpf);
		resp.put("status", status);
	}

	@Override
	public String getContext() {
		return "validar assinatura";
	}
}

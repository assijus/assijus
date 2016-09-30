package br.jus.trf2.assijus;

import org.json.JSONObject;

import br.jus.trf2.assijus.IAssijus.IValidatePost;
import br.jus.trf2.assijus.IAssijus.ValidatePostRequest;
import br.jus.trf2.assijus.IAssijus.ValidatePostResponse;

import com.crivano.restservlet.RestUtils;
import com.crivano.swaggerservlet.SwaggerUtils;

public class ValidatePost implements IValidatePost {

	@Override
	public void run(ValidatePostRequest req, ValidatePostResponse resp)
			throws Exception {
		// Parse request
		String envelope = SwaggerUtils.base64Encode(req.envelope);
		String time = SwaggerUtils.format(req.time);
		String sha1 = SwaggerUtils.base64Encode(req.sha1);
		String sha256 = SwaggerUtils.base64Encode(req.sha256);

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

		resp.policy = policy;
		resp.policyversion = policyversion;
		resp.policyoid = policyoid;
		resp.cn = cn;
		resp.cpf = cpf;
		resp.status = status;
	}

	@Override
	public String getContext() {
		return "validar assinatura";
	}

}

package br.jus.trf2.assijus;

import br.jus.trf2.assijus.IAssijus.IValidatePost;

import java.util.concurrent.TimeUnit;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

public class ValidatePost implements IValidatePost {

	@Override
	public void run(Request req, Response resp, AssijusContext ctx) throws Exception {
		// Parse request
		String envelope = SwaggerUtils.base64Encode(req.envelope);
		String time = SwaggerUtils.dateAdapter.format(req.time);
		String sha1 = SwaggerUtils.base64Encode(req.sha1);
		String sha256 = SwaggerUtils.base64Encode(req.sha256);

		// Validate: call bluc-server validate webservice. If there is an error,
		// it will throw an exception.
		IBlueCrystal.IValidatePost.Request q = new IBlueCrystal.IValidatePost.Request();
		q.time = SwaggerUtils.dateAdapter.parse(time);
		q.sha1 = SwaggerUtils.base64Decode(sha1);
		q.sha256 = SwaggerUtils.base64Decode(sha256);
		q.crl = true;
		q.envelope = SwaggerUtils.base64Decode(envelope);
		IBlueCrystal.IValidatePost.Response s = SwaggerCall
				.callAsync("bluc-validate", null, "POST", Utils.getUrlBluCServer() + "/validate", q,
						IBlueCrystal.IValidatePost.Response.class)
				.get(AssijusServlet.VALIDATE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();

		String policy = s.policy;
		String policyversion = s.policyversion;
		String policyoid = s.policyoid;
		String cn = s.cn;
		String cpf = s.certdetails.cpf0;
		String status = s.status;

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

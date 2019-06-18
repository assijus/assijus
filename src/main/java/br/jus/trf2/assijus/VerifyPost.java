package br.jus.trf2.assijus;

import java.util.concurrent.TimeUnit;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.IVerifyPost;
import br.jus.trf2.assijus.IAssijus.VerifyPostRequest;
import br.jus.trf2.assijus.IAssijus.VerifyPostResponse;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

public class VerifyPost implements IVerifyPost {

	@Override
	public void run(VerifyPostRequest req, VerifyPostResponse resp) throws Exception {
		String urlHash = Utils.getUrl(req.system) + "/doc/" + req.id + "/hash";

		// Call document repository hash webservice
		IAssijusSystem.DocIdHashGetRequest systemreq = new IAssijusSystem.DocIdHashGetRequest();
		IAssijusSystem.DocIdHashGetResponse systemresp = SwaggerCall
				.callAsync("system-hash", Utils.getPassword(req.system), "GET", urlHash, systemreq,
						IAssijusSystem.DocIdHashGetResponse.class)
				.get(AssijusServlet.SYSTEM_HASH_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();

		String urlSignature = Utils.getUrl(req.system) + "/sign/" + req.ref;

		// Call document repository hash webservice
		IAssijusSystem.SignRefGetRequest systemsignreq = new IAssijusSystem.SignRefGetRequest();
		IAssijusSystem.SignRefGetResponse systemsignresp = SwaggerCall
				.callAsync("system-signature", Utils.getPassword(req.system), "GET", urlSignature, systemreq,
						IAssijusSystem.SignRefGetResponse.class)
				.get(AssijusServlet.SYSTEM_SIGNATURE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();

		// Parse request
		String envelope = SwaggerUtils.base64Encode(systemsignresp.envelope);
		String time = SwaggerUtils.format(systemsignresp.time);
		String sha1 = SwaggerUtils.base64Encode(systemresp.sha1);
		String sha256 = SwaggerUtils.base64Encode(systemresp.sha256);

		// Verify: call bluc-server validate webservice. If there is an error,
		// it will throw an exception.
		IBlueCrystal.ValidatePostRequest q = new IBlueCrystal.ValidatePostRequest();
		q.time = SwaggerUtils.parse(time);
		q.sha1 = SwaggerUtils.base64Decode(sha1);
		q.sha256 = SwaggerUtils.base64Decode(sha256);
		q.crl = true;
		q.envelope = SwaggerUtils.base64Decode(envelope);
		IBlueCrystal.ValidatePostResponse s = SwaggerCall
				.callAsync("bluc-validate", null, "POST", Utils.getUrlBluCServer() + "/validate", q,
						IBlueCrystal.ValidatePostResponse.class)
				.get(AssijusServlet.SYSTEM_SIGNATURE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();

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

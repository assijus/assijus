package br.jus.trf2.assijus;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.IEnvelopePost;

public class EnvelopePost implements IEnvelopePost {
	private static final Logger log = LoggerFactory.getLogger(EnvelopePost.class);

	@Override
	public void run(Request req, Response resp, AssijusContext ctx) throws Exception {
		String certificate = SwaggerUtils.base64Encode(req.certificate);
		String signature = SwaggerUtils.base64Encode(req.signature);
		String time = SwaggerUtils.dateAdapter.format(req.time);
		String policy = req.policy;

		String sha1 = SwaggerUtils.base64Encode(req.sha1);
		String sha256 = SwaggerUtils.base64Encode(req.sha256);

		if (signature == null)
			throw new Exception("Não foi possível obter o parâmetro signature.");

		// Parse certificate
		IBlueCrystal.CertificatePostRequest q = new IBlueCrystal.CertificatePostRequest();
		q.certificate = SwaggerUtils.base64Decode(certificate);
		IBlueCrystal.CertificatePostResponse s = SwaggerCall
				.callAsync("bluc-certificate", null, "POST", Utils.getUrlBluCServer() + "/certificate", q,
						IBlueCrystal.CertificatePostResponse.class)
				.get(AssijusServlet.CERTIFICATE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();
		String subject = s.subject;
		String cn = s.cn;
		String name = s.name;
		String cpf = s.cpf;

		// Build envelope
		String envelope = null;
		if (!"PKCS7".equals(policy)) {
			IBlueCrystal.EnvelopePostRequest q2 = new IBlueCrystal.EnvelopePostRequest();
			q2.certificate = SwaggerUtils.base64Decode(certificate);
			q2.time = SwaggerUtils.dateAdapter.parse(time);
			q2.policy = policy;
			q2.sha1 = SwaggerUtils.base64Decode(sha1);
			q2.sha256 = SwaggerUtils.base64Decode(sha256);
			q2.crl = true;
			q2.signature = SwaggerUtils.base64Decode(signature);
			IBlueCrystal.EnvelopePostResponse s2 = SwaggerCall
					.callAsync("bluc-envelope", null, "POST", Utils.getUrlBluCServer() + "/envelope", q2,
							IBlueCrystal.EnvelopePostResponse.class)
					.get(AssijusServlet.ENVELOPE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();
			envelope = SwaggerUtils.base64Encode(s2.envelope);
		} else {
			envelope = signature;
		}

		// Validate: call bluc-server validate webservice. If there is an error,
		// it will throw an exception.
		IBlueCrystal.ValidatePostRequest q3 = new IBlueCrystal.ValidatePostRequest();
		q3.time = SwaggerUtils.dateAdapter.parse(time);
		q3.sha1 = SwaggerUtils.base64Decode(sha1);
		q3.sha256 = SwaggerUtils.base64Decode(sha256);
		q3.crl = true;
		q3.envelope = SwaggerUtils.base64Decode(envelope);
		IBlueCrystal.ValidatePostResponse s3 = SwaggerCall
				.callAsync("bluc-validate", null, "POST", Utils.getUrlBluCServer() + "/validate", q3,
						IBlueCrystal.ValidatePostResponse.class)
				.get(AssijusServlet.VALIDATE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();

		// Return the envelope
		resp.envelope = SwaggerUtils.base64Decode(envelope);
		resp.time = SwaggerUtils.dateAdapter.parse(time);
		resp.name = name;
		resp.cpf = cpf;
		resp.sha1 = SwaggerUtils.base64Decode(sha1);
		resp.sha256 = SwaggerUtils.base64Decode(sha256);
	}

	@Override
	public String getContext() {
		return "produzir o envelope";
	}

}

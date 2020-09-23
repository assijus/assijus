package br.jus.trf2.assijus;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.ISavePost;
import br.jus.trf2.assijus.IAssijus.SavePostRequest;
import br.jus.trf2.assijus.IAssijus.SavePostResponse;
import br.jus.trf2.assijus.IAssijus.Warning;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

public class SavePost implements ISavePost {
	private static final Logger log = LoggerFactory.getLogger(SavePost.class);

	@Override
	public void run(SavePostRequest req, SavePostResponse resp) throws Exception {

		String code = req.code;
		String system = req.system;
		String id = req.id;
		String password = Utils.getPassword(system);

		String certificate = SwaggerUtils.base64Encode(req.certificate);
		String signature = SwaggerUtils.base64Encode(req.signature);
		String time = SwaggerUtils.format(req.time);
		String policy = req.policy;

		String sha1 = SwaggerUtils.base64Encode(req.sha1);
		String sha256 = SwaggerUtils.base64Encode(req.sha256);

		String extra = req.extra;

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
		// Test if we received a full envelope or just the signature
		if (signature.length() < 2000) {
			IBlueCrystal.EnvelopePostRequest q2 = new IBlueCrystal.EnvelopePostRequest();
			q2.certificate = SwaggerUtils.base64Decode(certificate);
			q2.time = SwaggerUtils.parse(time);
			q2.policy = policy;
			q2.sha1 = SwaggerUtils.base64Decode(sha1);
			q2.sha256 = SwaggerUtils.base64Decode(sha256);
			q2.crl = true;
			q2.signature = SwaggerUtils.base64Decode(signature);
			if ("PKCS7".equals(q2.policy))
				q2.policy = "PKCS#7";
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
		q3.time = SwaggerUtils.parse(time);
		q3.sha1 = SwaggerUtils.base64Decode(sha1);
		q3.sha256 = SwaggerUtils.base64Decode(sha256);
		q3.crl = true;
		q3.envelope = SwaggerUtils.base64Decode(envelope);
		IBlueCrystal.ValidatePostResponse s3 = SwaggerCall
				.callAsync("bluc-validate", null, "POST", Utils.getUrlBluCServer() + "/validate", q3,
						IBlueCrystal.ValidatePostResponse.class)
				.get(AssijusServlet.VALIDATE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();

		// Store the signature
		IAssijusSystem.DocIdSignPutRequest q4 = new IAssijusSystem.DocIdSignPutRequest();
		q4.envelope = SwaggerUtils.base64Decode(envelope);
		q4.time = SwaggerUtils.parse(time);
		q4.name = name;
		q4.cpf = cpf;
		q4.sha1 = SwaggerUtils.base64Decode(sha1);
		q4.extra = extra;
		String urlSave = Utils.getUrl(system) + "/doc/" + id + "/sign";
		IAssijusSystem.DocIdSignPutResponse s4;
		try {
			s4 = SwaggerCall
				.callAsync("system-save", password, "PUT", urlSave, q4, IAssijusSystem.DocIdSignPutResponse.class)
				.get(AssijusServlet.SYSTEM_SAVE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();
		} catch (Exception ex) {
			throw new PresentableException("Problema reportado por " + system + ": " + ex.getMessage(), ex);
		}
		
		// Produce response
		resp.warning = new ArrayList<IAssijus.Warning>();
		if (s4.warning != null) {
			for (int i = 0; i < s4.warning.size(); i++) {
				Warning warning = new Warning();
				warning.label = s4.warning.get(i).label;
				warning.description = s4.warning.get(i).description;
				resp.warning.add(warning);
			}

		}
		if ("PKCS7".equals(policy)) {
			Warning warning = new Warning();
			warning.label = "p7";
			warning.description = "Assinatura no padrão PKCS#7.";
			resp.warning.add(warning);
		}
		if (resp.warning.size() == 0)
			resp.warning = null;

		resp.status = s4.status;

		log.info("*** Assinatura: " + name + ", " + system + ", " + code + ", sha1:" + sha1 + ", sha256:" + sha256 + ", " + resp.status);
	}

	@Override
	public String getContext() {
		return "gravar a assinatura";
	}

}

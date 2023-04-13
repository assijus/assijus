package br.jus.trf2.assijus;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.IHashPost;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

public class HashPost implements IHashPost {

	@Override
	public void run(Request req, Response resp, AssijusContext ctx) throws Exception {
		String certificate = SwaggerUtils.base64Encode(req.certificate);
		String system = req.system;
		String password = Utils.getPassword(system);
		String id = req.id;

		String authkey = req.authkey;
		String cpf = Utils.assertValidAuthKey(authkey, Utils.getUrlBluCServer()).cpf;

		String urlHash = Utils.getUrl(system) + "/doc/" + id + "/hash";
		String time = SwaggerUtils.dateAdapter.format(new Date());

		// Call document repository hash webservice
		IAssijusSystem.IDocIdHashGet.Request systemreq = new IAssijusSystem.IDocIdHashGet.Request();
		systemreq.cpf = cpf;
		IAssijusSystem.IDocIdHashGet.Response systemresp;
		try {
			systemresp = SwaggerCall
					.callAsync("system-hash", password, "GET", urlHash, systemreq,
							IAssijusSystem.IDocIdHashGet.Response.class)
					.get(AssijusServlet.SYSTEM_HASH_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();
		} catch (Exception ex) {
			throw new PresentableException("Problema reportado por " + system + ": " + ex.getMessage(), ex);
		}

		// Produce response

		String doc = SwaggerUtils.base64Encode(systemresp.doc);
		String sha1 = SwaggerUtils.base64Encode(systemresp.sha1);
		String sha256 = SwaggerUtils.base64Encode(systemresp.sha256);
		String policy = systemresp.policy;

		if (policy == null && sha256 != null)
			policy = "AD-RB";
		if (policy != null && !"PKCS7".equals(policy)) {
			IBlueCrystal.IHashPost.Request q = new IBlueCrystal.IHashPost.Request();
			q.certificate = SwaggerUtils.base64Decode(certificate);
			q.time = SwaggerUtils.dateAdapter.parse(time);
			q.policy = policy;
			q.sha1 = SwaggerUtils.base64Decode(sha1);
			q.sha256 = SwaggerUtils.base64Decode(sha256);
			q.crl = true;
			IBlueCrystal.IHashPost.Response s = SwaggerCall
					.callAsync("bluc-hash", null, "POST", Utils.getUrlBluCServer() + "/hash", q,
							IBlueCrystal.IHashPost.Response.class)
					.get(AssijusServlet.HASH_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();
			resp.hash = s.hash;
			if (req.digest != null && req.digest)
				resp.hash = Utils.calcSha256(resp.hash);
			resp.policyversion = s.policyversion;
			resp.policy = s.policy;
		} else {
			if (req.digest != null && req.digest) {
				if (systemresp.sha1 == null)
					throw new Exception(
							"Para realizar assinaturas sem política, é necessário informar sha1 do documento, na propriedade 'sha1'.");
				resp.hash = systemresp.sha1;
			} else {
				if (doc == null)
					throw new Exception(
							"Para realizar assinaturas sem política, é necessário informar o conteúdo do documento, codificado em Base64, na propriedade 'doc'.");

				if (systemresp.secret != null) {
					if (!Utils.makeSecret(systemresp.secret).equals(req.secret))
						throw new PresentableException("Não autorizado.");
				} else {
					if (SwaggerUtils.memCacheRetrieve(cpf + "-" + system + "-" + id) == null)
						throw new PresentableException("CPF não autorizado.");
				}
				resp.hash = SwaggerUtils.base64Decode(doc);
			}
			resp.policy = "PKCS7";
		}
		resp.time = SwaggerUtils.dateAdapter.parse(time);
		resp.sha1 = SwaggerUtils.base64Decode(sha1);
		resp.sha256 = SwaggerUtils.base64Decode(sha256);

		resp.extra = systemresp.extra;
	}

	@Override
	public String getContext() {
		return "obter o hash";
	}

}

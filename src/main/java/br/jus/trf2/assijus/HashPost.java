package br.jus.trf2.assijus;

import java.util.Date;

import br.jus.trf2.assijus.IAssijus.HashPostRequest;
import br.jus.trf2.assijus.IAssijus.HashPostResponse;
import br.jus.trf2.assijus.IAssijus.IHashPost;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

public class HashPost implements IHashPost {

	@Override
	public void run(HashPostRequest req, HashPostResponse resp) throws Exception {
		String certificate = SwaggerUtils.base64Encode(req.certificate);
		String system = req.system;
		String password = Utils.getPassword(system);
		String id = req.id;

		String authkey = req.authkey;
		String cpf = Utils.assertValidAuthKey(authkey, Utils.getUrlBluCServer());

		String urlHash = Utils.getUrl(system) + "/doc/" + id + "/hash";
		String time = Utils.format(new Date());

		// Call document repository hash webservice
		IAssijusSystem.DocIdHashGetRequest systemreq = new IAssijusSystem.DocIdHashGetRequest();
		systemreq.cpf = cpf;
		IAssijusSystem.DocIdHashGetResponse systemresp = SwaggerCall.call("system-hash", password, "GET", urlHash,
				systemreq, IAssijusSystem.DocIdHashGetResponse.class);

		// Produce response

		String doc = SwaggerUtils.base64Encode(systemresp.doc);
		String sha1 = SwaggerUtils.base64Encode(systemresp.sha1);
		String sha256 = SwaggerUtils.base64Encode(systemresp.sha256);
		String policy = systemresp.policy;

		if (policy == null && sha256 != null)
			policy = "AD-RB";
		if (policy != null && !"PKCS7".equals(policy)) {
			IBlueCrystal.HashPostRequest q = new IBlueCrystal.HashPostRequest();
			q.certificate = SwaggerUtils.base64Decode(certificate);
			q.time = SwaggerUtils.parse(time);
			q.policy = policy;
			q.sha1 = SwaggerUtils.base64Decode(sha1);
			q.sha256 = SwaggerUtils.base64Decode(sha256);
			q.crl = true;
			IBlueCrystal.HashPostResponse s = SwaggerCall.call("bluc-hash", null, "POST",
					Utils.getUrlBluCServer() + "/hash", q, IBlueCrystal.HashPostResponse.class);
			resp.hash = s.hash;
			resp.policyversion = s.policyversion;
			resp.policy = s.policy;
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
			resp.policy = "PKCS7";
		}
		resp.time = SwaggerUtils.parse(time);
		resp.sha1 = SwaggerUtils.base64Decode(sha1);
		resp.sha256 = SwaggerUtils.base64Decode(sha256);

		resp.extra = systemresp.extra;
	}

	@Override
	public String getContext() {
		return "obter o hash";
	}

}

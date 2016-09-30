package br.jus.trf2.assijus;

import java.util.Date;

import org.json.JSONObject;

import br.jus.trf2.assijus.IAssijus.HashPostRequest;
import br.jus.trf2.assijus.IAssijus.HashPostResponse;
import br.jus.trf2.assijus.IAssijus.IHashPost;

import com.crivano.restservlet.PresentableException;
import com.crivano.restservlet.RestUtils;
import com.crivano.swaggerservlet.SwaggerUtils;

public class HashPost implements IHashPost {

	@Override
	public void run(HashPostRequest req, HashPostResponse resp)
			throws Exception {
		String certificate = SwaggerUtils.base64Encode(req.certificate);
		String system = req.system;
		String password = Utils.getPassword(system);
		String id = req.id;

		String authkey = req.authkey;
		String cpf = Utils
				.assertValidAuthKey(authkey, Utils.getUrlBluCServer());

		if (RestUtils.memCacheRetrieve(cpf + "-" + system + "-" + id) == null)
			throw new PresentableException("CPF não autorizado.");

		String urlHash = Utils.getUrl(system) + "/doc/" + id + "/hash";
		String time = Utils.format(new Date());

		// Call document repository hash webservice
		JSONObject gedresp = null;
		gedresp = RestUtils.restGet("ged-hash", password, urlHash, "cpf", cpf);

		// Produce response

		String doc = gedresp.optString("doc", null);
		String sha1 = gedresp.optString("sha1", null);
		String sha256 = gedresp.optString("sha256", null);

		String policy = gedresp.optString("policy", null);
		if (policy == null && sha256 != null)
			policy = "AD-RB";
		if (policy != null && !"PKCS7".equals(policy)) {
			JSONObject blucreq = new JSONObject();
			blucreq.put("certificate", certificate);
			blucreq.put("time", time);
			blucreq.put("policy", policy);
			blucreq.put("sha1", sha1);
			blucreq.put("sha256", sha256);
			blucreq.put("crl", true);

			// Call bluc-server hash webservice
			JSONObject blucresp = RestUtils.restPost("bluc-hash", null,
					Utils.getUrlBluCServer() + "/hash", blucreq);

			String hash = blucresp.getString("hash");
			String hashPolicyVersion = blucresp.getString("policyversion");
			String hashPolicy = blucresp.getString("policy");

			resp.hash = SwaggerUtils.base64Decode(hash);
			resp.policyversion = hashPolicyVersion;
			resp.policy = hashPolicy;
		} else {
			if (doc == null)
				throw new Exception(
						"Para realizar assinaturas sem política, é necessário informar o conteúdo do documento, codificado em Base64, na propriedade 'doc'.");
			resp.hash = SwaggerUtils.base64Decode(doc);
			resp.policy = "PKCS7";
		}
		resp.time = SwaggerUtils.parse(time);
		resp.sha1 = SwaggerUtils.base64Decode(sha1);
		resp.sha256 = SwaggerUtils.base64Decode(sha256);

		String extra = gedresp.optString("extra", null);
		if (extra != null) {
			resp.extra = extra;
		}

	}

	@Override
	public String getContext() {
		return "obter o hash";
	}

}

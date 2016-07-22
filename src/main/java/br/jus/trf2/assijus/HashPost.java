package br.jus.trf2.assijus;

import java.util.Date;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.PresentableException;
import com.crivano.restservlet.RestUtils;

public class HashPost implements IRestAction {

	@Override
	public void run(JSONObject req, final JSONObject resp) throws Exception {
		// Parse request
		String certificate = req.getString("certificate");
		String urlHash = req.getString("urlHash");
		String system = Utils.chooseSystem(urlHash);
		String password = Utils.choosePassword(urlHash);

		String token = req.getString("token");
		String cpf = Utils.assertValidToken(token, Utils.getUrlBluCServer());

		if (Utils.cacheRetrieve(cpf + "-" + urlHash) == null)
			throw new PresentableException("CPF não autorizado.");

		urlHash = Utils.fixUrl(urlHash);
		String time = Utils.format(new Date());

		// Call document repository hash webservice
		JSONObject gedresp = null;
		if ("sigadocsigner".equals(system))
			gedresp = RestUtils.restGet("ged-hash", password, urlHash, "cpf",
					cpf, "password", password);
		else
			gedresp = RestUtils.restGet("ged-hash", password, urlHash, "cpf",
					cpf);

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

			resp.put("hash", hash);
			resp.put("policyversion", hashPolicyVersion);
			resp.put("policy", hashPolicy);
			resp.put("certificate", certificate);
			resp.put("time", time);
			resp.put("sha1", sha1);
			resp.put("sha256", sha256);
		} else {
			if (doc == null)
				throw new Exception(
						"Para realizar assinaturas sem política, é necessário informar o conteúdo do documento, codificado em Base64, na propriedade 'doc'.");
			resp.put("time", time);
			resp.put("hash", doc);
			resp.put("policy", "PKCS7");
			resp.put("sha1", sha1);
			resp.put("sha256", sha256);
		}

		String urlSave = gedresp.optString("urlSave", null);
		if (urlSave != null) {
			resp.put("urlSave", urlSave);
			Utils.cacheStore(cpf + "-" + urlSave, new byte[] { 1 });
		}

	}

	@Override
	public String getContext() {
		return "obter o hash";
	}
}

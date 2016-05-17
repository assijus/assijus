package br.jus.trf2.assijus;

import java.net.URL;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import br.jus.trf2.restservlet.RestServlet;
import br.jus.trf2.restservlet.RestUtils;

@SuppressWarnings("serial")
public class HashServlet extends AssijusServlet {

	@Override
	protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception {
		// Parse request
		String certificate = req.getString("certificate");
		String urlHash = req.getString("urlHash");
		String password = Utils.choosePassword(urlHash);
		urlHash = Utils.fixUrl(urlHash);

		String token = req.getString("token");
		Utils.assertValidToken(token, urlblucserver);

		String time = Utils.format(new Date());

		JSONObject gedreq = new JSONObject();
		// restreq.put("policy", policy);
		gedreq.put("certificate", certificate);
		gedreq.put("time", time);

		if (urlHash.startsWith(urlapolo))
			gedreq.put("urlapi", urlapolo);
		if (urlHash.startsWith(urlsiga))
			gedreq.put("urlapi", urlsiga);

		gedreq.put("password", password);

		// Call document repository hash webservice
		JSONObject gedresp = RestUtils.getJsonObjectFromJsonPost(new URL(
				urlHash), gedreq, "ged-hash");

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
			JSONObject blucresp = RestUtils.getJsonObjectFromJsonPost(new URL(
					urlblucserver + "/hash"), blucreq, "bluc-hash");

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
		resp.put("urlSave", gedresp.optString("urlSave", null));
	}

	@Override
	protected String getContext() {
		return "obter o hash";
	}
}

package br.jus.trf2.assijus;

import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import br.jus.trf2.restservlet.RestUtils;

@SuppressWarnings("serial")
public class SaveServlet extends AssijusServlet {

	private static final String CONTEXT = "save servlet";

	@Override
	protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, JSONObject resp)
			throws Exception {
		// Parse request

		String urlSave = req.getString("urlSave");
		String password = Utils.choosePassword(urlSave);
		urlSave = Utils.fixUrl(urlSave);

		String certificate = req.getString("certificate");
		String signature = req.optString("signature", null);
		String signkey = req.optString("signkey", null);
		String time = req.getString("time");
		String policy = req.getString("policy");

		String sha1 = req.getString("sha1");
		String sha256 = req.getString("sha256");

		if (signature == null && signkey != null)
			signature = Utils.dbRetrieve(signkey, true);

		if (signature == null && Utils.getKeyValueServer() != null) {
			// Parse certificate
			JSONObject kvreq = new JSONObject();
			kvreq.put("key", signkey);
			kvreq.put("remove", true);
			if (Utils.getKeyValuePassword() != null)
				kvreq.put("password", Utils.getKeyValuePassword());

			// Call bluc-server hash webservice
			JSONObject kvresp = RestUtils.getJsonObjectFromJsonPost(new URL(
					Utils.getKeyValueServer() + "/retrieve"), kvreq,
					"sign-retrieve");
			signature = kvresp.optString("payload", null);
		}

		if (signature == null)
			throw new Exception("Não foi possível obter o parâmetro signature.");

		// Parse certificate
		JSONObject blucreq2 = new JSONObject();
		blucreq2.put("certificate", certificate);

		// Call bluc-server hash webservice
		JSONObject blucresp2 = RestUtils.getJsonObjectFromJsonPost(new URL(
				urlblucserver + "/certificate"), blucreq2, "bluc-certificate");

		String subject = blucresp2.getString("subject");
		String cn = blucresp2.getString("cn");
		String name = blucresp2.getString("name");
		String cpf = blucresp2.getString("cpf");

		String envelope = null;
		JSONObject blucreq = new JSONObject();
		blucreq.put("certificate", certificate);
		blucreq.put("time", time);
		blucreq.put("policy", policy);
		blucreq.put("sha1", sha1);
		blucreq.put("sha256", sha256);
		blucreq.put("crl", true);
		if (!"PKCS7".equals(policy)) {
			blucreq.put("signature", signature);
			// Call bluc-server hash webservice
			JSONObject blucresp = RestUtils.getJsonObjectFromJsonPost(new URL(
					urlblucserver + "/envelope"), blucreq, "bluc-envelope");

			envelope = blucresp.getString("envelope");

			// Call bluc-server validate webservice. If there is an error,
			// Utils will throw an exception.
			blucreq.remove("signature");
		} else {
			envelope = signature;
		}
		blucreq.put("envelope", envelope);

		// Call bluc-server validate webservice. If there is an error,
		// Utils will throw an exception.
		JSONObject blucvalidateresp = RestUtils.getJsonObjectFromJsonPost(
				new URL(urlblucserver + "/validate"), blucreq, "bluc-validate");

		// Call
		JSONObject sigareq = new JSONObject();
		sigareq.put("certificate", certificate);
		sigareq.put("envelope", envelope);
		sigareq.put("time", time);
		sigareq.put("subject", subject);
		sigareq.put("cn", cn);
		sigareq.put("name", name);
		sigareq.put("cpf", cpf);
		sigareq.put("sha1", sha1);
		sigareq.put("sha256", sha256);
		sigareq.put("password", password);

		// Call document repository hash webservice
		JSONObject sigaresp = RestUtils.getJsonObjectFromJsonPost(new URL(
				urlSave), sigareq, "save-signature");

		// Produce response
		JSONArray warning = sigaresp.optJSONArray("warning");
		if ("PKCS7".equals(policy)) {
			if (warning == null)
				warning = new JSONArray();

			JSONObject obj = new JSONObject();
			obj.put("label", "p7");
			obj.put("description", "Assinatura no padrão PKCS#7.");
			warning.put(obj);
		}
		if (warning != null)
			resp.put("warning", warning);

		resp.put("status", sigaresp.optString("status", null));
	}

	@Override
	protected String getContext() {
		return "gravar a assinatura";
	}
}

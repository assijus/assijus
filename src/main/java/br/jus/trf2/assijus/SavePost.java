package br.jus.trf2.assijus;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.RestUtils;

public class SavePost implements IRestAction {
	private static final Logger log = LoggerFactory.getLogger(RestUtils.class);

	@Override
	public void run(JSONObject req, final JSONObject resp) throws Exception {
		// Parse request

		String code = req.getString("code");
		String system = req.getString("system");
		String id = req.getString("id");
		String password = Utils.getPassword(system);

		String certificate = req.getString("certificate");
		String signature = req.optString("signature", null);
		String signkey = req.optString("signkey", null);
		String time = req.getString("time");
		String policy = req.getString("policy");

		String sha1 = req.getString("sha1");
		String sha256 = req.getString("sha256");

		String extra = req.optString("extra", null);

		if (signature == null && signkey != null)
			signature = RestUtils.dbRetrieve(signkey, true);

		if (signature == null)
			throw new Exception("Não foi possível obter o parâmetro signature.");

		// Parse certificate
		JSONObject blucreq2 = new JSONObject();
		blucreq2.put("certificate", certificate);

		// Call bluc-server hash webservice
		JSONObject blucresp2 = RestUtils.restPost("bluc-certificate", null,
				Utils.getUrlBluCServer() + "/certificate", blucreq2);

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
			JSONObject blucresp = RestUtils.restPost("bluc-envelope", null,
					Utils.getUrlBluCServer() + "/envelope", blucreq);

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
		JSONObject blucvalidateresp = RestUtils.restPost("bluc-validate", null,
				Utils.getUrlBluCServer() + "/validate", blucreq);

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
		if (extra != null)
			sigareq.put("extra", extra);

		// Call document repository hash webservice
		String urlSave = Utils.getUrl(system) + "/doc/" + id + "/sign";
		JSONObject sigaresp = RestUtils.restPut("save-signature", password,
				urlSave, sigareq);

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

		String status = sigaresp.optString("status", null);
		resp.put("status", status);

		log.info("*** Assinatura: " + name + ", " + system + ", " + code + ", "
				+ status);
	}

	@Override
	public String getContext() {
		return "gravar a assinatura";
	}
}

package br.jus.trf2.assijus;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.jus.trf2.assijus.IAssijus.ISavePost;
import br.jus.trf2.assijus.IAssijus.SavePostRequest;
import br.jus.trf2.assijus.IAssijus.SavePostResponse;
import br.jus.trf2.assijus.IAssijus.Warning;

import com.crivano.restservlet.RestUtils;
import com.crivano.swaggerservlet.SwaggerUtils;

public class SavePost implements ISavePost {
	private static final Logger log = LoggerFactory.getLogger(RestUtils.class);

	@Override
	public void run(SavePostRequest req, SavePostResponse resp)
			throws Exception {

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
		resp.warning = new ArrayList<IAssijus.Warning>();
		JSONArray receivedwarnings = sigaresp.optJSONArray("warning");
		if (receivedwarnings != null) {
			for (int i = 0; i > receivedwarnings.length(); i++) {
				Warning warning = new Warning();
				JSONObject w = receivedwarnings.getJSONObject(i);
				warning.label = w.getString("label");
				warning.description = w.getString("description");
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

		String status = sigaresp.optString("status", null);
		resp.status = status;

		log.info("*** Assinatura: " + name + ", " + system + ", " + code + ", "
				+ status);
	}

	@Override
	public String getContext() {
		return "gravar a assinatura";
	}

}

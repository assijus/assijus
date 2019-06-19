package br.jus.trf2.assijus;

import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.blucservice.api.IBlueCrystal.ValidatePostResponse;
import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.AuthPostRequest;
import br.jus.trf2.assijus.IAssijus.AuthPostResponse;
import br.jus.trf2.assijus.IAssijus.EnvelopePostRequest;
import br.jus.trf2.assijus.IAssijus.EnvelopePostResponse;
import br.jus.trf2.assijus.IAssijus.IAuthPost;
import br.jus.trf2.assijus.Utils.AuthKeyFields;

public class AuthPost implements IAuthPost {

	@Override
	public void run(AuthPostRequest req, AuthPostResponse resp)
			throws Exception {
		String authkey = req.authkey;
		String token = req.token;
		String payload = null;

		if (authkey != null) {
			payload = SwaggerUtils.dbRetrieve(authkey, false);

			if (payload.startsWith("TOKEN-"))
				// A token is stored
				token = payload;
			else if (payload.startsWith("{")) {
				// A client-cert authentication is stored
				JSONObject json = new JSONObject(payload);
				resp.authkey = authkey;
				resp.name = json.getString("name");
				resp.cpf = json.getString("cpf");
				resp.kind = "client-cert";
				return;
			}
		}
		
		if (req.signature != null && req.certificate != null) {
			EnvelopePost e = new EnvelopePost();
			EnvelopePostRequest ereq = new EnvelopePostRequest();
			EnvelopePostResponse eresp = new EnvelopePostResponse();
			
			ereq.certificate = req.certificate;
			ereq.policy = req.policy;
			ereq.policyversion = req.policyversion;
			byte[] bytes = token.getBytes(StandardCharsets.UTF_8);
			ereq.sha1 = Utils.calcSha1(bytes);
			ereq.sha256 = Utils.calcSha256(bytes);
			ereq.signature = req.signature;
			ereq.time = req.time;
			e.run(ereq, eresp);
			
			// Produce response
			resp.name = eresp.name;
			resp.cpf = eresp.cpf;
			resp.token = token + ";" + SwaggerUtils.base64Encode(eresp.envelope);
			resp.kind = "simply-signed-token";

			String key = SwaggerUtils.dbStore(SwaggerUtils.toJson(resp));
			resp.authkey = key;
			
			return;
		}

		if (token != null) {
			ValidatePostResponse json = Utils.validateToken(token,
					Utils.getUrlBluCServer());
			String cpf = null;
			cpf = json.certdetails.cpf0;

			// Produce response
			resp.name = json.cn;
			resp.cpf = cpf;
			resp.token = token;
			resp.kind = "signed-token";

			String key = SwaggerUtils.dbStore(SwaggerUtils.toJson(resp));
			resp.authkey = key;
			return;
		}

		throw new PresentableException(
				"Não foi possível realizar a autenticação.");
	}

	@Override
	public String getContext() {
		return "autenticar";
	}

}

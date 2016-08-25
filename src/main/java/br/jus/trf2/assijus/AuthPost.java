package br.jus.trf2.assijus;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.PresentableException;
import com.crivano.restservlet.RestUtils;

public class AuthPost implements IRestAction {

	@Override
	public void run(JSONObject req, JSONObject resp) throws Exception {
		String authkey = req.optString("authkey", null);
		String token = req.optString("token", null);
		String payload = null;
	
		if (authkey != null) {
			payload = Utils.dbRetrieve(authkey, false);

			if (payload == null && Utils.getKeyValueServer() != null) {
				// Parse certificate
				JSONObject kvreq = new JSONObject();
				kvreq.put("key", authkey);
				kvreq.put("remove", false);
				kvreq.put("password", Utils.getKeyValuePassword());

				// Call bluc-server hash webservice
				JSONObject kvresp = RestUtils.restPost("sign-retrieve", null,
						Utils.getKeyValueServer() + "/retrieve", kvreq);
				payload = kvresp.optString("payload", null);
			}
			
			if (payload.startsWith("TOKEN-"))
				// A token is stored
				token = payload;
			else if (payload.startsWith("{")) {
				// A client-cert authentication is stored
				JSONObject json = new JSONObject(payload);
				if (Utils.getRetrievePassword() != null)
					if (!Utils.getRetrievePassword().equals(json.get("password")))
						throw new Exception("Senha inválida na autenticação com client-cert");
				resp.put("certificate", json.get("certificate"));
				resp.put("name", json.get("name"));
				resp.put("cpf", json.get("cpf"));
				resp.put("kind", "client-cert");
				return;
			}
		}

		if (token != null) {
			JSONObject json = Utils.validateToken(token, Utils.getUrlBluCServer());
			// Produce response
			resp.put("certificate", json.get("certificate"));
			resp.put("name", json.get("cn"));
			resp.put("token", token);
			resp.put("kind", "signed-token");
			return;
		}
		
		throw new PresentableException(
					"Não foi possível realizar a autenticação. Por favor, lance novamente o aplicativo Assijus.Exe. Se o problema persistir, tente reiniciar o computador");
	}

	@Override
	public String getContext() {
		return "autenticar";
	}
}

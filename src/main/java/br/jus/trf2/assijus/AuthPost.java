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

			if (payload.startsWith("TOKEN-"))
				// A token is stored
				token = payload;
			else if (payload.startsWith("{")) {
				// A client-cert authentication is stored
				JSONObject json = new JSONObject(payload);
				resp.put("certificate", json.get("certificate"));
				resp.put("name", json.get("name"));
				resp.put("cpf", json.get("cpf"));
				resp.put("kind", "client-cert");
				return;
			}
		}

		if (token != null) {
			JSONObject json = Utils.validateToken(token,
					Utils.getUrlBluCServer());
			String cpf = null;
			cpf = json.getJSONObject("certdetails").getString("cpf0");

			// Produce response
			resp.put("certificate", json.get("certificate"));
			resp.put("name", json.get("cn"));
			resp.put("cpf", cpf);
			resp.put("token", token);
			resp.put("kind", "signed-token");

			String stored = resp.toString();
			String key = Utils.dbStore(stored);
			resp.put("authkey", key);
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

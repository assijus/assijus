package br.jus.trf2.assijus;

import org.json.JSONObject;

import com.crivano.restservlet.IRestAction;
import com.crivano.restservlet.PresentableException;
import com.crivano.restservlet.RestUtils;

public class ViewPost implements IRestAction {

	public void run(JSONObject req, final JSONObject resp) throws Exception {
		// Parse request
		String urlView = req.getString("urlView");
		String password = Utils.choosePassword(urlView);

		String token = req.getString("token");
		String cpf = Utils.assertValidToken(token, Utils.getUrlBluCServer());

		if (Utils.cacheRetrieve(cpf + "-" + urlView) == null)
			throw new PresentableException("CPF não autorizado.");

		urlView = Utils.fixUrl(urlView);

		// Call document repository hash webservice
		JSONObject gedresp = RestUtils.restGet("ged-view", password, urlView,
				"cpf", cpf);

		// Produce response
		String doc = gedresp.getString("doc");
		resp.put("payload", doc);
		resp.put("content-type", "application/pdf");
	}

	public String getContext() {
		return "obter o pdf";
	}
}

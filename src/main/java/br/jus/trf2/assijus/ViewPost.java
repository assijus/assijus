package br.jus.trf2.assijus;

import org.json.JSONObject;

import br.jus.trf2.assijus.IAssijus.IViewPost;
import br.jus.trf2.assijus.IAssijus.ViewPostRequest;
import br.jus.trf2.assijus.IAssijus.ViewPostResponse;

import com.crivano.restservlet.PresentableException;
import com.crivano.restservlet.RestUtils;
import com.crivano.swaggerservlet.SwaggerUtils;

public class ViewPost implements IViewPost {

	@Override
	public void run(ViewPostRequest req, ViewPostResponse resp)
			throws Exception {
		String system = req.system;
		String id = req.id;
		String password = Utils.getPassword(system);

		String authkey = req.authkey;
		String cpf = Utils
				.assertValidAuthKey(authkey, Utils.getUrlBluCServer());

		if (RestUtils.memCacheRetrieve(cpf + "-" + system + "-" + id) == null)
			throw new PresentableException("CPF não autorizado.");

		String urlView = Utils.getUrl(system) + "/doc/" + id + "/pdf";
		// Call document repository hash webservice
		JSONObject gedresp = RestUtils.restGet("ged-view", password, urlView,
				"cpf", cpf);

		// Produce response
		String doc = gedresp.getString("doc");
		resp.payload = SwaggerUtils.base64Decode(doc);
		resp.contenttype = "application/pdf";
	}

	public String getContext() {
		return "obter o pdf";
	}

}

package br.jus.trf2.assijus;

import br.jus.trf2.assijus.IAssijus.IViewPost;
import br.jus.trf2.assijus.IAssijus.ViewPostRequest;
import br.jus.trf2.assijus.IAssijus.ViewPostResponse;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

public class ViewPost implements IViewPost {

	@Override
	public void run(ViewPostRequest req, ViewPostResponse resp) throws Exception {
		String system = req.system;
		String id = req.id;
		String password = Utils.getPassword(system);

		String authkey = req.authkey;
		String cpf = Utils.assertValidAuthKey(authkey, Utils.getUrlBluCServer());

		String urlView = Utils.getUrl(system) + "/doc/" + id + "/pdf";

		// Call document repository hash webservice
		IAssijusSystem.DocIdPdfGetRequest q = new IAssijusSystem.DocIdPdfGetRequest();
		q.id = id;
		q.cpf = cpf;
		IAssijusSystem.DocIdPdfGetResponse s = SwaggerCall.call(system + "-get", password, "GET", urlView, q,
				IAssijusSystem.DocIdPdfGetResponse.class);

		if (s.secret != null) {
			if (!Utils.makeSecret(s.secret).equals(req.secret))
				throw new PresentableException("Não autorizado.");
		} else {
			if (SwaggerUtils.memCacheRetrieve(cpf + "-" + system + "-" + id) == null)
				throw new PresentableException("CPF não autorizado.");
		}

		// Produce response
		resp.payload = s.doc;
		resp.contenttype = "application/pdf";
	}

	public String getContext() {
		return "obter o pdf";
	}
}

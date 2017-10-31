package br.jus.trf2.assijus;

import java.io.ByteArrayInputStream;

import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.IViewPost;
import br.jus.trf2.assijus.IAssijus.ViewPostRequest;
import br.jus.trf2.assijus.IAssijus.ViewPostResponse;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

public class ViewPost implements IViewPost {

	@Override
	public void run(ViewPostRequest req, ViewPostResponse resp) throws Exception {
		IAssijusSystem.DocIdPdfGetResponse s = getPdf(req);

		String disposition = "attachment".equals(req.disposition) ? "attachment" : "inline";

		// Produce response
		resp.contentdisposition = disposition + ";filename=" + req.id + ".pdf";
		resp.contentlength = (long) s.doc.length;
		resp.contenttype = "application/pdf";
		resp.inputstream = new ByteArrayInputStream(s.doc);
	}

	public static IAssijusSystem.DocIdPdfGetResponse getPdf(ViewPostRequest req)
			throws Exception, PresentableException {
		String system = req.system;
		String id = req.id;
		String password = Utils.getPassword(system);

		String authkey = req.authkey;
		String cpf = req.cpf;
		if (authkey != null)
			cpf = Utils.assertValidAuthKey(authkey, Utils.getUrlBluCServer()).cpf;

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
			if (authkey == null)
				throw new PresentableException("CPF não foi validado por uma chave de autenticação.");
			if (SwaggerUtils.memCacheRetrieve(cpf + "-" + system + "-" + id) == null)
				throw new PresentableException("CPF não autorizado.");
		}
		return s;
	}

	public String getContext() {
		return "obter o pdf";
	}
}

package br.jus.trf2.assijus;

import java.io.ByteArrayInputStream;

import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;

import br.jus.trf2.assijus.IAssijus.IViewSystemIdSecretGet;
import br.jus.trf2.assijus.IAssijus.ViewSystemIdSecretGetRequest;
import br.jus.trf2.assijus.IAssijus.ViewSystemIdSecretGetResponse;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

public class ViewSystemIdSecretGet implements IViewSystemIdSecretGet {

	@Override
	public void run(ViewSystemIdSecretGetRequest req, ViewSystemIdSecretGetResponse resp) throws Exception {
		IAssijusSystem.DocIdPdfGetResponse s = getPdf(req);

		// Produce response
		resp.contentdisposition = "inline;filename=" + req.id + ".pdf";
		resp.contentlength = (long) s.doc.length;
		resp.contenttype = "application/pdf";
		resp.inputstream = new ByteArrayInputStream(s.doc);
	}

	public static IAssijusSystem.DocIdPdfGetResponse getPdf(ViewSystemIdSecretGetRequest req)
			throws Exception, PresentableException {
		String system = req.system;
		String id = req.id;
		String password = Utils.getPassword(system);

		String urlView = Utils.getUrl(system) + "/doc/" + id + "/pdf";

		// Call document repository hash webservice
		IAssijusSystem.DocIdPdfGetRequest q = new IAssijusSystem.DocIdPdfGetRequest();
		q.id = id;
		IAssijusSystem.DocIdPdfGetResponse s = SwaggerCall.call(system + "-get", password, "GET", urlView, q,
				IAssijusSystem.DocIdPdfGetResponse.class);

		if (s.secret != null) {
			if (req.secret == null)
				throw new Exception("Parâmetro 'secret' precisa ser informado.");

			if (!Utils.makeSecret(s.secret).equals(req.secret)) {
				// Apenas para garantir que não é possível
				// um ataque para tentar descobrir o valor
				// de secret
				Thread.sleep(4000);
				throw new PresentableException("Não autorizado.");
			}
		}
		return s;
	}

	public String getContext() {
		return "obter o pdf";
	}
}

package br.jus.trf2.assijus;

import java.util.concurrent.Future;

import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;

import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerAsyncResponse;
import com.crivano.swaggerservlet.SwaggerCall;

import br.jus.trf2.assijus.IAssijus.IViewSystemIdSecretAuthkeyGet;
import br.jus.trf2.assijus.IAssijus.ViewSystemIdSecretAuthkeyGetRequest;
import br.jus.trf2.assijus.IAssijus.ViewSystemIdSecretAuthkeyGetResponse;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

public class ViewSystemIdSecretAuthkeyGet implements IViewSystemIdSecretAuthkeyGet {

	@Override
	public void run(ViewSystemIdSecretAuthkeyGetRequest req, ViewSystemIdSecretAuthkeyGetResponse resp)
			throws Exception {
		String system = req.system;
		String id = req.id;
		String cpf = Utils.assertValidAuthKey(req.authkey, Utils.getUrlBluCServer()).cpf;

		String password = Utils.getPassword(system);

		String urlView = Utils.getUrl(system) + "/doc/" + id + "/pdf";

		// Processo completo
		IAssijusSystem.DocIdPdfGetRequest q = new IAssijusSystem.DocIdPdfGetRequest();
		q.id = id;
		q.cpf = cpf;
		Future<SwaggerAsyncResponse<IAssijusSystem.DocIdPdfGetResponse>> future = SwaggerCall.callAsync(system + "-get",
				password, "GET", urlView, q, IAssijusSystem.DocIdPdfGetResponse.class);
		SwaggerAsyncResponse<IAssijusSystem.DocIdPdfGetResponse> sar = future.get();
		if (sar.getException() != null)
			throw sar.getException();
		IAssijusSystem.DocIdPdfGetResponse r = (IAssijusSystem.DocIdPdfGetResponse) sar.getResp();

		String secret = null;
		if (r.getHeaderFields().get("Doc-Secret") != null)
			secret = r.getHeaderFields().get("Doc-Secret").get(0);

		if (secret != null) {
			if (req.secret == null)
				throw new Exception("Parâmetro 'secret' precisa ser informado.");

			if (!Utils.makeSecret(secret).equals(req.secret)) {
				// Apenas para garantir que não é possível
				// um ataque para tentar descobrir o valor
				// de secret
				Thread.sleep(4000);
				throw new PresentableException("Não autorizado.");
			}
		}
		
		String ext = null;
		MimeTypes allTypes = MimeTypes.getDefaultMimeTypes();
		MimeType mt = allTypes.forName(r.contenttype);
		if (mt != null) 
			ext = mt.getExtension();
		resp.contentdisposition = "inline;filename=" + req.id + (ext != null ? ext : ".bin");
		resp.contentlength = r.contentlength;
		resp.contenttype = r.contenttype;
		resp.inputstream = r.inputstream;
	}

	public String getContext() {
		return "obter o pdf";
	}
}

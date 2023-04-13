package br.jus.trf2.assijus;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;

import br.jus.trf2.assijus.IAssijus.IInfoSystemIdSecretGet;
import br.jus.trf2.assijus.IAssijus.Movement;
import br.jus.trf2.assijus.IAssijus.Signature;
import br.jus.trf2.assijus.system.api.IAssijusSystem;

public class InfoSystemIdSecretGet implements IInfoSystemIdSecretGet {

	@Override
	public void run(Request req, Response resp, AssijusContext ctx) throws Exception {
		IAssijusSystem.IDocIdInfoGet.Response s = getInfo(req);

		// Produce response
		resp.status = s.status;
		if (s.movement != null) {
			resp.movement = new ArrayList<>();
			for (IAssijusSystem.Movement mx : s.movement) {
				Movement m = new Movement();
				m.time = mx.time;
				m.department = mx.department;
				m.kind = mx.kind;
				resp.movement.add(m);
			}
		}
		if (s.signature != null) {
			resp.signature = new ArrayList<>();
			for (IAssijusSystem.Signature sx : s.signature) {
				Signature g = new Signature();
				g.ref = sx.ref;
				g.signer = sx.signer;
				g.kind = sx.kind;
				resp.signature.add(g);
			}
		}
	}

	public static IAssijusSystem.IDocIdInfoGet.Response getInfo(Request req) throws Exception, PresentableException {
		String system = req.system;
		String id = req.id;
		String password = Utils.getPassword(system);

		String urlInfo = Utils.getUrl(system) + "/doc/" + id + "/info";

		// Call document repository hash webservice
		IAssijusSystem.IDocIdInfoGet.Request q = new IAssijusSystem.IDocIdInfoGet.Request();
		q.id = id;
		IAssijusSystem.IDocIdInfoGet.Response s;
		try {
			s = SwaggerCall
					.callAsync(system + "-info", password, "GET", urlInfo, q, IAssijusSystem.IDocIdInfoGet.Response.class)
					.get(AssijusServlet.SYSTEM_LIST_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();
		} catch (Exception ex) {
			throw new PresentableException("Problema reportado por " + system + ": " + ex.getMessage(), ex);
		}

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

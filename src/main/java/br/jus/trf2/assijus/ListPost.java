package br.jus.trf2.assijus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerCallParameters;
import com.crivano.swaggerservlet.SwaggerMultipleCallResult;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.Document;
import br.jus.trf2.assijus.IAssijus.IListPost;
import br.jus.trf2.assijus.system.api.IAssijusSystem.DocListGetRequest;
import br.jus.trf2.assijus.system.api.IAssijusSystem.DocListGetResponse;

public class ListPost implements IListPost {
	private static final Logger log = LoggerFactory.getLogger(ListPost.class);
	private static final long TIMEOUT_MILLISECONDS = 27000;

	@Override
	public void run(Request req, Response resp, AssijusContext ctx) throws Exception {
		String listkey = req.key;

		String authkey = req.authkey;
		String cpf = Utils.assertValidAuthKey(authkey, Utils.getUrlBluCServer()).cpf;

		if (listkey != null) {
			resp.list = new ArrayList<>();
			resp.status = new ArrayList<IAssijus.ListStatus>();
			
			// Read list from cache
			String payload = null;
			if (listkey != null)
				payload = SwaggerUtils.dbRetrieve(listkey, false);

			ListPost.Response fakeresp = SwaggerUtils.fromJson(payload, ListPost.Response.class);
			if (fakeresp.list != null) {
				resp.list.addAll(fakeresp.list);
				for (Document d : resp.list)
					d.secret = Utils.makeSecret(d.secret);
			}
		} else {
			produceListPostResponse(cpf, resp);
		}
	}

	public static void produceListPostResponse(String cpf, Response resp) throws Exception {
		// Read list from connected systems
		String[] systems = Utils.getSystems();
		if (systems == null)
			return;

		Map<String, SwaggerCallParameters> mapp = new HashMap<>();
		for (String system : systems) {
			String urlsys = Utils.getUrl(system);
			DocListGetRequest q = new DocListGetRequest();
			q.cpf = cpf;
			q.urlapi = urlsys;
			mapp.put(system, new SwaggerCallParameters(system + "-list", Utils.getPassword(system), "GET",
					urlsys + "/doc/list", q, DocListGetResponse.class));

		}
		SwaggerMultipleCallResult mcr = SwaggerCall.callMultiple(mapp, 15000);
		resp.status = Utils.getStatus(mcr);
		resp.list = new ArrayList<>();

		for (String system : mcr.responses.keySet()) {
			DocListGetResponse rl = (DocListGetResponse) mcr.responses.get(system);
			if (rl.list == null || rl.list.size() == 0)
				continue;
			for (br.jus.trf2.assijus.system.api.IAssijusSystem.Document r : rl.list) {
				Document doc = new Document();
				doc.code = r.code;
				doc.descr = r.descr;
				doc.id = r.id;
				doc.secret = Utils.makeSecret(r.secret);
				doc.kind = r.kind;
				doc.origin = r.origin;
				doc.system = system;
				resp.list.add(doc);
				SwaggerUtils.memCacheStore(cpf + "-" + system + "-" + doc.id, new byte[] { 1 });
			}
		}
	}

	@Override
	public String getContext() {
		return "listar documentos";
	}

}

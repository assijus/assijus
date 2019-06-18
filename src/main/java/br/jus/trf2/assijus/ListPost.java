package br.jus.trf2.assijus;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crivano.swaggerservlet.SwaggerAsyncResponse;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerException;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.Document;
import br.jus.trf2.assijus.IAssijus.IListPost;
import br.jus.trf2.assijus.IAssijus.ListPostRequest;
import br.jus.trf2.assijus.IAssijus.ListPostResponse;
import br.jus.trf2.assijus.IAssijus.ListStatus;
import br.jus.trf2.assijus.system.api.IAssijusSystem;
import br.jus.trf2.assijus.system.api.IAssijusSystem.DocListGetRequest;
import br.jus.trf2.assijus.system.api.IAssijusSystem.DocListGetResponse;

public class ListPost implements IListPost {
	private static final Logger log = LoggerFactory.getLogger(ListPost.class);
	private static final long TIMEOUT_MILLISECONDS = 120000;

	@Override
	public void run(ListPostRequest req, ListPostResponse resp) throws Exception {
		String listkey = req.key;

		String authkey = req.authkey;
		String cpf = Utils.assertValidAuthKey(authkey, Utils.getUrlBluCServer()).cpf;

		resp.list = new ArrayList<>();
		resp.status = new ArrayList<IAssijus.ListStatus>();

		if (listkey != null) {
			// Read list from cache
			String payload = null;
			if (listkey != null)
				payload = SwaggerUtils.dbRetrieve(listkey, false);

			ListPostResponse fakeresp = SwaggerUtils.fromJson(payload, ListPostResponse.class);
			if (fakeresp.list != null) {
				resp.list.addAll(fakeresp.list);
				for (Document d : resp.list)
					d.secret = Utils.makeSecret(d.secret);
			}
		} else {
			// Read list from connected systems
			String[] systems = Utils.getSystems();
			if (systems == null)
				return;

			final CountDownLatch responseWaiter = new CountDownLatch(systems.length);
			Map<String, Future<SwaggerAsyncResponse<DocListGetResponse>>> map = new HashMap<>();

			// Call Each System
			for (String system : systems) {
				final String context = system.replace("signer", "");
				String urlsys = Utils.getUrl(system);

				DocListGetRequest q = new DocListGetRequest();
				q.cpf = cpf;
				q.urlapi = urlsys;
				Future<SwaggerAsyncResponse<DocListGetResponse>> future = SwaggerCall.callAsync(system + "-list",
						Utils.getPassword(system), "GET", urlsys + "/doc/list", q, DocListGetResponse.class);
				map.put(system, future);
			}

			Date dt1 = new Date();

			for (String system : systems) {
				final String context = system.replace("signer", "");
				try {
					long timeout = TIMEOUT_MILLISECONDS - ((new Date()).getTime() - dt1.getTime());
					if (timeout < 0L)
						timeout = 0;
					SwaggerAsyncResponse<DocListGetResponse> futureresponse = map.get(system).get(timeout,
							TimeUnit.MILLISECONDS);
					DocListGetResponse o = (DocListGetResponse) futureresponse.getRespOrThrowException();
					ListStatus ls = new ListStatus();
					ls.system = system;
					resp.status.add(ls);
					SwaggerException ex = futureresponse.getException();
					if (ex != null) {
						log.error("Erro obtendo a lista de {}", system, ex);
						ls.errormsg = SwaggerUtils.messageAsString(ex);
						ls.stacktrace = SwaggerUtils.stackAsString(ex);
					}
					if (o != null && o.list != null) {
						for (IAssijusSystem.Document d : o.list) {
							Document doc = new Document();
							doc.code = d.code;
							doc.descr = d.descr;
							doc.id = d.id;
							doc.secret = Utils.makeSecret(d.secret);
							doc.kind = d.kind;
							doc.origin = d.origin;
							doc.system = system;
							resp.list.add(doc);
							SwaggerUtils.memCacheStore(cpf + "-" + system + "-" + doc.id, new byte[] { 1 });
						}
					}
				} catch (Exception ex) {
					ListStatus ls = new ListStatus();
					ls.system = system;
					ls.errormsg = SwaggerUtils.messageAsString(ex);
					if (ls.errormsg == null)
						ls.errormsg = ex.getClass().getName();
					ls.stacktrace = SwaggerUtils.stackAsString(ex);
					resp.status.add(ls);
					if (ex instanceof TimeoutException)
						map.get(system).cancel(true);
				}
			}
		}
	}

	@Override
	public String getContext() {
		return "listar documentos";
	}

}

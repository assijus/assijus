package br.jus.trf2.assijus;

import br.jus.trf2.assijus.IAssijus.IStorePost;

import com.crivano.swaggerservlet.SwaggerUtils;

public class StorePost implements IStorePost {

	@Override
	public void run(Request req, Response resp, AssijusContext ctx) throws Exception {
		String payload = req.payload;

		// Call
		String key = SwaggerUtils.dbStore(payload);

		// Produce response
		resp.status = "OK";
		resp.key = key;
	}

	@Override
	public String getContext() {
		return "armazenar a assinatura";
	}

}
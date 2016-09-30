package br.jus.trf2.assijus;

import br.jus.trf2.assijus.IAssijus.IStorePost;
import br.jus.trf2.assijus.IAssijus.StorePostRequest;
import br.jus.trf2.assijus.IAssijus.StorePostResponse;

import com.crivano.restservlet.RestUtils;

public class StorePost implements IStorePost {

	@Override
	public void run(StorePostRequest req, StorePostResponse resp)
			throws Exception {
		String payload = req.payload;

		// Call
		String key = RestUtils.dbStore(payload);

		// Produce response
		resp.status = "OK";
		resp.key = key;
	}

	@Override
	public String getContext() {
		return "armazenar a assinatura";
	}

}
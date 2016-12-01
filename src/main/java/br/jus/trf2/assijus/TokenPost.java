package br.jus.trf2.assijus;

import java.util.Date;

import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.ITokenPost;
import br.jus.trf2.assijus.IAssijus.TokenPostRequest;
import br.jus.trf2.assijus.IAssijus.TokenPostResponse;

public class TokenPost implements ITokenPost {

	@Override
	public void run(TokenPostRequest req, TokenPostResponse resp)
			throws Exception {
		String time = SwaggerUtils.format(new Date());

		// Produce response
		String policy = "AD-RB";
		resp.token = "TOKEN-" + time;
		resp.policy = policy;
	}

	@Override
	public String getContext() {
		return "obter o token";
	}

}

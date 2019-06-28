package br.jus.trf2.assijus;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.ITokenPost;
import br.jus.trf2.assijus.IAssijus.SignedAttrsPostRequest;
import br.jus.trf2.assijus.IAssijus.SignedAttrsPostResponse;
import br.jus.trf2.assijus.IAssijus.TokenPostRequest;
import br.jus.trf2.assijus.IAssijus.TokenPostResponse;

public class TokenPost implements ITokenPost {

	@Override
	public void run(TokenPostRequest req, TokenPostResponse resp) throws Exception {
		String time = SwaggerUtils.format(new Date());

		// Produce response
		String policy = "AD-RB";
		resp.token = "TOKEN-" + time;
		resp.policy = policy;
		resp.policyversion = "2.1";

		if (req.certificate != null) {
			SignedAttrsPost sa = new SignedAttrsPost();
			SignedAttrsPostRequest sareq = new SignedAttrsPostRequest();
			SignedAttrsPostResponse saresp = new SignedAttrsPostResponse();
			sareq.certificate = req.certificate;
			sareq.policy = policy;
			byte[] bytes = resp.token.getBytes(StandardCharsets.UTF_8);
			sareq.sha1 = Utils.calcSha1(bytes);
			sareq.sha256 = Utils.calcSha256(bytes);
			sa.buildSignedAttrs(sareq, saresp);
			resp.hash = saresp.hash;
			if (req.digest != null && req.digest)
				resp.hash = Utils.calcSha256(resp.hash);
			resp.time = saresp.time;
		}
	}

	@Override
	public String getContext() {
		return "obter o token";
	}

}

package br.jus.trf2.assijus;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWTSigner;
import com.crivano.swaggerservlet.PresentableException;

import br.jus.trf2.assijus.IAssijus.ILoginPost;
import br.jus.trf2.assijus.Utils.AuthKeyFields;

public class LoginPost implements ILoginPost {
	private static final Logger log = LoggerFactory.getLogger(LoginPost.class);

	@Override
	public void run(Request req, Response resp, AssijusContext ctx) throws Exception {
		if (req.callback == null)
			throw new PresentableException("Nenhuma URL de retorno especificada");

		String authkey = req.authkey;
		AuthKeyFields akf = Utils.assertValidAuthKey(authkey, Utils.getUrlBluCServer());

		String systems = AssijusServlet.getProp("login.systems");

		for (String system : systems.split(",")) {
			String urlBase = AssijusServlet.getProp(system + ".login.url.base");

			if (req.callback.startsWith(urlBase)) {
				String urlRedirect = AssijusServlet.getProp(system + ".login.url.redirect");

				String password = AssijusServlet.getProp(system + ".login.password");

				String jwt = jwt(akf.cpf, akf.cn, akf.email, password);
				resp.url = urlRedirect + "?callback=" + URLEncoder.encode(req.callback, StandardCharsets.UTF_8.name())
						+ "&jwt=" + jwt;
				return;
			}
		}
		throw new PresentableException("Nenhum sistema de login configurado para a url: '" + req.callback + "'");
	}

	private String jwt(String cpf, String name, String email, String password) {
		final String issuer = AssijusServlet.getProp("login.issuer");
		final String secret = password;

		final long iat = System.currentTimeMillis() / 1000L; // issued at claim
		final long exp = iat + 24 * 60 * 60L; // token expires in 1 date

		final JWTSigner signer = new JWTSigner(secret);
		final HashMap<String, Object> claims = new HashMap<String, Object>();
		if (issuer != null)
			claims.put("iss", issuer);
		claims.put("exp", exp);
		claims.put("iat", iat);

		claims.put("cpf", cpf);
		claims.put("name", name);
		claims.put("email", email);

		final String jwt = signer.sign(claims);
		return jwt;
	}

	@Override
	public String getContext() {
		return "produzir jwt";
	}

}

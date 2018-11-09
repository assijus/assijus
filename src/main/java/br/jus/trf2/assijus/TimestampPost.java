package br.jus.trf2.assijus;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.HashMap;

import br.jus.trf2.assijus.IAssijus.ITimestampPost;
import br.jus.trf2.assijus.IAssijus.TimestampPostRequest;
import br.jus.trf2.assijus.IAssijus.TimestampPostResponse;

import com.auth0.jwt.Algorithm;
import com.auth0.jwt.JWTSigner;
import com.auth0.jwt.JWTSigner.Options;
import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;

public class TimestampPost implements ITimestampPost {

	@Override
	public void run(TimestampPostRequest req, TimestampPostResponse resp)
			throws Exception {
		final String issuer = SwaggerUtils
				.getRequiredProperty(
						"assijus.timestamp.issuer",
						"Issuer não está corretamente definido no parâmetro assijus.timestamp.issuer",
						true);
		final long currentTimeMillis = System.currentTimeMillis();

		final long iat = currentTimeMillis / 1000L; // issued at claim

		// create the key instance
		PublicKey publicKey = null;
		PrivateKey privateKey = null;

		byte[] publicKeyBytes = SwaggerUtils
				.base64Decode(SwaggerUtils
						.getRequiredProperty(
								"assijus.timestamp.public.key",
								"Chave pública não está corretamente definida no parâmetro assijus.timestamp.public.key",
								true));
		byte[] privateKeyBytes = SwaggerUtils
				.base64Decode(SwaggerUtils
						.getRequiredProperty(
								"assijus.timestamp.private.key",
								"Chave privada não está corretamente definida no parâmetro assijus.timestamp.private.key",
								true));

		KeyFactory kf = KeyFactory.getInstance("RSA");
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(
				privateKeyBytes);
		privateKey = kf.generatePrivate(privateKeySpec);
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
		publicKey = kf.generatePublic(publicKeySpec);

		final JWTSigner signer = new JWTSigner(privateKey);
		final HashMap<String, Object> claims = new HashMap<String, Object>();

		claims.put("iss", issuer);
		claims.put("iat", iat);
		claims.put("time", currentTimeMillis);

		if (req.sha256 == null)
			throw new Exception("Parâmetro sha256 não informado");
		claims.put("sha256", req.sha256);

		if (req.tipo == null)
			throw new Exception("Parâmetro tipo não informado");
		if (!("sign".equals(req.tipo) || "auth".equals(req.tipo)))
			throw new Exception(
					"Parâmetro tipo deve ser informado com 'sign' ou 'auth'");
		claims.put("kind", req.tipo);

		if (req.system == null)
			throw new Exception("Parâmetro system não informado");
		claims.put("sys", req.system);

		if (req.json != null)
			claims.put("json", req.json);

		if (req.cpf != null)
			claims.put("cpf", req.cpf);
		if (req.nome != null)
			claims.put("name", req.nome);

		// claims.put("email", req.email);
		// claims.put("jti", id);

		String host = SwaggerServlet.getHttpServletRequest().getHeader(
				"X-Forwarded-For");
		if (host == null)
			host = SwaggerServlet.getHttpServletRequest().getRemoteHost();
		if (host != null)
			claims.put("host", host);

		Options options = new Options();
		options.setAlgorithm(Algorithm.RS256);
		final String jwt = signer.sign(claims, options);

		// Produce response
		resp.jwt = jwt;
		resp.time = new Date(currentTimeMillis);

		resp.host = host;
		// resp.id;
		// resp.url;
	}

	@Override
	public String getContext() {
		return "produzir timestamp";
	}

}
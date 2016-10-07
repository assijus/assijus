package br.jus.trf2.assijus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.blucservice.api.IBlueCrystal.ValidatePostResponse;
import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerUtils;

public class Utils {
	// public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
	public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final SimpleDateFormat isoFormatter = new SimpleDateFormat(
			ISO_FORMAT);

	public static String getUrl(String system) {
		return SwaggerUtils.getProperty(system + ".url",
				"http://localhost:8080/" + system + "/api/v1");
	}

	public static String getPassword(String system) {
		return SwaggerUtils.getProperty(system + ".password", null);
	}

	public static String getUrlBluCServer() {
		return SwaggerUtils.getProperty("blucservice.url",
				"http://localhost:8080/blucservice/api/v1");
	}

	public static String[] getSystems() {
		String systems = SwaggerUtils.getProperty("assijus.systems", null);
		if (systems == null)
			return null;
		return systems.split(",");
	}

	public static String fixUrl(String url) {
		for (String system : getSystems()) {
			if (url.startsWith(system.replace("signer", "") + "/")
					|| url.startsWith(system + "/")) {
				String urlSystem = Utils.getUrl(system);
				return urlSystem + url.substring(url.indexOf("/"));
			}
		}
		return url;
	}

	public static String chooseSystem(String url) {
		for (String system : getSystems()) {
			if (url.startsWith(system.replace("signer", "") + "/")
					|| url.startsWith(system + "/")) {
				return system;
			}
		}
		return null;
	}

	public static ValidatePostResponse validateToken(String token,
			String urlblucserver) throws Exception {
		String tokenAsString = token.split(";")[0];
		if (!tokenAsString.startsWith("TOKEN-"))
			throw new Exception("Token não está no formato correto.");
		byte[] tokenAsBytes = tokenAsString.getBytes("UTF-8");
		String dateAsString = tokenAsString.substring(6);
		Date date = parse(dateAsString);
		if (date == null)
			throw new Exception("Data do token não está no formato correto.");
		String signB64 = token.split(";")[1];
		if (signB64 == null)
			throw new Exception("Assinatura do token não foi encontrada.");

		// Validate: call bluc-server validate webservice. If there is an error,
		// it will throw an exception.
		IBlueCrystal.ValidatePostRequest q = new IBlueCrystal.ValidatePostRequest();
		q.time = SwaggerUtils.parse(dateAsString);
		q.sha1 = calcSha1(tokenAsBytes);
		q.sha256 = calcSha256(tokenAsBytes);
		q.crl = true;
		q.envelope = SwaggerUtils.base64Decode(signB64);
		return SwaggerCall.call("bluc-validate", null, "POST",
				Utils.getUrlBluCServer() + "/validate", q,
				IBlueCrystal.ValidatePostResponse.class);
	}

	public static JSONObject validateAuthKey(String authkey,
			String urlblucserver) throws Exception {
		String payload = SwaggerUtils.dbRetrieve(authkey, false);

		if (payload == null) {
			throw new PresentableException(
					"Não foi possível recuperar dados de autenticação a partir da chave informada.");
		}

		JSONObject json = new JSONObject(payload);
		String kind = json.getString("kind");
		ValidatePostResponse blucresp = validateToken(json.getString("token"),
				urlblucserver);
		String cpf = null;
		cpf = blucresp.certdetails.cpf0;
		if (!cpf.equals(json.getString("cpf")))
			throw new Exception("cpf não confere");
		return json;
	}

	public static String assertValidAuthKey(String authkey, String urlblucserver)
			throws Exception {
		byte[] cached = SwaggerUtils.memCacheRetrieve("valid-" + authkey);
		if (cached != null)
			return new String(cached);

		JSONObject json = validateAuthKey(authkey, urlblucserver);

		String cpf = json.getString("cpf");
		SwaggerUtils.memCacheStore("valid-" + authkey, cpf.getBytes());
		return cpf;
	}

	public static String format(Date date) {
		return isoFormatter.format(date);
	}

	public static Date parse(String date) {
		try {
			return isoFormatter.parse(date);
		} catch (ParseException e) {
			return null;
		}
	}

	public static byte[] calcSha1(byte[] content)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.reset();
		md.update(content);
		byte[] output = md.digest();
		return output;
	}

	public static byte[] calcSha256(byte[] content)
			throws NoSuchAlgorithmException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.reset();
		md.update(content);
		byte[] output = md.digest();
		return output;
	}

}

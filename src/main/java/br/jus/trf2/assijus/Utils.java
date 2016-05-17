package br.jus.trf2.assijus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import br.jus.trf2.restservlet.RestUtils;

public class Utils {
	private static final Logger log = Logger.getLogger(Utils.class.getName());

	// public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
	public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final SimpleDateFormat isoFormatter = new SimpleDateFormat(
			ISO_FORMAT);

	private static final Map<String, byte[]> cache = new HashMap<String, byte[]>();

	public static String getUrlBaseApolo() {
		return RestUtils.getProperty("apolosigner.url",
				"http://localhost:8080/apolosigner/api/v1");
	}

	public static String getApoloPassword() {
		return RestUtils.getProperty("apolosigner.password", null);
	}

	public static String getUrlBaseSigaDoc() {
		return RestUtils.getProperty("sigadocsigner.url",
				"http://localhost:8080/sigaex/public/app/assinador-externo");
	}

	public static String getSigaDocPassword() {
		return RestUtils.getProperty("sigadocsigner.password", null);
	}

	public static String getUrlBaseBluCServer() {
		return RestUtils.getProperty("blucservice.url",
				"http://localhost:8080/blucservice/api/v1");
	}

	public static String getUrlBaseAPI() {
		return RestUtils.getProperty("assijus.url",
				"http://localhost:8080/trf2signer/api/v1");
	}

	public static String getKeyValueServer() {
		return RestUtils.getProperty("assijus.keyvalue.url",
				"http://localhost:8080/trf2signer/api/v1");
	}

	public static String getKeyValuePassword() {
		return RestUtils.getProperty("assijus.keyvalue.password", null);
	}

	public static String getRetrievePassword() {
		return RestUtils.getProperty("assijus.retrieve.password", null);
	}

	public static String fixUrl(String url) {
		if (url.startsWith("apolo/")) {
			return getUrlBaseApolo() + url.substring(5);
		}
		if (url.startsWith("sigadoc/")) {
			return getUrlBaseSigaDoc() + url.substring(7);
		}
		return url;
	}

	public static String choosePassword(String url) {
		if (url.startsWith("apolo/")) {
			return getApoloPassword();
		}
		if (url.startsWith("sigadoc/")) {
			return getSigaDocPassword();
		}
		return null;
	}

	public static String assertValidToken(String token, String urlblucserver)
			throws Exception {
		byte[] cached = cacheRetrieve(token);
		if (cached != null)
			return new String(cached);
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

		JSONObject blucreq = new JSONObject();
		blucreq.put("envelope", signB64);
		blucreq.put("time", dateAsString);
		blucreq.put("policy", "PKCS7");
		blucreq.put("sha1", Base64.encode(calcSha1(tokenAsBytes)));
		blucreq.put("sha256", Base64.encode(calcSha256(tokenAsBytes)));
		blucreq.put("crl", true);

		// Call bluc-server hash webservice
		JSONObject blucresp = RestUtils.getJsonObjectFromJsonPost(new URL(
				urlblucserver + "/validate"), blucreq, "bluc-validate");

		String cpf = null;
		cpf = blucresp.getJSONObject("certdetails").getString("cpf0");
		cacheStore(token, cpf.getBytes());
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

	public static void cacheStore(String sha1, byte[] ba) {
		cache.put(sha1, ba);
	}

	public static byte[] cacheRetrieve(String sha1) {
		if (cache.containsKey(sha1)) {
			byte[] ba = cache.get(sha1);
			return ba;
		}
		return null;
	}

	public static byte[] cacheRemove(String sha1) {
		if (cache.containsKey(sha1)) {
			byte[] ba = cache.get(sha1);
			cache.remove(sha1);
			return ba;
		}
		return null;
	}

	public static String dbStore(String payload) {
		String id = UUID.randomUUID().toString();
		cacheStore(id, payload.getBytes());
		return id;
	}

	public static String dbRetrieve(String id, boolean remove) {
		byte[] ba = null;
		if (remove)
			ba = cacheRemove(id);
		else
			ba = cacheRetrieve(id);
		if (ba == null)
			return null;
		String s = new String(ba);

		if (s == null || s.trim().length() == 0)
			return null;

		return s;
	}

	public static void logsevere(String s) {
		log.severe(s);
	}

}

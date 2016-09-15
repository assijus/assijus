package br.jus.trf2.assijus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.json.JSONObject;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;

import com.crivano.restservlet.PresentableException;
import com.crivano.restservlet.RestUtils;

public class Utils {
	static JedisPool pool = new JedisPool(new JedisPoolConfig(),
			RestUtils.getProperty("redis.servername", "localhost"),
			Integer.parseInt(RestUtils
					.getProperty("redis.port", "6379")),
			Protocol.DEFAULT_TIMEOUT, RestUtils.getProperty(
					"redis.password", null), Integer.parseInt(RestUtils
					.getProperty("redis.database", "10")));

	// public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
	public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final SimpleDateFormat isoFormatter = new SimpleDateFormat(
			ISO_FORMAT);

	public static String getUrl(String system) {
		return RestUtils.getProperty(system + ".url", "http://localhost:8080/"
				+ system + "/api/v1");
	}

	public static String getPassword(String system) {
		return RestUtils.getProperty(system + ".password", null);
	}

	public static String getUrlBluCServer() {
		return RestUtils.getProperty("blucservice.url",
				"http://localhost:8080/blucservice/api/v1");
	}

	public static String getKeyValueServer() {
		return RestUtils.getProperty("assijus.keyvalue.url",
				"http://localhost:8080/assijus/api/v1");
	}

	public static String getKeyValuePassword() {
		return RestUtils.getProperty("assijus.keyvalue.password", null);
	}

	public static String getRetrievePassword() {
		return RestUtils.getProperty("assijus.retrieve.password", null);
	}

	public static String[] getSystems() {
		String systems = RestUtils.getProperty("assijus.systems", null);
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

	public static JSONObject validateToken(String token, String urlblucserver)
			throws Exception {
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
		JSONObject blucresp = RestUtils.restPost("bluc-validate", null,
				urlblucserver + "/validate", blucreq);
		return blucresp;
	}

	public static JSONObject validateAuthKey(String authkey,
			String urlblucserver) throws Exception {
		String payload = Utils.dbRetrieve(authkey, false);

		if (payload == null && Utils.getKeyValueServer() != null) {
			// Parse certificate
			JSONObject kvreq = new JSONObject();
			kvreq.put("key", authkey);
			kvreq.put("remove", false);
			kvreq.put("password", Utils.getKeyValuePassword());

			// Call bluc-server hash webservice
			JSONObject kvresp = RestUtils.restPost("sign-retrieve", null,
					Utils.getKeyValueServer() + "/retrieve", kvreq);
			payload = kvresp.optString("payload", null);
		}

		if (payload == null) {
			throw new PresentableException(
					"Não foi possível recuperar dados de autenticação a partir da chave informada.");
		}

		JSONObject json = new JSONObject(payload);
		String kind = json.getString("kind");
		if ("client-cert".equals(kind)) {
			if (getRetrievePassword() != null)
				if (!getRetrievePassword().equals(json.get("password")))
					throw new Exception(
							"Senha inválida na autenticação com client-cert");
		} else if ("signed-token".equals(kind)) {
			JSONObject blucresp = validateToken(json.getString("token"),
					urlblucserver);
			String cpf = null;
			cpf = blucresp.getJSONObject("certdetails").getString("cpf0");
			if (!cpf.equals(json.getString("cpf")))
				throw new Exception("cpf não confere");
		}
		return json;
	}

	public static String assertValidAuthKey(String authkey, String urlblucserver)
			throws Exception {
		byte[] cached = cacheRetrieve("valid-" + authkey);
		if (cached != null)
			return new String(cached);

		JSONObject json = validateAuthKey(authkey, urlblucserver);

		String cpf = json.getString("cpf");
		cacheStore("valid-" + authkey, cpf.getBytes());
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
		try (Jedis jedis = pool.getResource()) {
			jedis.set(SafeEncoder.encode(sha1), ba);
		}
	}

	public static byte[] cacheRetrieve(String sha1) {
		try (Jedis jedis = pool.getResource()) {
			byte[] ba = jedis.get(SafeEncoder.encode(sha1));
			return ba;
		} catch (Exception e) {
			return null;
		}
	}

	public static byte[] cacheRemove(String sha1) {
		try (Jedis jedis = pool.getResource()) {
			byte[] key = SafeEncoder.encode(sha1);
			byte[] ba = jedis.get(key);
			jedis.del(key);
			return ba;
		} catch (Exception e) {
			return null;
		}
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
}

package br.jus.trf2.assijus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class Utils {
	private static final Logger log = Logger.getLogger(Utils.class.getName());

	// public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS zzz";
	public static String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
	public static final SimpleDateFormat isoFormatter = new SimpleDateFormat(
			ISO_FORMAT);

	private static final Map<String, byte[]> cache = new HashMap<String, byte[]>();

	public static String getUrlBaseApolo() {
		return getProperty("apolosigner.url",
				"http://localhost:8080/apolosigner/api/v1");
	}

	public static String getApoloPassword() {
		return getProperty("apolosigner.password", null);
	}

	public static String getUrlBaseSigaDoc() {
		return getProperty("sigadocsigner.url",
				"http://localhost:8080/sigaex/public/app/assinador-externo");
	}

	public static String getSigaDocPassword() {
		return getProperty("sigadocsigner.password", null);
	}

	public static String getUrlBaseBluCServer() {
		return getProperty("blucservice.url",
				"http://localhost:8080/blucservice/api/v1");
	}

	public static String getUrlBaseAPI() {
		return getProperty("assijus.url",
				"http://localhost:8080/trf2signer/api/v1");
	}

	public static String getKeyValueServer() {
		return getProperty("assijus.keyvalue.url",
				"http://localhost:8080/trf2signer/api/v1");
	}

	public static String getKeyValuePassword() {
		return getProperty("assijus.keyvalue.password", null);
	}

	public static String getRetrievePassword() {
		return getProperty("assijus.retrieve.password", null);
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

	public static String getProperty(String propertyName, String defaultValue) {
		String s = System.getProperty(propertyName);
		if (s != null)
			return s;
		s = System.getenv("PROP_"
				+ propertyName.replace(".", "_").toUpperCase());
		if (s != null)
			return s;
		return defaultValue;
	}

	public static JSONObject getJsonReq(HttpServletRequest request,
			String context) {
		try {
			String sJson = Utils.getBody(request);
			JSONObject req = new JSONObject(sJson);
			if (context != null)
				log.info(context + " req: " + req.toString(3));
			return req;
		} catch (Exception ex) {
			throw new RuntimeException("Cannot parse request body as JSON", ex);
		}
	}

	public static void writeJsonResp(HttpServletResponse response,
			JSONObject resp, String context) throws JSONException, IOException {
		if (context != null)
			log.info(context + " resp: " + resp.toString(3));

		String s = resp.toString(2);
		response.setContentType("application/json; charset=UTF-8");
		response.setCharacterEncoding("UTF-8");
		response.getWriter().println(s);
	}

	public static String getBody(HttpServletRequest request) throws IOException {

		String body = null;
		StringBuilder stringBuilder = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			InputStream inputStream = request.getInputStream();
			if (inputStream != null) {
				bufferedReader = new BufferedReader(new InputStreamReader(
						inputStream));
				char[] charBuffer = new char[128];
				int bytesRead = -1;
				while ((bytesRead = bufferedReader.read(charBuffer)) > 0) {
					stringBuilder.append(charBuffer, 0, bytesRead);
				}
			} else {
				stringBuilder.append("");
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException ex) {
					throw ex;
				}
			}
		}

		body = stringBuilder.toString();
		return body;
	}

	public static JSONObject getJsonObjectFromURL(URL url, String context)
			throws Exception {
		if (context != null)
			log.info(context + " url: " + url);

		JSONObject o = null;
		try {
			final HttpResponse<JsonNode> jsonResponse = Unirest.get(
					url.toString()).asJson();

			o = jsonResponse.getBody().getObject();
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new WSException(errmsg, null, null, context);
		}

		if (context != null)
			log.info(context + " resp: " + o.toString(3));

		String error = o.optString("error", null);
		if (error != null)
			throw new Exception(error);

		return o;
	}

	public static JSONObject getJsonObjectFromJsonPost(URL url, JSONObject req,
			String context) throws Exception {
		if (context != null) {
			log.info(context + " url: " + url + " req: " + req.toString(3));
		}

		JSONObject o = null;
		try {
			final HttpResponse<JsonNode> jsonResponse = Unirest
					.post(url.toString()).body(new JsonNode(req.toString()))
					.asJson();
			o = jsonResponse.getBody().getObject();
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new WSException(errmsg, req, null, context);
		}
		if (context != null)
			log.info(context + " resp: " + o.toString(3));

		String error = o.optString("error", null);
		if (error != null)
			throw new WSException(error, req, o, context);

		return o;
	}

	private static class LoggingCallback implements Callback<JsonNode> {
		private JSONAsyncCallback callback;
		private JSONObject req;
		private String context;

		public LoggingCallback(JSONAsyncCallback callback, JSONObject req,
				String context) {
			this.callback = callback;
			this.req = req;
			this.context = context;
		}

		@Override
		public void completed(HttpResponse<JsonNode> response) {
			JSONObject o = null;
			o = response.getBody().getObject();
			if (context != null)
				try {
					log.info(context + " resp: " + o.toString(3));
				} catch (JSONException e) {
				}
			String error = o.optString("error", null);
			if (error != null) {
				String jsonreq;
				String jsonresp;
				try {
					callback.failed(new WSException(error, req, o, context));
				} catch (Exception e) {
				}
			} else
				try {
					callback.completed(o);
				} catch (Exception e) {
				}
		}

		@Override
		public void failed(UnirestException ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			WSException wse = new WSException(errmsg, req, null, context);
			try {
				callback.failed(wse);
			} catch (Exception e) {
			}
		}

		@Override
		public void cancelled() {
			try {
				callback.cancelled();
			} catch (Exception e) {
			}
		}

	}

	public static Future getJsonObjectFromJsonPostAsync(URL url,
			JSONObject req, String context, final JSONAsyncCallback callback)
			throws Exception {
		if (context != null) {
			log.info(context + " url: " + url + " req: " + req.toString(3));
		}

		try {
			return Unirest.post(url.toString())
					.body(new JsonNode(req.toString()))
					.asJsonAsync(new LoggingCallback(callback, req, context));
		} catch (Exception ex) {
			String errmsg = messageAsString(ex);
			String errstack = stackAsString(ex);
			throw new WSException(errmsg, req, null, context);
		}
	}

	private static class WSException extends Exception {
		JSONObject jsonreq;
		JSONObject jsonresp;

		public WSException(String error, JSONObject jsonreq,
				JSONObject jsonresp, String context) {
			super(error);
			this.jsonreq = jsonreq;
			this.jsonresp = jsonresp;
		}
	}

	public static void writeJsonError(HttpServletResponse response,
			final Exception e, String context) {
		JSONObject json = new JSONObject();
		String errmsg = messageAsString(e);
		String errstack = stackAsString(e);

		try {
			json.put("error", errmsg);

			// Error Details
			JSONArray arr = new JSONArray();
			for (Throwable t = e; t != null && t != t.getCause(); t = t
					.getCause()) {
				if (t instanceof WSException) {
					WSException wse = (WSException) t;
					if (wse.jsonresp != null) {
						JSONObject resp = new JSONObject(wse.jsonresp);
						JSONArray arrsub = resp.optJSONArray("errordetails");
						if (arrsub != null)
							arr = arrsub;
					}
					break;
				}
			}
			JSONObject detail = new JSONObject();
			detail.put("context", context);
			detail.put("service", "assijus");
			detail.put("stacktrace", errstack);
			arr.put(detail);
			json.put("errordetails", arr);

			response.setStatus(500);
			writeJsonResp(response, json, context);
		} catch (Exception e1) {
			throw new RuntimeException("Erro retornando mensagem de erro.", e1);
		}
	}

	public static String messageAsString(final Exception e) {
		String errmsg = e.getMessage();
		if (errmsg == null)
			if (e instanceof NullPointerException)
				errmsg = "null pointer.";
		return errmsg;
	}

	public static String stackAsString(final Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String errstack = sw.toString(); // stack trace as a string
		return errstack;
	}

	public static void assertValidToken(String token, String urlblucserver)
			throws Exception {
		if (cacheRetrieve(token) != null)
			return;
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
		JSONObject blucresp = Utils.getJsonObjectFromJsonPost(new URL(
				urlblucserver + "/validate"), blucreq, "bluc-validate");

		cacheStore(token, tokenAsBytes);
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

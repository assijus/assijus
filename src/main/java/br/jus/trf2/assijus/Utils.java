package br.jus.trf2.assijus;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

import com.crivano.blucservice.api.IBlueCrystal;
import com.crivano.blucservice.api.IBlueCrystal.ValidatePostResponse;
import com.crivano.swaggerservlet.PresentableException;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerCallStatus;
import com.crivano.swaggerservlet.SwaggerMultipleCallResult;
import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;

public class Utils {
	public static String getUrl(String system) {
		return AssijusServlet.getProp(system + ".url");
	}

	public static String getPassword(String system) {
		return AssijusServlet.getProp(system + ".password");
	}

	public static String getUrlBluCServer() {
		return AssijusServlet.getProp("blucservice.url");
	}

	public static String[] getSystems() {
		String systems = AssijusServlet.getProp("systems");
		if (systems == null)
			return null;
		return systems.split(",");
	}

	public static String[] getLoginSystems() {
		String systems = AssijusServlet.getProp("login.systems");
		if (systems == null)
			return null;
		return systems.split(",");
	}

	public static String fixUrl(String url) {
		String[] systems = getSystems();
		if (systems != null) {
			for (String system : getSystems()) {
				if (url.startsWith(system.replace("signer", "") + "/") || url.startsWith(system + "/")) {
					String urlSystem = Utils.getUrl(system);
					return urlSystem + url.substring(url.indexOf("/"));
				}
			}
		}
		return url;
	}

	public static String chooseSystem(String url) {
		String[] systems = getSystems();
		if (systems != null) {
			for (String system : systems) {
				if (url.startsWith(system.replace("signer", "") + "/") || url.startsWith(system + "/")) {
					return system;
				}
			}
		}
		return null;
	}

	public static ValidatePostResponse validateToken(String token, String urlblucserver) throws Exception {
		String tokenAsString = token.split(";")[0];
		if (!tokenAsString.startsWith("TOKEN-"))
			throw new Exception("Token não está no formato correto.");
		byte[] tokenAsBytes = tokenAsString.getBytes("UTF-8");
		String dateAsString = tokenAsString.substring(6);
		Date date = SwaggerUtils.dateAdapter.parse(dateAsString);
		if (date == null)
			throw new Exception("Data do token não está no formato correto.");
		String signB64 = token.split(";")[1];
		if (signB64 == null)
			throw new Exception("Assinatura do token não foi encontrada.");

		// Validate: call bluc-server validate webservice. If there is an error,
		// it will throw an exception.
		IBlueCrystal.ValidatePostRequest q = new IBlueCrystal.ValidatePostRequest();
		q.time = SwaggerUtils.dateAdapter.parse(dateAsString);
		q.sha1 = calcSha1(tokenAsBytes);
		q.sha256 = calcSha256(tokenAsBytes);
		q.crl = true;
		q.envelope = SwaggerUtils.base64Decode(signB64);
		return SwaggerCall
				.callAsync("bluc-validate", null, "POST", Utils.getUrlBluCServer() + "/validate", q,
						IBlueCrystal.ValidatePostResponse.class)
				.get(AssijusServlet.VALIDATE_TIMEOUT, TimeUnit.SECONDS).getRespOrThrowException();
	}

	public static AuthKeyFields validateAuthKey(String authkey, String urlblucserver) throws Exception {
		String payload = SwaggerUtils.dbRetrieve(authkey, false);
		if (payload == null) {
			throw new PresentableException(
					"Não foi possível recuperar dados de autenticação a partir da chave informada.");
		}

		JSONObject json = new JSONObject(payload);
		ValidatePostResponse blucresp = validateToken(json.getString("token"), urlblucserver);

		AuthKeyFields akf = new AuthKeyFields();
		akf.cn = blucresp.cn;
		akf.cpf = blucresp.certdetails.cpf0;
		akf.email = blucresp.certdetails.san_email0;

		if (!akf.cpf.equals(json.getString("cpf")))
			throw new Exception("cpf não confere");
		return akf;
	}

	public static class AuthKeyFields implements Serializable {
		private static final long serialVersionUID = 7284471261596241887L;
		String cpf;
		String email;
		String cn;

		public static byte[] serialize(AuthKeyFields akf) throws Exception {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ObjectOutput out = null;
			try {
				out = new ObjectOutputStream(bos);
				out.writeObject(akf);
				out.flush();
				return bos.toByteArray();
			} finally {
				try {
					bos.close();
				} catch (IOException ex) {
					// ignore close exception
				}
			}
		}

		public static AuthKeyFields deserialize(byte[] bytes) throws Exception {
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			ObjectInput in = null;
			try {
				in = new ObjectInputStream(bis);
				return (AuthKeyFields) in.readObject();
			} finally {
				try {
					if (in != null) {
						in.close();
					}
				} catch (IOException ex) {
					// ignore close exception
				}
			}
		}
	}

	public static AuthKeyFields assertValidAuthKey(String authkey, String urlblucserver) throws Exception {
		byte[] cached = SwaggerUtils.memCacheRetrieve("valid-" + authkey);
		if (cached != null)
			return AuthKeyFields.deserialize(cached);
		AuthKeyFields akf = validateAuthKey(authkey, urlblucserver);
		SwaggerUtils.memCacheStore("valid-" + authkey, AuthKeyFields.serialize(akf));
		return akf;
	}

	public static byte[] calcSha1(byte[] content) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		md.reset();
		md.update(content);
		byte[] output = md.digest();
		return output;
	}

	public static byte[] calcSha256(byte[] content) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		md.reset();
		md.update(content);
		byte[] output = md.digest();
		return output;
	}

	final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {

		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static String makeSecret(String s) {
		if (s == null || s.length() == 0)
			return null;
		byte[] bytes = s.getBytes();
		return bytesToHex(calcSha1(bytes));
	}

	public static ArrayList<IAssijus.ListStatus> getStatus(SwaggerMultipleCallResult mcr) {
		ArrayList<IAssijus.ListStatus> l = new ArrayList<IAssijus.ListStatus>();
		for (SwaggerCallStatus sts : mcr.status) {
			IAssijus.ListStatus lsts = new IAssijus.ListStatus();
			lsts.system = sts.system;
			lsts.errormsg = sts.errormsg;
			lsts.stacktrace = sts.stacktrace;
			if (sts.miliseconds != null)
				lsts.miliseconds = sts.miliseconds.doubleValue();
			l.add(lsts);
		}
		return l;
	}
}

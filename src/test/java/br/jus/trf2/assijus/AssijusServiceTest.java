package br.jus.trf2.assijus;

import org.json.JSONException;
import org.json.JSONObject;

import com.crivano.restservlet.HTTPMockFromJSON;
import com.crivano.restservlet.RestUtils;
import com.crivano.swaggerservlet.SwaggerTestSupport;

public class AssijusServiceTest extends SwaggerTestSupport {

	// Token
	String time = "2016-03-16T19:13:36.428-03:00";
	String sha1 = "vBpvCtThfEl+PXn6ZpkQEcWEIyw\u003d";
	String sha256 = "9wjEyeorr2HA78aSNQNK7OqZ/rkhw/Br+0BzwAO2TYQ\u003d";
	// envelope foi alterado por questões de segurança
	String envelope = "NFARIGcbOEfESQt/Niq2EXP6L3gEbzrMZKC5vRqguws=";
	String policy = "AD-RB";

	// Auth
	String token = "TOKEN-2016-09-26T11:07:29.828-03:00;NFARIGcbOEfESQt/Niq2EXP6L3gEbzrMZKC5vRqguws=";
	String certificate = "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";
	String cpf = "00489623760";
	String kind = "signed-token";
	String name = "RENATO DO AMARAL CRIVANO MACHADO";

	// List
	String authkey = "57cfe493-dcde-4a84-a125-ba25d4491e21";

	// Save
	String id = "00489623760_144_46";
	String system = "testesigner";
	String policyversion = "2.1";
	String code = "0148053-07.2014.4.02.5151/01";
	// signature foi alterado por questões de segurança
	String signature = "NFARIGcbOEfESQt/Niq2EXP6L3gEbzrMZKC5vRqguws=";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		HTTPMockFromJSON http = new HTTPMockFromJSON();
		http.add("http://localhost:8080/blucservice/api/v1", this.getClass()
				.getResourceAsStream("blucservice.mock.json"));
		http.add("http://localhost:8080/testesigner/api/v1", this.getClass()
				.getResourceAsStream("testesigner.mock.json"));
		RestUtils.setHttp(http);
		RestUtils.setProperty("assijus.systems", "testesigner");
	}

	@Override
	protected String getPackage() {
		return "br.jus.trf2.assijus";
	}

	public void testToken_Simple_Success() throws JSONException {
		JSONObject req = new JSONObject();
		JSONObject resp = run("POST", "/token", req);

		assertEquals(policy, resp.get("policy"));
		assertTrue(resp.getString("token").startsWith("TOKEN-"));
	}

	public void testAuth_ByToken_Success() throws JSONException {
		JSONObject req = new JSONObject();
		req.put("token", token);
		JSONObject resp = run("POST", "/auth", req);

		assertEquals(36, resp.getString("authkey").length());
		assertEquals(token, resp.getString("token"));
		assertEquals(certificate, resp.getString("certificate"));
		assertEquals(cpf, resp.getString("cpf"));
		assertEquals(kind, resp.getString("kind"));
		assertEquals(name, resp.getString("name"));
	}

	public void testStore_Simple_Success() throws JSONException {
		JSONObject req = new JSONObject();
		req.put("payload", sha1);
		JSONObject resp = run("POST", "/store", req);

		assertEquals(36, resp.getString("key").length());
		assertEquals("OK", resp.getString("status"));
	}

	public void testList_Simple_Success() throws JSONException {
		JSONObject authreq = new JSONObject();
		authreq.put("token", token);
		JSONObject authresp = run("POST", "/auth", authreq);
		String authkey = authresp.getString("authkey");

		JSONObject req = new JSONObject();
		req.put("certificate", certificate);
		req.put("authkey", authkey);
		JSONObject resp = run("POST", "/list", req);

		assertEquals(3, resp.getJSONArray("list").length());
		assertEquals("testesigner", resp.getJSONArray("list").getJSONObject(0)
				.getString("system"));
		assertEquals("OK", resp.getString("status-teste"));
	}

	public void testHash_Simple_Success() throws JSONException {
		JSONObject authreq = new JSONObject();
		authreq.put("token", token);
		JSONObject authresp = run("POST", "/auth", authreq);
		String authkey = authresp.getString("authkey");

		JSONObject req = new JSONObject();
		req.put("authkey", authkey);
		req.put("id", id);
		req.put("certificate", certificate);
		req.put("system", system);
		JSONObject resp = run("POST", "/hash", req);

		assertEquals(sha1, resp.getString("sha1"));
		assertEquals(sha256, resp.getString("sha256"));
	}

	public void testSave_Simple_Success() throws JSONException {
		JSONObject req = new JSONObject();
		req.put("id", id);
		req.put("code", code);
		req.put("certificate", certificate);
		req.put("time", time);
		req.put("system", system);
		req.put("sha1", sha1);
		req.put("sha256", sha256);
		req.put("policy", policy);
		req.put("policyversion", policyversion);
		req.put("signature", signature);
		JSONObject resp = run("POST", "/save", req);

		assertEquals("OK", resp.getString("status"));
	}
}

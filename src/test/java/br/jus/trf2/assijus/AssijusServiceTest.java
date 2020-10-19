package br.jus.trf2.assijus;

import java.util.concurrent.Executors;

import org.json.JSONException;

import com.crivano.swaggerservlet.HTTPMockFromJSON;
import com.crivano.swaggerservlet.ISwaggerRequest;
import com.crivano.swaggerservlet.ISwaggerResponse;
import com.crivano.swaggerservlet.SwaggerCall;
import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;

import br.jus.trf2.assijus.IAssijus.AuthPostRequest;
import br.jus.trf2.assijus.IAssijus.AuthPostResponse;
import br.jus.trf2.assijus.IAssijus.HashPostRequest;
import br.jus.trf2.assijus.IAssijus.HashPostResponse;
import br.jus.trf2.assijus.IAssijus.ListPostRequest;
import br.jus.trf2.assijus.IAssijus.ListPostResponse;
import br.jus.trf2.assijus.IAssijus.SavePostRequest;
import br.jus.trf2.assijus.IAssijus.SavePostResponse;
import br.jus.trf2.assijus.IAssijus.StorePostRequest;
import br.jus.trf2.assijus.IAssijus.StorePostResponse;
import br.jus.trf2.assijus.IAssijus.TokenPostRequest;
import br.jus.trf2.assijus.IAssijus.TokenPostResponse;
import junit.framework.TestCase;

public class AssijusServiceTest extends TestCase {
	private AssijusServlet ss = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ss = new AssijusServlet();
		ss.INSTANCE = ss;
		ss.setAPI(IAssijus.class);
		ss.setActionPackage("br.jus.trf2.assijus");
		SwaggerServlet.executor = Executors.newFixedThreadPool(10);

		ss.addPublicProperty("systems", "testesigner");
		ss.addPublicProperty("swaggerservlet.threadpool.size", "20");
		ss.addPublicProperty("blucservice.url", "http://localhost:8080/blucservice/api/v1");
		ss.addPublicProperty("testesigner.url", "http://localhost:8080/testesigner/api/v1");
		ss.addPublicProperty("testesigner.password", null);

		HTTPMockFromJSON http = new HTTPMockFromJSON();
		http.add("http://localhost:8080/blucservice/api/v1",
				this.getClass().getResourceAsStream("blucservice.mock.json"));
		http.add("http://localhost:8080/testesigner/api/v1",
				this.getClass().getResourceAsStream("testesigner.mock.json"));
		SwaggerCall.setHttp(http);
	}

	public void run(String method, String pathInfo, ISwaggerRequest req, ISwaggerResponse resp) {
		try {
			ss.prepare(method, pathInfo);
			ss.run(req, resp);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

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

	public void testToken_Simple_Success() throws JSONException {
		TokenPostRequest req = new TokenPostRequest();
		TokenPostResponse resp = new TokenPostResponse();
		run("POST", "/token", req, resp);

		assertEquals(policy, resp.policy);
		assertTrue(resp.token.startsWith("TOKEN-"));
	}

	public void testAuth_ByToken_Success() throws JSONException {
		AuthPostRequest req = new AuthPostRequest();
		AuthPostResponse resp = new AuthPostResponse();
		req.token = token;
		run("POST", "/auth", req, resp);

		assertEquals(36, resp.authkey.length());
		assertEquals(token, resp.token);
		assertEquals(cpf, resp.cpf);
		assertEquals(kind, resp.kind);
		assertEquals(name, resp.name);
	}

	public void testStore_Simple_Success() throws JSONException {
		StorePostRequest req = new StorePostRequest();
		StorePostResponse resp = new StorePostResponse();
		req.payload = sha1;
		run("POST", "/store", req, resp);

		assertEquals(36, resp.key.length());
		assertEquals("OK", resp.status);
	}

	private String getAuthKey() {
		AuthPostRequest req = new AuthPostRequest();
		AuthPostResponse resp = new AuthPostResponse();
		req.token = token;
		run("POST", "/auth", req, resp);
		return resp.authkey;
	}

	public void testList_Simple_Success() throws JSONException {
		ListPostRequest req = new ListPostRequest();
		ListPostResponse resp = new ListPostResponse();

		req.certificate = SwaggerUtils.base64Decode(certificate);
		req.authkey = getAuthKey();
		run("POST", "/list", req, resp);

		assertEquals(3, resp.list.size());
		assertEquals("testesigner", resp.list.get(0).system);
		assertNull(resp.status.get(0).errormsg);
	}

	public void testHash_Simple_Success() throws JSONException {
		HashPostRequest req = new HashPostRequest();
		HashPostResponse resp = new HashPostResponse();

		req.authkey = getAuthKey();
		req.id = id;
		req.certificate = SwaggerUtils.base64Decode(certificate);
		req.system = system;
		run("POST", "/hash", req, resp);

		assertEquals(sha1, SwaggerUtils.base64Encode(resp.sha1));
		assertEquals(sha256, SwaggerUtils.base64Encode(resp.sha256));
	}

	public void testSave_Simple_Success() throws JSONException {
		SavePostRequest req = new SavePostRequest();
		SavePostResponse resp = new SavePostResponse();

		req.id = id;
		req.code = code;
		req.certificate = SwaggerUtils.base64Decode(certificate);
		req.time = SwaggerUtils.parse(time);
		req.system = system;
		req.sha1 = SwaggerUtils.base64Decode(sha1);
		req.sha256 = SwaggerUtils.base64Decode(sha256);
		req.policy = policy;
		req.policyversion = policyversion;
		req.signature = SwaggerUtils.base64Decode(signature);
		run("POST", "/save", req, resp);

		assertEquals("OK", resp.status);
	}

}

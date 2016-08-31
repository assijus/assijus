package br.jus.trf2.assijus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.crivano.restservlet.RestUtils;

@SuppressWarnings("serial")
public class ClientCertServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String cert = req.getHeader("X-SSL-Client-Cert");
		String dn = req.getHeader("X-SSL-Client-ND");

		cert = cert.replace(" ", "");
		cert = cert.replace("-----BEGINCERTIFICATE-----", "");
		cert = cert.replace("-----ENDCERTIFICATE-----", "");
		
		// Store some information relevant to identify the user
		JSONObject sessionData = new JSONObject();

		// Parse certificate
		JSONObject blucreq2 = new JSONObject();
		try {
			// Call bluc-server hash webservice
			blucreq2.put("certificate", cert);
			JSONObject blucresp2 = RestUtils.restPost("bluc-certificate", null,
					Utils.getUrlBluCServer() + "/certificate", blucreq2);
			
			String subject = blucresp2.getString("subject");
			String cn = blucresp2.getString("cn");
			String name = blucresp2.getString("name");
			String cpf = blucresp2.getString("cpf");
			
			sessionData.put("certificate", cert);
			sessionData.put("name", name);
			sessionData.put("cpf", cpf);
			sessionData.put("kind", "client-cert");
			sessionData.put("password", Utils.getRetrievePassword());
			
			String payload = sessionData.toString();
			String key = Utils.dbStore(payload);
			
			String qs = req.getQueryString();
			if (qs == null)
				qs = "";
			else 
				qs += "&";
			
			qs += "authkey=" + key;
			
			resp.sendRedirect("/assijus/?" +  qs);
			
			//resp.sendError(400, sessionData.toString(3));
		} catch (Exception e) {
			resp.sendError(500, e.getMessage());
		}
	}
}

package br.jus.trf2.assijus;

import java.io.IOException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import br.jus.trf2.restservlet.RestUtils;

@SuppressWarnings("serial")
public class ViewServlet extends HttpServlet {
	protected String urlapolo;
	protected String urlsiga;
	protected String urlblucserver;

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		try {
			JSONObject req = new JSONObject();
			urlapolo = Utils.getUrlBaseApolo();
			urlsiga = Utils.getUrlBaseSigaDoc();
			urlblucserver = Utils.getUrlBaseBluCServer();

			req.put("certificate", request.getParameter("certificate"));
			req.put("token", request.getParameter("token"));
			req.put("urlView", request.getParameter("urlView"));

			JSONObject resp = new JSONObject();
			run(request, response, req, resp);

			byte[] pdf = Base64.decode(resp.getString("pdf"));
			response.setContentLength(pdf.length);
			response.setContentType("application/pdf");
			response.getOutputStream().write(pdf);
			response.getOutputStream().flush();
		} catch (Exception e) {
			RestUtils.writeJsonError(response, e, getContext(), "assijus");
		}
	}

	protected void run(HttpServletRequest request,
			HttpServletResponse response, JSONObject req, final JSONObject resp)
			throws Exception {
		// Parse request
		String certificate = req.getString("certificate");
		String urlView = req.getString("urlView");
		String password = Utils.choosePassword(urlView);
		urlView = Utils.fixUrl(urlView);

		String token = req.getString("token");
		String cpf = Utils.assertValidToken(token, urlblucserver);

		JSONObject gedreq = new JSONObject();
		gedreq.put("certificate", certificate);

		if (urlView.startsWith(urlapolo))
			gedreq.put("urlapi", urlapolo);
		if (urlView.startsWith(urlsiga))
			gedreq.put("urlapi", urlsiga);

		gedreq.put("password", password);
		gedreq.put("cpf", cpf);

		// Call document repository hash webservice
		JSONObject gedresp = RestUtils.getJsonObjectFromJsonPost(new URL(
				urlView), gedreq, "ged-view");

		// Produce response
		String doc = gedresp.getString("doc");
		resp.put("pdf", doc);
	}

	protected String getContext() {
		return "obter o pdf";
	}
}

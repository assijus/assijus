package br.jus.trf2.assijus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import br.jus.trf2.restservlet.RestServlet;
import br.jus.trf2.restservlet.RestUtils;

public abstract class AssijusServlet extends RestServlet {
	protected String urlapolo;
	protected String urlsiga;
	protected String urlblucserver;

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		urlapolo = Utils.getUrlBaseApolo();
		urlsiga = Utils.getUrlBaseSigaDoc();
		urlblucserver = Utils.getUrlBaseBluCServer();
		super.doPost(request, response);
	}

	@Override
	protected String getService() {
		return "assijus";
	}

	@Override
	protected String getSwagger() {
		return "/api/v1/swagger.yaml";
	}

}

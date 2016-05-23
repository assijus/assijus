package br.jus.trf2.assijus;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.crivano.restservlet.RestServlet;

public abstract class AssijusServlet extends RestServlet {
	protected String urltextoweb;
	protected String urlapolo;
	protected String urlsiga;
	protected String urlblucserver;

	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		urltextoweb = Utils.getUrlBaseTextoWeb();
		urlapolo = Utils.getUrlBaseApolo();
		urlsiga = Utils.getUrlBaseSigaDoc();
		urlblucserver = Utils.getUrlBaseBluCServer();
		super.doPost(request, response);
	}

	@Override
	protected String getService() {
		return "assijus";
	}
}

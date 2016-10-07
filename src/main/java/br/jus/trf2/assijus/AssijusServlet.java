package br.jus.trf2.assijus;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.crivano.swaggerservlet.Swagger;
import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;

public class AssijusServlet extends SwaggerServlet {
	private static final long serialVersionUID = 1756711359239182178L;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		SwaggerUtils.setCache(new MemCacheRedis());
		
		super.setAPI(IAssijus.class);

		super.setActionPackage("br.jus.trf2.assijus");
	}

	@Override
	protected String getService() {
		return "assijus";
	}

}

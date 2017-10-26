package br.jus.trf2.assijus;

import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;
import com.crivano.swaggerservlet.dependency.SwaggerServletDependency;
import com.crivano.swaggerservlet.dependency.TestableDependency;
import com.crivano.swaggerservlet.property.PrivateProperty;
import com.crivano.swaggerservlet.property.PublicProperty;
import com.crivano.swaggerservlet.property.RestrictedProperty;

public class AssijusServlet extends SwaggerServlet {
	private static final long serialVersionUID = 1756711359239182178L;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		SwaggerUtils.setCache(new MemCacheRedis());

		super.setAPI(IAssijus.class);

		super.setActionPackage("br.jus.trf2.assijus");

		super.addProperty(new PublicProperty("assijus.systems"));
		super.addProperty(new PublicProperty("assijus.popup.urls"));
		super.addProperty(new RestrictedProperty("blucservice.url"));
		for (String system : Utils.getSystems()) {
			super.addProperty(new RestrictedProperty(system + ".url"));
			super.addProperty(new PrivateProperty(system + ".password"));
		}
		super.setAuthorizationToProperties(SwaggerUtils.getProperty("assijus.properties.secret", null));

		addDependency(new SwaggerServletDependency("webservice", "blucservice", false, 0, 10000) {

			@Override
			public String getUrl() {
				return Utils.getUrlBluCServer();
			}

			@Override
			public String getResponsable() {
				return null;
			}

		});

		String[] systems = Utils.getSystems();
		for (final String system : systems) {
			addDependency(new SwaggerServletDependency("webservice", system, false, 0, 10000) {

				@Override
				public String getUrl() {
					return Utils.getUrl(system);
				}

				@Override
				public String getResponsable() {
					return null;
				}

			});
		}

		addDependency(new TestableDependency("cache", "redis", false, 0, 10000) {

			@Override
			public String getUrl() {
				return "redis://" + MemCacheRedis.getMasterHost() + ":" + MemCacheRedis.getMasterPort() + "/"
						+ MemCacheRedis.getDatabase() + " (" + "redis://" + MemCacheRedis.getSlaveHost() + ":"
						+ MemCacheRedis.getSlavePort() + "/" + MemCacheRedis.getDatabase() + ")";
			}

			@Override
			public boolean test() throws Exception {
				String uuid = UUID.randomUUID().toString();
				MemCacheRedis mc = new MemCacheRedis();
				mc.store("test", uuid.getBytes());
				String uuid2 = new String(mc.retrieve("test"));
				return uuid.equals(uuid2);
			}
		});
	}

	@Override
	public String getService() {
		return "assijus";
	}

}

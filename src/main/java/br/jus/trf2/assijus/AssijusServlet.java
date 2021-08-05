package br.jus.trf2.assijus;

import java.util.UUID;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import com.crivano.swaggerservlet.SwaggerServlet;
import com.crivano.swaggerservlet.SwaggerUtils;
import com.crivano.swaggerservlet.dependency.SwaggerServletDependency;
import com.crivano.swaggerservlet.dependency.TestableDependency;

public class AssijusServlet extends SwaggerServlet {
	private static final long serialVersionUID = 1756711359239182178L;
	static final int CERTIFICATE_TIMEOUT = 30;
	static final int VALIDATE_TIMEOUT = 30;
	static final int HASH_TIMEOUT = 30;
	static final int ENVELOPE_TIMEOUT = 30;
	static final int SYSTEM_HASH_TIMEOUT = 30;
	static final int SYSTEM_LIST_TIMEOUT = 30;
	static final int SYSTEM_SAVE_TIMEOUT = 30;
	static final int SYSTEM_SIGNATURE_TIMEOUT = 30;

	static AssijusServlet INSTANCE = null;

	@Override
	public void initialize(ServletConfig config) throws ServletException {
		INSTANCE = this;
		setAPI(IAssijus.class);
		setActionPackage("br.jus.trf2.assijus");

		addPublicProperty("systems", null);
		addPublicProperty("popup.urls", "http://localhost:8080");
		addRestrictedProperty("blucservice.url", "http://localhost:8080/blucservice/api/v1");
		addPublicProperty("siga.url", "http://siga.jfrj.jus.br");
		addPublicProperty("dotnet.download.url", "http://assijus.jfrj.jus.br/downloads/Microsoft-DOT-NET-Framework-x86-x64-AllOS-ENU.exe");
		addPublicProperty("java8.download.url", "resources/download/jre-8u301-macosx-x64.dmg");
		addPublicProperty("exibe.titulo.dr", "true");
		addPublicProperty("extensao.macos.version", "2.0.0");
		addPublicProperty("extensao.windows.version", "1.2.10.6");
		
		// Redis
		//
		addRestrictedProperty("redis.database", "10");
		addPrivateProperty("redis.password", null);
		addRestrictedProperty("redis.slave.port", "0");
		addRestrictedProperty("redis.slave.host", null);
		addRestrictedProperty("redis.master.host", "localhost");
		addRestrictedProperty("redis.master.port", "6379");

		if (getProperty("redis.password") != null)
			SwaggerUtils.setCache(new MemCacheRedis());

		addPrivateProperty("timestamp.issuer", null);
		if (getProperty("timestamp.issuer") != null) {
			addPublicProperty("timestamp.public.key");
			addPrivateProperty("timestamp.private.key");
		} else {
			addPublicProperty("timestamp.public.key", null);
			addPrivateProperty("timestamp.private.key", null);
		}

		addPublicProperty("login.issuer", null);
		addPublicProperty("login.systems", null);
		String[] loginsystems = Utils.getLoginSystems();
		if (loginsystems != null) {
			for (final String system : loginsystems) {
				addPrivateProperty(system + ".login.url.base");
				addPrivateProperty(system + ".login.url.redirect");
				addPrivateProperty(system + ".login.password");
			}
		}

		String[] systems = Utils.getSystems();
		if (systems != null) {
			for (final String system : systems) {
				addRestrictedProperty(system + ".url", "http://localhost:8080/" + system + "/api/v1");
				addPrivateProperty(system + ".password", null);
				addDependency(new SwaggerServletDependency("webservice", system, false, 0, 10000) {

					@Override
					public String getUrl() {
						return Utils.getUrl(system);
					}

				});
			}
		}
		addPublicProperty("timestamp.public.key");
		addPrivateProperty("timestamp.private.key");

		addDependency(new SwaggerServletDependency("webservice", "blucservice", false, 0, 10000) {

			@Override
			public String getUrl() {
				return Utils.getUrlBluCServer();
			}
		});

		if (getProperty("redis.password") != null)
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

	public static String getProp(String propertyName) {
		return INSTANCE.getProperty(propertyName);
	}
}

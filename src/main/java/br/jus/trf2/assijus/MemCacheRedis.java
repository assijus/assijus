package br.jus.trf2.assijus;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;

import com.crivano.swaggerservlet.IMemCache;
import com.crivano.swaggerservlet.SwaggerUtils;

public class MemCacheRedis implements IMemCache {
	private static JedisPool poolMaster;
	private static JedisPool poolSlave;

	static {
		redisConfig();
	}

	private static void redisConfig() {
		String masterhost = getMasterHost();
		int masterport = getMasterPort();
		String slavehost = getSlaveHost();
		int slaveport = getSlavePort();
		String password = getPassword();
		int database = getDatabase();

		poolMaster = new JedisPool(new JedisPoolConfig(), masterhost,
				masterport, Protocol.DEFAULT_TIMEOUT, password, database);

		if (slavehost != null)
			poolSlave = new JedisPool(new JedisPoolConfig(), slavehost,
					slaveport, Protocol.DEFAULT_TIMEOUT, password, database);
		else
			poolSlave = poolMaster;
	}

	public static int getDatabase() {
		return Integer.parseInt(SwaggerUtils.getProperty(
				"assijus.redis.database", "10"));
	}

	public static String getPassword() {
		return SwaggerUtils.getProperty("assijus.redis.password", null);
	}

	public static int getSlavePort() {
		return Integer.parseInt(SwaggerUtils.getProperty(
				"assijus.redis.slave.port", "0"));
	}

	public static String getSlaveHost() {
		return SwaggerUtils.getProperty("assijus.redis.slave.host",
				null);
	}

	public static String getMasterHost() {
		return SwaggerUtils.getProperty("assijus.redis.master.host",
				"localhost");
	}

	public static int getMasterPort() {
		return Integer.parseInt(SwaggerUtils.getProperty(
				"assijus.redis.master.port", "6379"));
	}

	@Override
	public void store(String sha1, byte[] ba) {
		try (Jedis jedis = poolMaster.getResource()) {
			jedis.set(SafeEncoder.encode(sha1), ba);
		}
	}

	@Override
	public byte[] retrieve(String sha1) {
		try (Jedis jedis = poolSlave.getResource()) {
			byte[] ba = jedis.get(SafeEncoder.encode(sha1));
			return ba;
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public byte[] remove(String sha1) {
		try (Jedis jedis = poolMaster.getResource()) {
			byte[] key = SafeEncoder.encode(sha1);
			byte[] ba = jedis.get(key);
			jedis.del(key);
			return ba;
		} catch (Exception e) {
			return null;
		}
	}

}

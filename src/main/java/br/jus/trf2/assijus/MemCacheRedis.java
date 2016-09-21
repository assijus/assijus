package br.jus.trf2.assijus;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;

import com.crivano.restservlet.RestUtils;

public class MemCacheRedis {
	private static JedisPool poolMaster;
	private static JedisPool poolSlave;

	static {
		redisConfig();
	}

	private static void redisConfig() {
		JedisPool p = null;
		String masterhost = RestUtils.getProperty("assijus.redis.master.host",
				"localhost");
		int masterport = Integer.parseInt(RestUtils.getProperty(
				"assijus.redis.master.port", "6379"));
		String slavehost = RestUtils.getProperty("assijus.redis.slave.host",
				null);
		int slaveport = Integer.parseInt(RestUtils.getProperty(
				"assijus.redis.slave.port", "0"));
		String password = RestUtils.getProperty("assijus.redis.password", null);
		int database = Integer.parseInt(RestUtils.getProperty(
				"assijus.redis.database", "10"));

		poolMaster = new JedisPool(new JedisPoolConfig(), masterhost,
				masterport, Protocol.DEFAULT_TIMEOUT, password, database);

		if (slavehost != null)
			poolSlave = new JedisPool(new JedisPoolConfig(), slavehost,
					slaveport, Protocol.DEFAULT_TIMEOUT, password, database);
		else
			poolSlave = poolMaster;
	}

	public static void cacheStore(String sha1, byte[] ba) {
		try (Jedis jedis = poolMaster.getResource()) {
			jedis.set(SafeEncoder.encode(sha1), ba);
		}
	}

	public static byte[] cacheRetrieve(String sha1) {
		try (Jedis jedis = poolSlave.getResource()) {
			byte[] ba = jedis.get(SafeEncoder.encode(sha1));
			return ba;
		} catch (Exception e) {
			return null;
		}
	}

	public static byte[] cacheRemove(String sha1) {
		try (Jedis jedis = poolMaster.getResource()) {
			byte[] key = SafeEncoder.encode(sha1);
			byte[] ba = jedis.get(key);
			jedis.del(key);
			return ba;
		} catch (Exception e) {
			return null;
		}
	}

	public static String dbStore(String payload) {
		String id = UUID.randomUUID().toString();
		cacheStore(id, payload.getBytes());
		return id;
	}

	public static String dbRetrieve(String id, boolean remove) {
		byte[] ba = null;
		if (remove)
			ba = cacheRemove(id);
		else
			ba = cacheRetrieve(id);
		if (ba == null)
			return null;
		String s = new String(ba);

		if (s == null || s.trim().length() == 0)
			return null;

		return s;
	}
}

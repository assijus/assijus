package br.jus.trf2.assijus;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;
import redis.clients.util.SafeEncoder;

import com.crivano.restservlet.IMemCache;
import com.crivano.restservlet.RestUtils;

public class MemCacheRedis implements IMemCache {
	private static JedisPool poolMaster;
	private static JedisPool poolSlave;

	static {
		redisConfig();
	}

	private static void redisConfig() {
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

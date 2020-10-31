package redis.redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class MyJedisPool {
	public static MyJedisPool INSTANCE = new MyJedisPool();
	private final JedisPool pool;

	private MyJedisPool() {
		JedisPoolConfig poolConfig = new JedisPoolConfig();
		poolConfig.setMaxIdle(32);
		poolConfig.setTestOnBorrow(true);
		pool = new JedisPool(poolConfig, "175.24.41.121", 6379, 1000, "wdnmd");
	}

	public Jedis getJedis() {
		return pool.getResource();
	}
}

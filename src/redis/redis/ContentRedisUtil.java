package redis.redis;

import redis.entity.Content;
import redis.entity.ContentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;

public class ContentRedisUtil extends MyRedisUtil {
	Logger logger;

	public ContentRedisUtil() {
		logger = LoggerFactory.getLogger(ContentRedisUtil.class);
	}

	@Override
	public void store(String key, Object content) {
		String s = ContentUtil.INSTANCE.compress((Content) content);
		Jedis jedis = MyJedisPool.INSTANCE.getJedis();
		jedis.lpush(key, s);
		jedis.set("mark" + key, "written");
		jedis.close();
	}

	@Override
	public ArrayList<Content> fetch(String key) {
		Jedis jedis = MyJedisPool.INSTANCE.getJedis();
		ArrayList<String> strings = (ArrayList<String>) jedis.lrange(key, 0, -1);
		jedis.close();
		ArrayList<Content> contents = new ArrayList<Content>();
		for (String s : strings) {
			contents.add(ContentUtil.INSTANCE.parse(s));
		}
		return contents;
	}

	@Override
	public void delObject(String key) {
		Jedis jedis = MyJedisPool.INSTANCE.getJedis();
		jedis.del(key);
		jedis.close();
	}
}

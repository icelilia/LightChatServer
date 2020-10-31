package redis;

import redis.entity.Content;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import redis.redis.ContentRedisUtil;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

public class Redis {
	static Logger logger;

	static {
		logger = LoggerFactory.getLogger(Redis.class);
	}

	public static void send(int sessionId, String from, String to, Date date, String messageText, String kind) {
		final SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-mm-dd-hh-mm-ss");
		Content content = new Content(from, to, dateForm.format(date), messageText, kind);
		String sessionIdString = String.valueOf(sessionId);
		sendObject(content, sessionIdString);
	}

	public static void send(int sessionId, Content content) {
		String sessionIdString = String.valueOf(sessionId);
		sendObject(content, sessionIdString);
	}

	public static String receive(int sessionId) {
		String sessionIdString = String.valueOf(sessionId);
		return getObject(sessionIdString);
	}

	public static void sendObject(Object obj, String uuid) {
		ContentRedisUtil.INSTANCE.store(uuid, obj);
		logger.info("send " + obj);
	}

	public static String getObject(String key) {

		ArrayList<Content> contents = ContentRedisUtil.INSTANCE.fetch(key);

		Collections.sort(contents, new Comparator<Content>() {
			public int compare(Content o1, Content o2) {
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-mm-dd-hh-mm-ss");
					Date d1 = dateFormat.parse(o1.getTime());
					Date d2 = dateFormat.parse(o2.getTime());
					if (d1.compareTo(d2) > 0)
						return 1;
					else
						return -1;
				} catch (ParseException e) {
					e.printStackTrace();
				}
				return 0;
			}
		});

		JSONArray list = new JSONArray();

		for (Content obj : contents) {
			Content content = (Content) obj;
			JSONObject json = new JSONObject();
			json.put("content", content.getContent());
			json.put("from", content.getFrom());
			json.put("time", content.getTime());
			json.put("to", content.getTo());
			json.put("kind", content.getKind());
			list.add(json);
		}

		logger.info("get" + list.toJSONString());

		return list.toJSONString();
	}

	public static void delObject(int key) {
		String k = String.valueOf(key);
		ContentRedisUtil.INSTANCE.delObject(k);
	}
}

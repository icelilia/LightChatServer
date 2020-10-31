package redis.entity;

public class Content {
	String from;
	String to;
	String time;
	String content;
	String kind;

	public Content(String from, String to, String time, String content, String kind) {
		this.from = from;
		this.to = to;
		this.time = time;
		this.content = content;
		this.kind = kind;
	}

	@Override
	public String toString() {
		return "Content{" + "from='" + from + '\'' + ", to='" + to + '\'' + ", time='" + time + '\'' + ", content='"
				+ content + '\'' + ", kind='" + kind + '\'' + '}';
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getKind() {
		return kind;
	}

	public void setKind(String kind) {
		this.kind = kind;
	}
}

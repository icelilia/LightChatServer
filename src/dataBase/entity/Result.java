package dataBase.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Result {
	private String sessionId;
	private String receiverUsername;
	private String result;
	private String time;

	public Result() {

	}

	public Result(int sessionId, String receiverUsername, String result, Date date) {
		this.sessionId = String.valueOf(sessionId);
		this.receiverUsername = receiverUsername;
		this.result = result;
		final SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
		this.time = dateForm.format(date);
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getReceiverUsername() {
		return receiverUsername;
	}

	public void setReceiverUsername(String username) {
		this.receiverUsername = username;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}
}

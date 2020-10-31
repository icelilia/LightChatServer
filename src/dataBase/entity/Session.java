package dataBase.entity;

import java.util.Vector;

public class Session {
	public static int sessionNum = 0;

	private int sessionId = sessionNum;

	private String managerUsername;
	
	private String sessionName;

	private Vector<String> sessionMembers = new Vector<String>();

	public int getSessionId() {
		return sessionId;
	}

	public void setSessionId(int sessionId) {
		this.sessionId = sessionId;
	}

	// 获得对话中的用户列表
	synchronized public Vector<String> getSessionMembers() {
		return sessionMembers;
	}

	// 这个方法是为了Fastjson能够反序列化
	synchronized public void setSessionMembers(Vector<String> sessionMembers) {
		this.sessionMembers = sessionMembers;
	}

	// 向对话中添加新的用户
	synchronized public void addSessionMember(String sessionMemberUsername) {
		this.sessionMembers.add(sessionMemberUsername);
	}

	public String getManagerUsername() {
		return managerUsername;
	}

	public void setManagerUsername(String managerUsername) {
		this.managerUsername = managerUsername;
	}

	public String getSessionName() {
		return sessionName;
	}

	public void setSessionName(String sessionName) {
		this.sessionName = sessionName;
	}
}

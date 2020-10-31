package dataBase.entity;

import java.util.Vector;

public class User {
	private String username;
	private String password;
	private String nickname;
	private String avatarAddress;

	// 好友列表，这里只用用户名，防止后面改昵称时牵扯太多
	private Vector<String> friendUsernames = new Vector<String>();

	// 会话列表
	private Vector<Integer> sessionIds = new Vector<Integer>();

	// 请求列表
	private Vector<Request> requests = new Vector<Request>();

	// 结果列表
	private Vector<Result> results = new Vector<Result>();

	// 一些没什么用的属性
	private String phoneNumber;
	private String email;
	private String occupation;
	private String location;

	public User() {
	}

	public User(String username, String password, String nickname) {
		this.username = username;
		this.password = password;
		this.nickname = nickname;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	synchronized public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	public String getAvatarAddress() {
		return avatarAddress;
	}

	public void setAvatarAddress(String avatarAddress) {
		this.avatarAddress = avatarAddress;
	}

	public Vector<String> getFriendUsernames() {
		return friendUsernames;
	}

	public void setFriendUsernames(Vector<String> friendUsernames) {
		this.friendUsernames = friendUsernames;
	}

	public void addFriendUsername(String friendUsername) {
		this.friendUsernames.add(friendUsername);
	}

	public Vector<Integer> getSessionIds() {
		return sessionIds;
	}

	public void setSessionIds(Vector<Integer> sessions) {
		this.sessionIds = sessions;
	}

	public void addSession(int sessionId) {
		this.sessionIds.add(sessionId);
	}

	public Vector<Request> getRequests() {
		return requests;
	}

	public void setRequests(Vector<Request> requests) {
		this.requests = requests;
	}

	public void addRequest(Request request) {
		this.requests.add(request);
	}

	public Vector<Result> getResults() {
		return results;
	}

	public void setResults(Vector<Result> results) {
		this.results = results;
	}

	public void addResults(Result result) {
		this.results.add(result);
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getOccupation() {
		return occupation;
	}

	public void setOccupation(String occupation) {
		this.occupation = occupation;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public void justInformation() {
		this.password = null;
		this.friendUsernames = null;
		this.sessionIds = null;
		this.requests = null;
		this.results = null;
	}
}

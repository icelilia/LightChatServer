package dataBase.entity;

import java.util.Vector;

public class Session {
    /**
     * 当前的会话数量，会根据数据库中的最大sessionId来设定
     */
    public static int sessionNum = 0;

    /**
     * 会话编号
     */
    private int sessionId = sessionNum;

    /**
     * 会话Manager的用户名
     */
    private String managerUsername;

    /**
     * 会话名（群名），如果是普通会话，则为null
     */
    private String sessionName;

    /**
     * 成员列表
     */
    private Vector<String> sessionMembers = new Vector<>();

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获得对话中的用户列表
     *
     * @return 用户名Vector
     */
    public Vector<String> getSessionMembers() {
        return sessionMembers;
    }

    public void setSessionMembers(Vector<String> sessionMembers) {
        this.sessionMembers = sessionMembers;
    }

    /**
     * 向对话中添加新的用户
     *
     * @param sessionMemberUsername 新成员用户名
     */
    public void addSessionMember(String sessionMemberUsername) {
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

    /**
     * 修改群名
     *
     * @param sessionName 新群名
     */
    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }
}

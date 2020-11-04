package dataBase.entity;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Request {
    private final static SimpleDateFormat dateForm = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
    private int sessionId;
    private String senderUsername;
    private String checkMessage;
    private String time;

    public Request(int sessionId, String senderUsername, String checkMessage, Date date) {
        this.sessionId = sessionId;
        this.senderUsername = senderUsername;
        this.checkMessage = checkMessage;
        this.setTime(dateForm.format(date));
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String username) {
        this.senderUsername = username;
    }

    public String getCheckMessage() {
        return checkMessage;
    }

    public void setCheckMessage(String checkMessage) {
        this.checkMessage = checkMessage;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}

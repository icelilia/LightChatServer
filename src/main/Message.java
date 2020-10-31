package main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;
import java.text.ParseException;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;

import dataBase.controller.DataBase;
import dataBase.entity.*;
import redis.Redis;
import redis.entity.Content;

public class Message {
    private String messageNumber; // 对应开发文档里的编号
    private String messageField1; // 字段1
    private String messageField2; // 字段2
    private String messageField3; // 字段3

    public Message() {
    }

    public Message(String messageNumber) {
        this.messageNumber = messageNumber;
    }

    public String getMessageNumber() {
        return messageNumber;
    }

    public void setMessageNumber(String messageNumber) {
        this.messageNumber = messageNumber;
    }

    public String getMessageField1() {
        return messageField1;
    }

    public void setMessageField1(String messageField1) {
        this.messageField1 = messageField1;
    }

    public String getMessageField2() {
        return messageField2;
    }

    public void setMessageField2(String messageField2) {
        this.messageField2 = messageField2;
    }

    public String getMessageField3() {
        return messageField3;
    }

    public void setMessageField3(String messageField3) {
        this.messageField3 = messageField3;
    }

    public static Message receiveMessage(DataInputStream dataInputStream) throws IOException {
        // 裸奔版
        final int MAX_SIZE = 0xffff;
        byte[] messageByteArray = new byte[MAX_SIZE];
        dataInputStream.read(messageByteArray);
        String messageJSONString = new String(messageByteArray, "utf-8");
        System.err.println("收到包：" + messageJSONString);
        return JSON.parseObject(messageJSONString, Message.class);

        // 首部长度校验版
        // int length = dataInputStream.readInt();
        // byte[] messageByteArray = new byte[length];
        // dataInputStream.read(messageByteArray);
        // return JSON.parseObject(new String(messageByteArray, "utf-8"),
        // Message.class);
    }

    private static void sendMessageAsByteArray(DataOutputStream dataOutputStream, Message message) throws IOException {
        byte[] messageByteArray;
        messageByteArray = JSON.toJSONString(message).getBytes("utf-8");
        dataOutputStream.write(messageByteArray);
        System.out.println("发送包：" + JSON.toJSONString(message));
    }

    /**
     * 注销登录
     * 将username从socketTable中删除，并输出日志
     *
     * @param username 待注销用户的用户名
     */
    public void msg0(DataBase dataBase, String username) {
        dataBase.delSocket(username);
        System.out.println("用户" + "[" + username + "]" + "已登出");
    }

    /**
     * 初始化
     * 发送一个1r消息，表示服务器目前能够以处理这个连接
     */
    public void msg1(DataOutputStream dataOutputStream) throws IOException {
        setMessageNumber("1r");
        sendMessageAsByteArray(dataOutputStream, this);
    }

    /**
     * 登录
     * 检查用户名和密码是否对应，发送一个2r消息并附带登录结果
     *
     * @return true：登录成功；false：登录失败
     */
    public boolean msg2(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
        // 先获得用户名和密码
        String username = getMessageField1();
        String password = getMessageField2();

        Message msg = new Message("2r");

        // 检验登录
        if (!dataBase.checkLogin(username, password)) {
            msg.setMessageField1("0");
            msg.setMessageField2("用户名或密码错误");
            sendMessageAsByteArray(dataOutputStream, msg);
            return false;
        } else {
            // 检验该用户是否已经登录
            if (dataBase.searchOnlineUserByUsername(username) != null) {
                msg.setMessageField1("0");
                msg.setMessageField2("该用户已登录");
                sendMessageAsByteArray(dataOutputStream, msg);
                return false;
            }
            msg.setMessageField1("1");
            msg.setMessageField2("OK");

            // 这里面包含了User对象的所有信息
            User user = dataBase.getUserByUsername(username);

            msg.setMessageField3(JSON.toJSONString(user));
            sendMessageAsByteArray(dataOutputStream, msg);

            System.out.println("用户" + "[" + username + "]" + "已登录");
            return true;
        }
    }

    /**
     * 注册
     * 检查用户名和密码是否合法后，发送一个3r消息并附带注册结果
     */
    public void msg3(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
        User user = JSON.parseObject(getMessageField1(), User.class);

        String username = user.getUsername();
        String password = user.getPassword();

        Message message = new Message("3r");

        String pattern1 = "[A-Za-z0-9_]{6,16}";
        String pattern2 = "[A-Za-z0-9_]{6,16}";

        if (!Pattern.matches(pattern1, username)) {
            message.setMessageField1("0");
            message.setMessageField2("用户名格式非法");
            sendMessageAsByteArray(dataOutputStream, message);
        } else if (!Pattern.matches(pattern2, password)) {
            message.setMessageField1("0");
            message.setMessageField2("密码格式非法");
            sendMessageAsByteArray(dataOutputStream, message);
        } else if (!dataBase.checkUsernameUniqueness(username)) {
            message.setMessageField1("0");
            message.setMessageField2("用户名已被占用");
            sendMessageAsByteArray(dataOutputStream, message);
        } else {
            dataBase.registerUser(user);
            message.setMessageField1("1");
            message.setMessageField2("OK");
            message.setMessageField3(JSON.toJSONString(user));

            sendMessageAsByteArray(dataOutputStream, message);

            // 日志显示
            System.out.println("用户" + "[" + username + "]" + "已注册");
        }
    }

    /**
     * 请求好友列表，4号消息的处理方法。检索数据库后，返回发送一个由若干个4r消息组成的消息数组。
     *
     * @param dataBase         数据库对象引用
     * @param username         被请求的用户名
     * @param dataOutputStream 输出流对象引用
     * @throws IOException 流IO错误
     */
    public void message4(DataBase dataBase, DataOutputStream dataOutputStream, String username) throws IOException {
        // 先获得所有的好友对象
        Vector<User> friends = dataBase.getFriends(username);
        Message message;
        message = new Message("4r");
        message.setMessageField1(String.valueOf(friends.size()));
        message.setMessageField2(JSONArray.toJSONString(friends));
        sendMessageAsByteArray(dataOutputStream, message);
    }

    /**
     * 获取历史聊天记录，5号消息的处理方法。检索redis后，返回发送一个由若干个5r消息组成的消息数组。
     *
     * @param dataBase         数据库对象引用
     * @param dataOutputStream 输出流对象引用
     * @param username         被请求的用户名
     * @throws IOException 流IO错误
     */
    public void message5(DataBase dataBase, DataOutputStream dataOutputStream, String username) throws IOException {
        // 获取用户所有的session
        Vector<Integer> sessions = dataBase.getSessions(username);

        Vector<Message> messages = new Vector<Message>();
        Message message;

        // 遍历sessions，从redis中搜索聊天记录
        for (Integer sessionId : sessions) {
            message = new Message("5r");
            message.setMessageField1(String.valueOf(sessionId));
            message.setMessageField2(dataBase.getSessionName(sessionId));
            message.setMessageField3(Redis.receive(sessionId));
            messages.add(message);
        }
        message = new Message("5r");
        message.setMessageField1(String.valueOf(messages.size()));
        message.setMessageField2(JSONArray.toJSONString(messages));
        sendMessageAsByteArray(dataOutputStream, message);
    }

    /**
     * 创建会话，6号消息的处理方法。在数据库中新建一个会话，并将创建者设置为会话管理员，返回发送一个6r消息并附带新会话的sessionId。
     *
     * @param dataBase         数据库对象引用
     * @param creatorUsername  创建者的用户名
     * @param dataOutputStream 输出流对象引用
     * @throws IOException 流IO错误
     */
    public void message6(DataBase dataBase, DataOutputStream dataOutputStream, String creatorUsername)
            throws IOException {
        String sessionName = getMessageField1();
        if (sessionName != null && !sessionName.contentEquals("")) {
            // 创建群聊
            int sessionId = dataBase.createSession(creatorUsername, sessionName);
            Message message = new Message("6r");
            message.setMessageField1(String.valueOf(sessionId));
            sendMessageAsByteArray(dataOutputStream, message);
        } else {
            int sessionId = dataBase.createSession(creatorUsername);
            Message message = new Message("6r");
            message.setMessageField1(String.valueOf(sessionId));
            sendMessageAsByteArray(dataOutputStream, message);
        }
    }

    /**
     * 加入会话，7号消息的处理方法。
     * 将messageField1字段所表示的invitee加入至messageField2字段所表示的session中去。若该用户已存在在此会话中，则什么也不做。
     *
     * @param dataBase         数据库对象引用
     * @param dataOutputStream 输出流对象引用
     * @throws IOException 流IO错误
     */
    public void message7(DataBase dataBase, DataOutputStream dataOutputStream) throws IOException {
        // 目标用户
        String inviteeUsername = getMessageField1();
        // 目标会话
        int sessionId = Integer.parseInt(getMessageField2());

        Message message = new Message("7r");
        message.setMessageField1("0");
        if (dataBase.joinSession(inviteeUsername, sessionId)) {
            message.setMessageField1("1");
        }
        Message.sendMessageAsByteArray(dataOutputStream, message);
    }

    public void message8(DataBase dataBase, DataOutputStream dataOutputStream, String username) throws IOException {
        Vector<Request> requests = dataBase.getRequests(username);
        Message message;
        message = new Message("8r");
        message.setMessageField1(String.valueOf(requests.size()));
        message.setMessageField2(JSONArray.toJSONString(requests));
        sendMessageAsByteArray(dataOutputStream, message);
    }

    /**
     * 发送信息，9号消息的处理方法。一成不变地（除了messageNumber）将其转发给对应会话中除了发送者外所有的用户。
     *
     * @param dataBase       数据库引用
     * @param senderUsername 发送者用户名，即应LinkThread中储存的username
     * @throws IOException 向所有接收方的InputStream中写入数据时可能发生IOException
     */
    public void message9(DataBase dataBase, String senderUsername) throws IOException {
        // 内容除了messageNumber外不会变
        setMessageNumber("9r");
        // 会话编号
        int sessionId = Integer.parseInt(getMessageField1());
        //
        String contentString = getMessageField2();
        Content content = JSON.parseObject(contentString, Content.class);

        // 储存在redis中
        Redis.send(sessionId, content);

        // 获得该会话中的所有用户
        Vector<String> users = dataBase.getMembers(sessionId);

        Socket socket;
        OutputStream outputStream;
        DataOutputStream dataOutputStream;

        // 转发给会话中所有的用户
        for (String username : users) {
            socket = dataBase.searchOnlineUserByUsername(username);
            // 为空表示未上线，直接跳过
            if (socket == null) {
                continue;
            }
            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            sendMessageAsByteArray(dataOutputStream, this);
        }
    }

    /**
     * 好友或群聊请求：A==>服务端。A向B发出申请，先经过服务器。
     * 无论B在线与否，在B的请求列表里添加上A的请求信息，同时，若B在线，则调用message11方法立即向B发送一个11r消息。
     *
     * @param dataBase          数据库的引用
     * @param requestorUsername 发送方的用户名
     * @throws IOException 流IO异常
     */
    public void message10(DataBase dataBase, String requestorUsername) throws IOException {
        String receiverUsername = getMessageField1();
        String checkMessage = getMessageField2();
        int sessionId = Integer.parseInt(getMessageField3());

        Date date = new Date();

        // 无论是否在线，先添加至请求列表
        receiverUsername = dataBase.addRequest(requestorUsername, receiverUsername, checkMessage, sessionId, date);

        // 判断接收方是否在线
        Socket receiverSocket = dataBase.searchOnlineUserByUsername(receiverUsername);

        if (receiverSocket != null) {
            // 接收方在线则直接发送11号消息
            Request request = new Request(sessionId, requestorUsername, checkMessage, date);
            message11(receiverSocket, request);
        }
    }

    /**
     * 好友请求：服务端==>B。服务端将申请转发给B。注意，该方法只有在B在线时才会调用。
     */
    private void message11(Socket receiverSocket, Request request) throws IOException {
        Message message = new Message("11r");
        message.setMessageField1(JSON.toJSONString(request));
        DataOutputStream dataOutputStream = new DataOutputStream(receiverSocket.getOutputStream());
        sendMessageAsByteArray(dataOutputStream, message);
    }

    /**
     * 好友或群聊请求：B==>服务端。B确认后将确认信息发送给服务端。 无论同意还是通过，都将从B的请求列表里删除A的请求。
     * 无论A在线与否，都将在A的结果列表里记录结果，同时，如果A在线，则调用message13方法立即向A发送一个13r消息。
     *
     * @param dataBase
     * @param receiverUsername
     * @throws IOException
     * @throws ParseException
     */
    public void message12(DataBase dataBase, String receiverUsername) throws IOException, ParseException {
        String requestorUsername = getMessageField1();
        int sessionId = Integer.parseInt(getMessageField2());
        String result = getMessageField3();

        dataBase.checkRequest(requestorUsername, receiverUsername, sessionId, result);

        Socket requestorSocket = dataBase.searchOnlineUserByUsername(requestorUsername);
        if (requestorSocket != null) {
            message13(requestorSocket, receiverUsername, result);
        }
    }

    /**
     * 好友请求：服务端==>A。服务端将结果发送给A。注意，该方法只有在A在线时才会调用。
     *
     * @param requestorSocket
     * @param receiverUsername
     * @param result
     * @throws IOException
     */
    private void message13(Socket requestorSocket, String receiverUsername, String result) throws IOException {
        Message message = new Message("13r");
        message.setMessageField1(receiverUsername);
        message.setMessageField2(result);
        DataOutputStream dataOutputStream = new DataOutputStream(requestorSocket.getOutputStream());
        sendMessageAsByteArray(dataOutputStream, message);
    }

    public void message14(DataBase dataBase, DataOutputStream dataOutputStream, String username) throws IOException {
        Vector<Result> results = dataBase.getResults(username);
        Message message;
        message = new Message("14r");
        message.setMessageField1(String.valueOf(results.size()));
        message.setMessageField2(JSONArray.toJSONString(results));
        sendMessageAsByteArray(dataOutputStream, message);
    }

    public void message15(DataBase dataBase, DataOutputStream dataOutputStream, String activeUsername)
            throws IOException {
        String passiveUsername = getMessageField1();
        dataBase.delFriend(activeUsername, passiveUsername);
        Socket passiveUserSocket = dataBase.searchOnlineUserByUsername(passiveUsername);
        if (passiveUserSocket != null) {
            message16(passiveUserSocket, activeUsername);
        }
    }

    private void message16(Socket passiveUserSocket, String activeUsername) throws IOException {
        Message message = new Message("16r");
        message.setMessageField1(activeUsername);
        DataOutputStream dataOutputStream = new DataOutputStream(passiveUserSocket.getOutputStream());
        sendMessageAsByteArray(dataOutputStream, message);
    }

    public void message17(DataBase dataBase, String username) {
        int sessionId = Integer.parseInt(getMessageField1());
        dataBase.quitSession(username, sessionId);
    }

    public void message18(DataBase dataBase, String username) {
        String userJSONString = getMessageField1();
        User user = JSON.parseObject(userJSONString, User.class);
        dataBase.updateUserInfo(user);
    }
}

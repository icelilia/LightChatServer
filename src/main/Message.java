package main;

import java.io.IOException;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
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

    /**
     * int转byte[]
     */
    private static byte[] intToByteArray(int x) {
        byte[] result = new byte[4];
        //由高位到低位
        result[0] = (byte) ((x >> 24) & 0xFF);
        result[1] = (byte) ((x >> 16) & 0xFF);
        result[2] = (byte) ((x >> 8) & 0xFF);
        result[3] = (byte) (x & 0xFF);
        return result;
    }

    /**
     * 接收msg方法更新，添加了首部长度（首部长度只表示后续消息的长度，并不包含自身这个int）
     * int + msg
     * 4   + length
     */
    public static Message receiveMessage(final DataInputStream dataInputStream) throws IOException {
        //首部长度校验版
        int length = dataInputStream.readInt();
        byte[] messageByteArray = new byte[length];
        if (dataInputStream.read(messageByteArray) != length) {
            // 错误情况，回头仔细考虑一下，这里就暂时强行执行下去
            System.err.println("接收包时，长度与首部长度不等，数据丢失");
        }
        String jsonString = new String(messageByteArray, StandardCharsets.UTF_8);
        System.out.println("接收包：" + jsonString);
        return JSON.parseObject(jsonString, Message.class);
    }

    /**
     * 发送msg方法更新，添加了首部长度（首部长度只表示后续消息的长度，并不包含自身这个int）
     * int + msg
     * 4   + length
     *
     * @param msg 待发送的消息对象
     */
    public static void sendMsg(final DataOutputStream dataOutputStream, final Message msg) throws IOException {
        String jsonString = JSON.toJSONString(msg);
        byte[] temp = jsonString.getBytes(StandardCharsets.UTF_8);
        int length = temp.length;
        byte[] messageByteArray = new byte[4 + length];
        System.arraycopy(intToByteArray(length), 0, messageByteArray, 0, 4);
        System.arraycopy(temp, 0, messageByteArray, 4, length);
        dataOutputStream.write(messageByteArray);
        System.out.println("发送包：" + jsonString);
    }

    /**
     * 注销登录
     * 将username从socketTable中删除，并输出日志
     *
     * @param username 待注销用户的用户名
     */
    public void msg00(final DataBase dataBase, final String username) {
        dataBase.delSocket(username);
        System.out.println("用户" + "[" + username + "]" + "已登出");
    }

    /**
     * 初始化
     * 发送一个1r消息，表示服务器目前能够以处理这个连接
     */
    public void msg01(final DataOutputStream dataOutputStream) throws IOException {
        setMessageNumber("1r");
        sendMsg(dataOutputStream, this);
    }

    /**
     * 登录
     * 检查用户名和密码是否对应，发送一个2r消息并附带登录结果
     *
     * @return true：登录成功；false：登录失败
     */
    public boolean msg02(final DataBase dataBase, final DataOutputStream dataOutputStream) throws IOException {
        // 先获得用户名和密码
        String username = getMessageField1();
        String password = getMessageField2();

        Message msg = new Message("2r");

        // 检验登录
        if (!dataBase.checkLogin(username, password)) {
            msg.setMessageField1("0");
            msg.setMessageField2("用户名或密码错误");
            sendMsg(dataOutputStream, msg);
            return false;
        } else {
            // 检验该用户是否已经登录
            if (dataBase.searchOnlineUserByUsername(username) != null) {
                msg.setMessageField1("0");
                msg.setMessageField2("该用户已登录");
                sendMsg(dataOutputStream, msg);
                return false;
            }
            msg.setMessageField1("1");
            msg.setMessageField2("OK");

            // 这里面包含了User对象的所有信息
            User user = dataBase.getUserByUsername(username);

            msg.setMessageField3(JSON.toJSONString(user));
            sendMsg(dataOutputStream, msg);

            System.out.println("用户" + "[" + username + "]" + "已登录");
            return true;
        }
    }

    /**
     * 注册
     * 检查用户名和密码是否合法后，发送一个3r消息并附带注册结果
     */
    public void msg03(final DataBase dataBase, final DataOutputStream dataOutputStream) throws IOException {
        User user = JSON.parseObject(getMessageField1(), User.class);

        String username = user.getUsername();
        String password = user.getPassword();

        Message message = new Message("3r");

        String pattern1 = "[A-Za-z0-9_]{6,16}";
        String pattern2 = "[A-Za-z0-9_]{6,16}";

        if (!Pattern.matches(pattern1, username)) {
            message.setMessageField1("0");
            message.setMessageField2("用户名格式非法");
            sendMsg(dataOutputStream, message);
        } else if (!Pattern.matches(pattern2, password)) {
            message.setMessageField1("0");
            message.setMessageField2("密码格式非法");
            sendMsg(dataOutputStream, message);
        } else if (!dataBase.checkUsernameUniqueness(username)) {
            message.setMessageField1("0");
            message.setMessageField2("用户名已被占用");
            sendMsg(dataOutputStream, message);
        } else {
            dataBase.registerUser(user);
            message.setMessageField1("1");
            message.setMessageField2("OK");
            message.setMessageField3(JSON.toJSONString(user));

            sendMsg(dataOutputStream, message);

            // 日志显示
            System.out.println("用户" + "[" + username + "]" + "已注册");
        }
    }

    /**
     * 请求好友列表
     *
     * @param username 请求者用户名
     */
    public void msg04(final DataBase dataBase, final DataOutputStream dataOutputStream, final String username) throws IOException {
        // 先获得所有的好友对象
        Vector<User> friends = dataBase.getFriends(username);
        Message message;
        message = new Message("4r");
        message.setMessageField1(String.valueOf(friends.size()));
        message.setMessageField2(JSONArray.toJSONString(friends));
        sendMsg(dataOutputStream, message);
    }

    /**
     * 检索Redis，获取历史聊天记录
     *
     * @param username 请求者用户名
     */
    public void msg05(final DataBase dataBase, final DataOutputStream dataOutputStream, final String username) throws IOException {
        // 获取用户所有的session
        Vector<Integer> sessions = dataBase.getSessions(username);

        Vector<Message> messages = new Vector<>();
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
        sendMsg(dataOutputStream, message);
    }

    /**
     * 创建会话
     * 在数据库中新建一个会话，并将创建者设置为会话管理员，返回发送一个6r消息并附带新会话的sessionId
     *
     * @param dataBase         数据库对象引用
     * @param creatorUsername  创建者的用户名
     * @param dataOutputStream 输出流对象引用
     * @throws IOException 流IO错误
     */
    public void msg06(final DataBase dataBase, final DataOutputStream dataOutputStream, final String creatorUsername) throws IOException {
        String sessionName = getMessageField1();
        if (sessionName != null && !sessionName.contentEquals("")) {
            // 创建群聊
            int sessionId = dataBase.createSession(creatorUsername, sessionName);
            Message msg = new Message("6r");
            msg.setMessageField1(String.valueOf(sessionId));
            sendMsg(dataOutputStream, msg);
        } else {
            int sessionId = dataBase.createSession(creatorUsername);
            Message message = new Message("6r");
            message.setMessageField1(String.valueOf(sessionId));
            sendMsg(dataOutputStream, message);
        }
    }

    /**
     * 加入会话
     * 将messageField1字段的用户加入至messageField2字段的会话中
     * 若该用户已存在在此会话中，则什么也不做
     */
    public void msg07(final DataBase dataBase, final DataOutputStream dataOutputStream) throws IOException {
        // 目标用户
        String username = getMessageField1();
        // 目标会话
        int sessionId = Integer.parseInt(getMessageField2());

        Message msg = new Message("7r");
        msg.setMessageField1("0");
        if (dataBase.joinSession(username, sessionId)) {
            msg.setMessageField1("1");
        }
        Message.sendMsg(dataOutputStream, msg);
    }

    /**
     * 获取请求列表
     *
     * @param username 请求者用户名
     */
    public void msg08(final DataBase dataBase, final DataOutputStream dataOutputStream, final String username) throws IOException {
        Vector<Request> requests = dataBase.getRequests(username);
        Message msg;
        msg = new Message("8r");
        msg.setMessageField1(String.valueOf(requests.size()));
        msg.setMessageField2(JSONArray.toJSONString(requests));
        sendMsg(dataOutputStream, msg);
    }

    /**
     * 发送信息
     * 一成不变地（除了messageNumber）将其转发给对应会话中除了发送者外所有的用户
     *
     * @param senderUsername 发送者用户名，即应LinkThread中储存的username
     */
    public void msg09(final DataBase dataBase, final String senderUsername) throws IOException {
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
            if (username.contentEquals(senderUsername)) {
                continue;
            }
            socket = dataBase.searchOnlineUserByUsername(username);
            // 为空表示未上线，直接跳过
            if (socket == null) {
                continue;
            }
            outputStream = socket.getOutputStream();
            dataOutputStream = new DataOutputStream(outputStream);
            sendMsg(dataOutputStream, this);
        }
    }

    /**
     * 好友或群聊请求：A==>服务端。A向B发出申请，先经过服务器
     * 无论B在线与否，在B的请求列表里添加上A的请求信息，同时，若B在线，则调用message11方法立即向B发送一个11r消息
     *
     * @param requesterUsername 请求者的用户名
     */
    public void msg10(final DataBase dataBase, final String requesterUsername) throws IOException {
        String receiverUsername = getMessageField1();
        String checkMessage = getMessageField2();
        int sessionId = Integer.parseInt(getMessageField3());
        Date date = new Date();
        // 无论是否在线，先添加至请求列表
        receiverUsername = dataBase.addRequest(requesterUsername, receiverUsername, checkMessage, sessionId, date);
        // 判断接收方是否在线
        Socket receiverSocket = dataBase.searchOnlineUserByUsername(receiverUsername);
        if (receiverSocket != null) {
            // 接收方在线则直接发送11号消息
            Request request = new Request(sessionId, requesterUsername, checkMessage, date);
            msg11(receiverSocket, request);
        }
    }

    /**
     * 好友请求：服务端==>B。服务端将申请转发给B
     * 注意，该方法只有在B在线时才会调用
     */
    private void msg11(final Socket receiverSocket, final Request request) throws IOException {
        Message message = new Message("11r");
        message.setMessageField1(JSON.toJSONString(request));
        DataOutputStream dataOutputStream = new DataOutputStream(receiverSocket.getOutputStream());
        sendMsg(dataOutputStream, message);
    }

    /**
     * 好友或群聊请求：B==>服务端。B确认后将确认信息发送给服务端
     * 无论同意还是通过，都将从B的请求列表里删除A的请求
     * 无论A在线与否，都将在A的结果列表里记录结果，同时，如果A在线，则调用message13方法立即向A发送一个13r消息
     *
     * @param receiverUsername 接收者用户名
     */
    public void msg12(final DataBase dataBase, final String receiverUsername) throws IOException {
        String requesterUsername = getMessageField1();
        int sessionId = Integer.parseInt(getMessageField2());
        String result = getMessageField3();

        dataBase.handleRequest(requesterUsername, receiverUsername, sessionId, result);

        Socket requestSocket = dataBase.searchOnlineUserByUsername(requesterUsername);
        if (requestSocket != null) {
            msg13(requestSocket, receiverUsername, result);
        }
    }

    /**
     * 好友请求：服务端==>A。服务端将结果发送给A
     * 注意，该方法只有在A在线时才会调用
     *
     * @param requestSocket    请求者的socket
     * @param receiverUsername 接收者用户名
     * @param result           处理结果
     */
    private void msg13(final Socket requestSocket, final String receiverUsername, final String result) throws IOException {
        Message msg = new Message("13r");
        msg.setMessageField1(receiverUsername);
        msg.setMessageField2(result);
        DataOutputStream dataOutputStream = new DataOutputStream(requestSocket.getOutputStream());
        sendMsg(dataOutputStream, msg);
    }

    /**
     * 获取好友申请结果列表
     *
     * @param username 用户名
     */
    public void msg14(DataBase dataBase, DataOutputStream dataOutputStream, String username) throws IOException {
        Vector<Result> results = dataBase.getResults(username);
        Message message;
        message = new Message("14r");
        message.setMessageField1(String.valueOf(results.size()));
        message.setMessageField2(JSONArray.toJSONString(results));
        sendMsg(dataOutputStream, message);
    }

    public void msg15(DataBase dataBase, DataOutputStream dataOutputStream, String activeUsername)
            throws IOException {
        String passiveUsername = getMessageField1();
        dataBase.delFriend(activeUsername, passiveUsername);
        Socket passiveUserSocket = dataBase.searchOnlineUserByUsername(passiveUsername);
        if (passiveUserSocket != null) {
            msg16(passiveUserSocket, activeUsername);
        }
    }

    private void msg16(Socket passiveUserSocket, String activeUsername) throws IOException {
        Message message = new Message("16r");
        message.setMessageField1(activeUsername);
        DataOutputStream dataOutputStream = new DataOutputStream(passiveUserSocket.getOutputStream());
        sendMsg(dataOutputStream, message);
    }

    public void msg17(DataBase dataBase, String username) {
        int sessionId = Integer.parseInt(getMessageField1());
        dataBase.quitSession(username, sessionId);
    }

    public void msg18(DataBase dataBase, String username) {
        String userJSONString = getMessageField1();
        User user = JSON.parseObject(userJSONString, User.class);
        dataBase.updateUserInfo(user);
    }
}

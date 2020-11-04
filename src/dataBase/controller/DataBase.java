package dataBase.controller;

import java.net.Socket;
import java.util.Date;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.bson.Document;
import com.alibaba.fastjson.JSON;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;

import dataBase.entity.*;

public class DataBase {
    /**
     * MongoDB地址。
     */
    private static final String MONGODB_CONNECTION_STRING = "mongodb://LightChat:213533@175.24.10.214:27017";

    /**
     * MongoDB连接。
     */
    private final static MongoClient mongoClient = new MongoClient(new MongoClientURI(MONGODB_CONNECTION_STRING));

    /**
     * LightChat数据库。
     */
    private final static MongoDatabase lightChat = MongoDBAPI.getOrCreateDatabase(mongoClient, "LightChat");

    private static final String USER_COLLECTION_NAME = "userCollection";
    private static final String SESSION_COLLECTION_NAME = "sessionCollection";

    /**
     * <用户名, socket>关联。
     */
    private final ConcurrentHashMap<String, Socket> usernameSocketMap = new ConcurrentHashMap<>();

    /**
     * 单例模式，同时包含了一些初始化工作。
     */
    private DataBase() {
        Document document = MongoDBAPI.getMaxOfCollection(lightChat, SESSION_COLLECTION_NAME, "sessionId");
        // 获取当前最大的sessionId，以防重启服务端后生成的sessionId和以前的重复
        try {
            Session.sessionNum = JSON.parseObject(document.toJson(), Session.class).getSessionId();
        }
        // 异常则表示当前数据库中无数据，直接从0开始
        catch (Exception e) {
            Session.sessionNum = 0;
        }
    }

    private final static DataBase dataBaseInstance = new DataBase();

    /**
     * 获得数据库单例对象的引用。
     *
     * @return 数据库单例对象的引用
     */
    public static DataBase getDataBaseInstance() {
        return dataBaseInstance;
    }



    /* 数据库相关 */

    /**
     * 检验用户名唯一性。
     *
     * @param username 待检验的用户名
     * @return true：该用户名唯一；false：该用户名已存在
     */
    public boolean checkUsernameUniqueness(final String username) {
        Document document = new Document("username", username);
        Document searchResult = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, document);
        // 如果搜索出结果，则表示用户名已存在
        return !isValidDocument(searchResult);
    }

    /**
     * 注册用户，将用户注册至数据库中。
     * 注意，该方法并不对待注册的用户对象做任何格式检查。
     *
     * @param user 待注册的用户对象
     */
    public void registerUser(final User user) {
        Document userDocument = Document.parse(JSON.toJSONString(user));
        MongoDBAPI.insertOneDocument(lightChat, USER_COLLECTION_NAME, userDocument);
    }

    /**
     * 检验登录。登录时检验用户名和密码是否匹配。
     *
     * @param username 用户名
     * @param password 密码
     * @return true：匹配，登录成功；false：不匹配，登录失败
     */
    public boolean checkLogin(final String username, final String password) {
        Document searchDocument = new Document("username", username).append("password", password);
        Document searchResult = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchDocument);
        return isValidDocument(searchResult);
    }

    /**
     * 更新用户信息。
     *
     * @param user 替换的用户对象（username与原来的相同）
     */
    public void updateUserInfo(final User user) {
        Document oldUserDocument = new Document("username", user.getUsername());
        Document newUserDocument = Document.parse(JSON.toJSONString(user));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, oldUserDocument, newUserDocument);
    }

    /**
     * 获取用户对象。
     *
     * @param username 用户名
     * @return 用户对象
     */
    public User getUserByUsername(final String username) {
        Document searchUserDocument = new Document("username", username);
        Document userDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument);
        return JSON.parseObject(userDocument.toJson(), User.class);
    }

    /**
     * 获得用户的好友列表。
     *
     * @param username 用户名
     * @return 用户的好友列表（Vector<User>)
     */
    public Vector<User> getFriends(final String username) {
        Document document = new Document("username", username);
        Document searchResult = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, document);
        // 指定用户的对象
        User user = JSON.parseObject(searchResult.toJson(), User.class);

        Vector<String> friendUsernames = user.getFriendUsernames();
        Vector<User> friends = new Vector<>(friendUsernames.size());

        for (String friendUsername : friendUsernames) {
            document = new Document("username", friendUsername);
            searchResult = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, document);
            user = JSON.parseObject(searchResult.toJson(), User.class);
            user.justInformation();
            friends.add(user);
        }
        return friends;
    }

    /**
     * 创建会话。
     *
     * @param username 创建者的用户名
     * @return 新创建的会话的sessionId
     */
    public int createSession(final String username) {
        // 获取用户
        Document searchUserDocument = new Document("username", username);
        Document userDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument);
        User user = JSON.parseObject(userDocument.toJson(), User.class);
        // 新建session
        Session.sessionNum++;
        Session session = new Session();
        int sessionId = session.getSessionId();
        // session中添加用户
        session.addSessionMember(user.getUsername());
        // 用户中添加session
        user.addSession(sessionId);
        // 记录入库
        Document sessionDocument = Document.parse(JSON.toJSONString(session));
        MongoDBAPI.insertOneDocument(lightChat, SESSION_COLLECTION_NAME, sessionDocument);
        Document newUserDocument = Document.parse(JSON.toJSONString(user));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, userDocument, newUserDocument);
        return sessionId;
    }

    /**
     * 创建群聊。
     * 创建者将自动加入新会话，同时默认创建者为新群聊的manager（群主）。
     *
     * @param username    创建者的用户名
     * @param sessionName 群聊名称
     * @return 新创建的群聊的sessionId（群号）
     */
    public int createSession(final String username, final String sessionName) {
        // 获取用户
        Document searchUserDocument = new Document("username", username);
        Document userDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument);
        User user = JSON.parseObject(userDocument.toJson(), User.class);
        // 新建session
        Session.sessionNum++;
        Session session = new Session();
        // 设置群聊名称
        session.setSessionName(sessionName);
        // 默认创建者为群主
        session.setManagerUsername(username);
        int sessionId = session.getSessionId();
        // session中添加用户
        session.addSessionMember(user.getUsername());
        // 用户中添加session
        user.addSession(sessionId);
        // 写回
        Document sessionDocument = Document.parse(JSON.toJSONString(session));
        MongoDBAPI.insertOneDocument(lightChat, SESSION_COLLECTION_NAME, sessionDocument);
        Document newUserDocument = Document.parse(JSON.toJSONString(user));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, userDocument, newUserDocument);
        return sessionId;
    }

    /**
     * 获得群名。
     *
     * @param sessionId 群聊的sessionId（群号）
     * @return 群名
     */
    public String getSessionName(final int sessionId) {
        Document searchSessionDocument = new Document("sessionId", sessionId);
        Document sessionDocument = MongoDBAPI.findOneDocument(lightChat, SESSION_COLLECTION_NAME, searchSessionDocument);
        Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);
        return session.getSessionName();
    }

    /**
     * 获得群主用户名。
     *
     * @param sessionId 群聊的sessionId（群号）
     * @return 群主用户名
     */
    public String getSessionManager(final int sessionId) {
        Document searchSessionDocument = new Document("sessionId", sessionId);
        Document sessionDocument = MongoDBAPI.findOneDocument(lightChat, SESSION_COLLECTION_NAME, searchSessionDocument);
        Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);
        return session.getManagerUsername();
    }

    /**
     * 将指定用户加入指定对话。
     * 会判断指定用户是否已经在指定会话中。
     * 如果是，则什么也不做。
     * 如果不是，那么将在指定用户的会话列表中添加指定会话，并在指定会话的成员列表中添加指定用户。
     *
     * @param username  用户名
     * @param sessionId 会话的sessionId
     * @return true：该用户不在该会话中；false：该用户已经在该会话中
     */
    public boolean joinSession(final String username, final int sessionId) {
        Document searchSessionDocument = new Document("sessionId", sessionId);
        Document sessionDocument = MongoDBAPI.findOneDocument(lightChat, SESSION_COLLECTION_NAME, searchSessionDocument);
        Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);

        // 先判断用户是否已经在会话中
        Vector<String> members = session.getSessionMembers();
        // 由于Vector.contains()底层是调用Object.equals()来比较对象的。而String类重写了equals()方法，所以可以直接这么判断
        if (members.contains(username)) {
            // 用户已经在会话中
            return false;
        }
        // 用户不在会话中
        Document searchUserDocument = new Document("username", username);
        Document userDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument);
        User user = JSON.parseObject(userDocument.toJson(), User.class);
        // 更新会话中的成员列表
        session.addSessionMember(username);
        Document newSessionDocument = Document.parse(JSON.toJSONString(session));
        MongoDBAPI.updateOneDocument(lightChat, SESSION_COLLECTION_NAME, sessionDocument, newSessionDocument);
        // 更新用户的会话列表
        user.addSession(sessionId);
        Document newUserDocument = Document.parse(JSON.toJSONString(user));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, userDocument, newUserDocument);
        return true;
    }

    /**
     * 退出会话。
     *
     * @param username  用户名
     * @param sessionId 会话或群聊的sessionId
     */
    public void quitSession(final String username, final int sessionId) {
        Document searchSessionDocument = new Document("sessionId", sessionId);
        Document sessionDocument = MongoDBAPI.findOneDocument(lightChat, SESSION_COLLECTION_NAME, searchSessionDocument);
        Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);

        Document searchUserDocument = new Document("username", username);
        Document userDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument);
        User user = JSON.parseObject(userDocument.toJson(), User.class);

        // 更新会话集合中用户信息
        Vector<String> sessionMembers = session.getSessionMembers();
        for (String str : sessionMembers) {
            if (str.contentEquals(username)) {
                sessionMembers.remove(str);
                break;
            }
        }
        // 更新用户下面的会话列表
        Vector<Integer> sessionIds = user.getSessionIds();
        for (Integer i : sessionIds) {
            if (i == sessionId) {
                sessionIds.remove(i);
                break;
            }
        }
        Document newSessionDocument = Document.parse(JSON.toJSONString(session));
        MongoDBAPI.updateOneDocument(lightChat, SESSION_COLLECTION_NAME, searchSessionDocument, newSessionDocument);

        Document newUserDocument = Document.parse(JSON.toJSONString(user));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument, newUserDocument);
    }

    /**
     * 获得用户的会话列表。
     *
     * @param username 用户的用户名
     * @return 该用户所在的所有会话的sessionId（Vector<Integer>）
     */
    public Vector<Integer> getSessions(final String username) {
        Document searchUserDocument = new Document("username", username);
        Document userDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument);
        User user = JSON.parseObject(userDocument.toJson(), User.class);
        return user.getSessionIds();
    }

    /**
     * 获得会话的成员列表。
     *
     * @param sessionId 会话的sessionId
     * @return 该会话中所有成员的username（Vector<String>）
     */
    public Vector<String> getMembers(final int sessionId) {
        Document searchSessionDocument = new Document("sessionId", sessionId);
        Document sessionDocument = MongoDBAPI.findOneDocument(lightChat, SESSION_COLLECTION_NAME, searchSessionDocument);
        Session session = JSON.parseObject(sessionDocument.toJson(), Session.class);
        return session.getSessionMembers();
    }

    /**
     * 发送方发出好友或群聊申请。
     * 无论接收方是否在线，该申请均会被添加至接收方的申请列表里。
     * <p>
     * 如果是普通的好友请求，那么receiverUsername参数应该传入好友用户名，sessionId参数应该传入int值0。
     * <p>
     * 如果是群聊请求，那么receiverUsername参数应该传入null，sessionId参数应该传入群聊会话的sessionId。
     * 该请求会被发送至群主，且群主接收到时，receiverUsername会是自己的用户名。
     *
     * @param senderUsername   发送方的用户名
     * @param receiverUsername 好友请求：接收方的用户名；群聊请求：null
     * @param checkMessage     验证信息
     * @param sessionId        好友请求："0"；群聊请求：sessionId
     * @return 好友请求：接收方的用户名；群聊请求：群主用户名
     */
    public String addRequest(final String senderUsername, String receiverUsername, final String checkMessage, final int sessionId, final Date date) {
        // 判断
        if (receiverUsername == null) {
            receiverUsername = getSessionManager(sessionId);
        }
        // 获取接收方用户对象
        Document searchReceiverDocument = new Document("username", receiverUsername);
        Document receiverDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchReceiverDocument);
        User receiver = JSON.parseObject(receiverDocument.toJson(), User.class);
        Request request = new Request(sessionId, senderUsername, checkMessage, date);
        // 添加请求
        receiver.addRequest(request);
        // 写回
        Document newReceiverDocument = Document.parse(JSON.toJSONString(receiver));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, receiverDocument, newReceiverDocument);
        return receiverUsername;
    }

    /**
     * 接收方处理好友申请。
     * 无论发送方是否在线，该申请均会被从接收方的申请列表里清除。 并且会将结果添加至发送方的结果列表里。
     * <p>
     * sessionId参数用来区别是好友请求还是加群请求，比如一个群主同时接收到了同一个人的好友请求和加群请求。
     *
     * @param senderUsername   发送方的用户名
     * @param receiverUsername 接收方的用户名
     * @param sessionId        会话的sessionId，用来区别是好友请求还是加群请求，好友请求就是0
     * @param result           处理结果，"0"为不同意，"1"为同意
     */
    public void handleRequest(final String senderUsername, final String receiverUsername, final int sessionId, final String result) {
        // 获取接收方用户对象
        Document searchReceiverDocument = new Document("username", receiverUsername);
        Document receiverDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchReceiverDocument);
        User receiver = JSON.parseObject(receiverDocument.toJson(), User.class);
        // 获取申请方用户对象
        Document searchSenderDocument = new Document("username", senderUsername);
        Document senderDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchSenderDocument);
        User sender = JSON.parseObject(senderDocument.toJson(), User.class);

        // 将申请方从接收方的请求列表里删除
        boolean flag = false;
        Vector<Request> requests = receiver.getRequests();
        for (Request request : requests) {
            if (request.getSenderUsername().contentEquals(senderUsername) && request.getSessionId() == sessionId) {
                requests.remove(request);
                flag = true;
                break;
            }
        }

        // 并未在申请列表里则直接结束，什么也不做
        if (!flag) {
            return;
        }

        // 好友请求
        if (sessionId == 0) {
            // 若同意，则双方互相成为好友
            if (result.contentEquals("1")) {
                receiver.getFriendUsernames().add(senderUsername);
                sender.getFriendUsernames().add(receiverUsername);
            }
        }
        // 群聊请求
        else {
            if (result.contentEquals("1")) {
                joinSession(senderUsername, sessionId);
            }
        }
        // 无论如何，receiver中的requests改变了，所以receiver需要写回数据库
        Document newReceiverDocument = Document.parse(JSON.toJSONString(receiver));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, receiverDocument, newReceiverDocument);
        // sender的results也改变了，需要写回数据库
        senderDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchSenderDocument);
        sender.getResults().add(new Result(sessionId, receiverUsername, result, new Date()));
        Document newSenderDocument = Document.parse(JSON.toJSONString(sender));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, senderDocument, newSenderDocument);
    }

    /**
     * 删除好友。
     *
     * @param activeUsername  主动方的用户名
     * @param passiveUsername 被动方的用户名
     */
    public void delFriend(final String activeUsername, final String passiveUsername) {
        // 获取两个用户对象
        Document searchActiveUserDocument = new Document("username", activeUsername);
        Document activeUserDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchActiveUserDocument);
        User activeUser = JSON.parseObject(activeUserDocument.toJson(), User.class);

        Document searchPassiveUserDocument = new Document("username", passiveUsername);
        Document passiveUserDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchPassiveUserDocument);
        User passiveUser = JSON.parseObject(passiveUserDocument.toJson(), User.class);

        // 删除好友关系后写回
        activeUser.getFriendUsernames().remove(passiveUsername);
        Document newActiveUserDocument = Document.parse(JSON.toJSONString(activeUser));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, activeUserDocument, newActiveUserDocument);

        passiveUser.getFriendUsernames().remove(activeUsername);
        Document newPassiveUserDocument = Document.parse(JSON.toJSONString(passiveUser));
        MongoDBAPI.updateOneDocument(lightChat, USER_COLLECTION_NAME, passiveUserDocument, newPassiveUserDocument);
    }

    /**
     * 获得用户的申请列表。
     *
     * @param username 用户名
     * @return 用户的申请列表（Vector<Request>）
     */
    public Vector<Request> getRequests(final String username) {
        Document searchUserDocument = new Document("username", username);
        Document userDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument);
        User user = JSON.parseObject(userDocument.toJson(), User.class);
        return user.getRequests();
    }

    /**
     * 获得用户的结果列表。
     *
     * @param username 用户名
     * @return 用户的结果列表（Vector<Result>）
     */
    public Vector<Result> getResults(final String username) {
        Document searchUserDocument = new Document("username", username);
        Document userDocument = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, searchUserDocument);
        User user = JSON.parseObject(userDocument.toJson(), User.class);
        return user.getResults();
    }

    /**
     * 根据nickname模糊查询用户。
     *
     * @param nickname 用户昵称
     * @return 查询结果（Vector<User>）
     */
    public Vector<User> fuzzySearchByNickname(final String nickname) {
        Pattern pattern = Pattern.compile(nickname);
        Document result = new Document("nickname", pattern);
        FindIterable<Document> documents = MongoDBAPI.findDocument(lightChat, USER_COLLECTION_NAME, result);
        Vector<User> users = new Vector<>();
        documents.forEach((Consumer<Document>) document -> users.add(JSON.parseObject(document.toJson(), User.class)));
        return users;
    }

    /**
     * 准确查询，根据username查询
     *
     * @param username 用户名
     * @return 查询结果
     */
    public User searchByUsername(final String username) {
        Document result = new Document("username", username);
        Document document = MongoDBAPI.findOneDocument(lightChat, USER_COLLECTION_NAME, result);
        if (document != null) {
            return JSON.parseObject(document.toJson(), User.class);
        }
        return null;
    }

    /**
     * 检验Document的合法性
     *
     * @param document 待检验的Document
     * @return true：合法；false：不合法
     */
    private boolean isValidDocument(Document document) {
        return (document != null) && (!document.isEmpty());
    }

    /* socket表相关 */

    /**
     * 在socketTable中添加<username, socket>关联。
     *
     * @param username 指定用户的用户名
     * @param socket   指定连接的socket引用
     */
    synchronized public void addSocket(final String username, final Socket socket) {
        if (username != null) {
            // 没有该用户才添加
            if (!usernameSocketMap.containsKey(username)) {
                usernameSocketMap.put(username, socket);
            }
        }
    }

    /**
     * 根据用户名查找socket，可用来检验用户是否在线。
     *
     * @param username 待查找的用户名
     * @return 对应连接的socket引用
     */
    synchronized public Socket searchOnlineUserByUsername(final String username) {
        if (username != null) {
            if (usernameSocketMap.containsKey(username)) {
                return usernameSocketMap.get(username);
            }
        }
        return null;
    }

    /**
     * 在socketTable中删除<username, socket>关联。
     *
     * @param username 待删除用户的用户名
     */
    synchronized public void delSocket(String username) {
        if (username != null && searchOnlineUserByUsername(username) != null) {
            usernameSocketMap.remove(username);
        }
    }

    public Set<String> showSocket() {
        return usernameSocketMap.keySet();
    }
}

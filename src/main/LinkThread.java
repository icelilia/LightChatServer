package main;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import dataBase.controller.DataBase;

// 一个LinkThread对应一个用户的连接
public class LinkThread extends Thread {
    public static ServerSocket server;
    public static DataBase dataBase;
    private Socket socket;
    private String username;

    public void run() {
        while (true) {
            try {
                // 没有用户连接之前，线程会一直阻塞在这一步
                socket = server.accept();

                // 各个流初始化
                InputStream inputStream = socket.getInputStream();
                DataInputStream dataInputStream = new DataInputStream(inputStream);
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dataOutputStream = new DataOutputStream(outputStream);

                Message msg;

                // 连接初始化
                msg = new Message();
                msg.msg01(dataOutputStream);

                // 循环接收登录请求或者注册请求
                while (true) {
                    msg = Message.receiveMessage(dataInputStream);
                    int messageNumber = Integer.parseInt(msg.getMessageNumber());
                    // 2号请求，登录请求
                    if (messageNumber == 2) {
                        // 需要记录一下用户名
                        username = msg.getMessageField1();
                        if (msg.msg02(dataBase, dataOutputStream)) {
                            // 添加socket关联
                            dataBase.addSocket(username, socket);
                            // 跳出循环
                            break;
                        } else {
                            username = null;
                        }
                    }
                    // 3号请求，注册请求
                    else if (messageNumber == 3) {
                        msg.msg03(dataBase, dataOutputStream);
                    }
                    // 其余请求直接跳过
                }

                // 接下来就是循环读取请求了
                while (true) {
                    msg = Message.receiveMessage(dataInputStream);
                    int messageNumber = Integer.parseInt(msg.getMessageNumber());

                    switch (messageNumber) {
                        // 注销
                        case 0 -> msg.msg00(dataBase, username);
                        // 获取好友列表
                        case 4 -> msg.msg04(dataBase, dataOutputStream, username);
                        // 获取历史消息列表
                        case 5 -> msg.msg05(dataBase, dataOutputStream, username);
                        // 创建会话
                        case 6 -> msg.msg06(dataBase, dataOutputStream, username);
                        // 将某用户加入会话
                        case 7 -> msg.msg07(dataBase, dataOutputStream);
                        // 获取申请列表
                        case 8 -> msg.msg08(dataBase, dataOutputStream, username);
                        // 发送信息
                        case 9 -> msg.msg09(dataBase, username);
                        // 好友申请
                        case 10 -> msg.msg10(dataBase, username);
                        // 申请结果
                        case 12 -> msg.msg12(dataBase, username);
                        // 获取结果列表
                        case 14 -> msg.msg14(dataBase, dataOutputStream, username);
                        // 删除好友
                        case 15 -> msg.msg15(dataBase, dataOutputStream, username);
                        // 退出群聊
                        case 17 -> msg.msg17(dataBase, username);
                        // 更新个人信息
                        case 18 -> msg.msg18(dataBase, username);
                    }
                    // 如果当前用户已注销，则跳出循环，释放socket连接
                    if (messageNumber == 0) {
                        socket.close();
                        socket = null;
                        username = null;
                        break;
                    }
                }
            }
            // 连接异常断开时，维护socketTable
            catch (SocketException socketException) {
                dataBase.delSocket(username);
                if (username != null) {
                    System.out.println("用户" + "[" + username + "]" + "异常登出");
                }
                socket = null;
                username = null;
            }
            // 其他异常的情况回头仔细考虑
            catch (Exception e) {
                System.err.println("异常：" + e);
            }
        }
    }

    public void forceOffline() {
        if (socket == null) {
            username = null;
            return;
        }
        try {
            dataBase.delSocket(username);
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket = null;
        username = null;
    }
}

package main;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

import dataBase.controller.DataBase;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;

// 一个LinkThread对应一个用户的连接
public class LinkThread extends Thread {
	public static ServerSocket server;
	public static DataBase dataBase;
	private Socket socket;
	private String username;

	public LinkThread() {
	}

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

				Message message;

				// 连接初始化
				message = new Message();
				message.message1(dataOutputStream);

				// 循环接收登录请求或者注册请求
				while (true) {
					message = Message.receiveMessage(dataInputStream);
					int messageNumber = Integer.parseInt(message.getMessageNumber());
					// 2号请求
					if (messageNumber == 2) {
						// 需要记录一下用户名
						username = message.getMessageField1();
						if (message.message2(dataBase, dataOutputStream)) {
							// 添加socket关联
							dataBase.addSocket(username, socket);
							// 跳出循环
							break;
						} else {
							username = null;
						}
					}
					// 3号请求
					else if (messageNumber == 3) {
						String registeredUsername = message.message3(dataBase, dataOutputStream);
						if (registeredUsername != null) {
							System.out.println("用户" + "[" + registeredUsername + "]" + "已注册");
						}
					}
					// 其余请求直接跳过
				}

				// 接下来就是循环读取请求了
				while (true) {
					message = Message.receiveMessage(dataInputStream);
					int messageNumber = Integer.parseInt(message.getMessageNumber());
					switch (messageNumber) {
					// 注销
					case 0:
						message.message0(dataBase, username);
						socket = null;
						username = null;
						break;
					// 获取好友列表
					case 4:
						message.message4(dataBase, dataOutputStream, username);
						break;
					// 获取历史消息列表
					case 5:
						message.message5(dataBase, dataOutputStream, username);
						break;
					// 创建会话
					case 6:
						message.message6(dataBase, dataOutputStream, username);
						break;
					// 将某用户加入会话
					case 7:
						message.message7(dataBase, dataOutputStream);
						break;
					// 获取申请列表
					case 8:
						message.message8(dataBase, dataOutputStream, username);
						break;
					// 发送信息
					case 9:
						message.message9(dataBase, username);
						break;
					// 好友申请
					case 10:
						message.message10(dataBase, username);
						break;
					// 申请结果
					case 12:
						message.message12(dataBase, username);
						break;
					// 获取结果列表
					case 14:
						message.message14(dataBase, dataOutputStream, username);
						break;
					// 删除好友
					case 15:
						message.message15(dataBase, dataOutputStream, username);
						break;
					// 退出群聊
					case 17:
						message.message17(dataBase, username);
						break;
					// 更新个人信息
					case 18:
						message.message18(dataBase, username);
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
}

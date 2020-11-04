package main;

import java.net.ServerSocket;
import java.util.Scanner;

import dataBase.controller.DataBase;

public class Main {

    public static void main(String[] args) {
        try {
            // 最高并发设为20
            int MAX_CONNECTION = 20;
            LinkThread.server = new ServerSocket(21915, MAX_CONNECTION);
            DataBase dataBase = DataBase.getDataBaseInstance();
            LinkThread.dataBase = dataBase;
            // 线程数要和最高并发数相同
            LinkThread[] linkThreads = new LinkThread[MAX_CONNECTION];
            for (int i = 0; i < MAX_CONNECTION; i++) {
                linkThreads[i] = new LinkThread();
                linkThreads[i].start();
            }

            // 回头可以加点服务器端的命令，像"stop"之类的
            Scanner scanner = new Scanner(System.in);
            String order;
            while (true) {
                order = scanner.nextLine();
                if (order.contentEquals("show users")) {
                    System.out.println(dataBase.showSocket());
                    continue;
                }
                if (order.contentEquals("kick")) {
                    System.out.print("which user? : ");
                    order = scanner.nextLine();
                    if (order.contentEquals("all")) {
                        for (LinkThread linkThread : linkThreads) {
                            linkThread.forceOffline();
                        }
                    }
                }
                if (order.contentEquals("stop")) {
                    scanner.close();
                    System.exit(0);
                }
            }
        } catch (Exception e) {
            System.err.println("出现异常：" + e);
            System.exit(-1);
        }
    }
}

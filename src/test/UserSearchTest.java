package test;

import dataBase.controller.DataBase;
import dataBase.entity.User;

import java.util.Arrays;
import java.util.Vector;

public class UserSearchTest {
	public static void main(String[] args) {
		DataBase dataBase = DataBase.getDataBaseInstance();
		dataBase.registerUser(new User("user1", "pass1", "nick1"));
		dataBase.registerUser(new User("user2", "pass2", "nick2"));
		dataBase.registerUser(new User("user3", "pass3", "nick3"));
		dataBase.registerUser(new User("user4", "pass4", "nick4"));
		dataBase.registerUser(new User("user5", "pass5", "nick5"));
		dataBase.registerUser(new User("user6", "pass6", "nick6"));
		dataBase.registerUser(new User("user7", "pass7", "nick7"));
		dataBase.registerUser(new User("us1", "pa1", "ni1"));
		dataBase.registerUser(new User("us2", "pa2", "ni2"));

		Vector<User> ni = dataBase.fuzzySearchByNickname("ni");
		System.out.println("ni :" + Arrays.toString(ni.toArray()));
		Vector<User> nick = dataBase.fuzzySearchByNickname("nick");
		System.out.println("nick :" + Arrays.toString(nick.toArray()));
		Vector<User> ck = dataBase.fuzzySearchByNickname("ck");
		System.out.println("ck :" + Arrays.toString(ck.toArray()));
		User us1 = dataBase.searchByUsername("us1");
		System.out.println("us1" + us1.toString());
	}
}

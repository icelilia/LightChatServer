package test;

import java.util.Vector;

public class Test {
    public static void main(String[] args) {
        Vector<String> v = new Vector<>();
        String str = new String("114514");
        v.add(str);
        byte[] array = str.getBytes();
        String s = new String(array);
        System.out.println(v.contains(s));
    }
}

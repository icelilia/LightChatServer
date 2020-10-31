package redis.entity;

public class ContentUtil {
    public static ContentUtil INSTANCE = new ContentUtil();

    //格式
    //发送方 € 接收方 € 时间 € 内容 € 类型

    public Content parse(String s){
        String[] seg = s.split("€");
        Content content = new Content(seg[0], seg[1], seg[2], seg[3], seg[4]);
        return content;
    }

    public String compress(Content content){
        String s = "";
        s += content.getFrom() + "€";
        s += content.getTo() + "€";
        s += content.getTime() + "€";
        s += content.getContent() + "€";
        s += content.getKind();
        return s;
    }
}

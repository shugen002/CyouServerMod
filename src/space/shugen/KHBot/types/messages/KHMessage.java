package space.shugen.KHBot.types.messages;


import space.shugen.KHBot.types.Author;

public class KHMessage {
    public String channel_type;
    public int type;
    public String target_id;
    public String author_id;
    public String content;
    public String msg_id;
    public Extra extra;

    public static class Extra {
        public Author author;
    }
}

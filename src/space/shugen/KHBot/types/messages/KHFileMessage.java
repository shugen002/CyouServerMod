package space.shugen.KHBot.types.messages;


import space.shugen.KHBot.types.Author;

public class KHFileMessage extends KHMessage {
    public Extra extra;

    public static class Extra extends KHMessage.Extra {
        public Author author;
        public Attachment attachments;
    }

    public static class Attachment {
        public String type;
        public String url;
        public String name;
        public String file_type;
        public int size;
    }
}

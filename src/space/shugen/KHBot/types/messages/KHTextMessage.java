package space.shugen.KHBot.types.messages;

import space.shugen.KHBot.types.Author;

public class KHTextMessage extends KHMessage {
    public Extra extra;

    public static class Extra {
        public Author author;
    }
}
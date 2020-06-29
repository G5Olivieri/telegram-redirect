package org.glayson.telegram;

public class ChatsHandler implements Handler {
    @Override
    public void handle(EventLoop loop, TdApi.Object object) {
        TdApi.Chats chats = (TdApi.Chats)object;
        for(long id : chats.chatIds) {
            System.out.println(id);
        }
    }
}

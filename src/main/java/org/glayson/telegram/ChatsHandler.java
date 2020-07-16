package org.glayson.telegram;

public class ChatsHandler implements AbstractHandler {
    private final EventLoop loop;

    public ChatsHandler(EventLoop loop) {
        this.loop = loop;
    }
    public void getChats() {
        loop.send(new TdApi.GetChats(new TdApi.ChatListMain(), Long.MAX_VALUE, 0, 10), this);
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        TdApi.Chats chats = (TdApi.Chats)object;
        ChatHandler chatHandler = new ChatHandler(loop);
        for (long chatId : chats.chatIds) {
            chatHandler.getChat(chatId);
        }
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.out.printf("Error in class %s: %s\n", getClass().getName(), error);
    }
}

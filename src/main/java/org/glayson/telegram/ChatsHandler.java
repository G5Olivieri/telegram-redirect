package org.glayson.telegram;

public class ChatsHandler implements AbstractHandler {
    private final EventLoop loop;
    private TdApi.Chats chats = null;
    private volatile boolean isLocked;

    public ChatsHandler(EventLoop loop) {
        this.loop = loop;
    }
    public TdApi.Chats getChats() {
        loop.send(new TdApi.GetChats(new TdApi.ChatListMain(), Long.MAX_VALUE, 0, 10), this);
        isLocked = true;
        while(isLocked) {
            Thread.onSpinWait();
        }
        return chats;
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        chats = (TdApi.Chats)object;
        isLocked = false;
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.out.printf("Error in class %s: %s\n", getClass().getName(), error);
        isLocked = false;
    }
}

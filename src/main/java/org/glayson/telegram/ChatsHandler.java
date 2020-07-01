package org.glayson.telegram;

import java.util.concurrent.Future;

public class ChatsHandler implements AbstractHandler {
    private final EventLoop loop;
    private final Object lock = new Object();
    private TdApi.Chats chats = null;

    public ChatsHandler(EventLoop loop) {
        this.loop = loop;
    }
    public Future<TdApi.Chats> getChats() {
        return loop.execute(() -> {
            if (chats != null) {
                return chats;
            }
            loop.send(new TdApi.GetChats(new TdApi.ChatListMain(), Long.MAX_VALUE, 0, 10), this);
            synchronized (lock) {
                lock.wait();
            }
            return chats;
        });
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        synchronized (lock) {
            chats = (TdApi.Chats)object;
            lock.notify();
        }
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        synchronized (lock) {
            System.out.printf("Error in class %s: %s\n", getClass().getName(), error);
            lock.notify();
        }
    }
}

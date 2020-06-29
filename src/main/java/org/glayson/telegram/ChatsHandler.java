package org.glayson.telegram;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ChatsHandler implements Handler {
    private final EventLoop loop;
    private final Object lock = new Object();
    private TdApi.Chats chats = null;

    public ChatsHandler(EventLoop loop) {
        this.loop = loop;
    }
    public Future<TdApi.Chats> getChats() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return executorService.submit(() -> {
            try {
                if (chats != null) {
                    return chats;
                }
                loop.send(new TdApi.GetChats(new TdApi.ChatListMain(), Long.MAX_VALUE, 0, 10));
                synchronized (lock) {
                    lock.wait();
                }
            } finally {
                executorService.shutdown();
            }
            return chats;
        });
    }

    @Override
    public void handle(TdApi.Object object) {
        synchronized (lock) {
            chats = (TdApi.Chats)object;
            lock.notify();
        }
    }
}

package org.glayson.telegram;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class ChatHandler implements Handler {
    private final EventLoop loop;
    private final Object lock = new Object();
    private final ConcurrentHashMap<Long, TdApi.Chat> cache = new ConcurrentHashMap<>();

    public ChatHandler(EventLoop loop) {
        this.loop = loop;
    }

    public Future<TdApi.Chat> getChat(long chatId) {
        return loop.execute(() -> {
            if (cache.contains(chatId)) {
                return cache.get(chatId);
            }

            loop.send(new TdApi.GetChat(chatId));
            synchronized (lock) {
                lock.wait();
            }

            return cache.get(chatId);
        });
    }

    @Override
    public void handle(TdApi.Object object) {
        synchronized (lock) {
            TdApi.Chat chat = (TdApi.Chat)object;
            cache.put(chat.id, chat);
            lock.notify();
        }
    }
}

package org.glayson.telegram;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatHandler implements AbstractHandler {
    private final EventLoop loop;
    private final ConcurrentHashMap<Long, Consumer<TdApi.Chat>> callbacks = new ConcurrentHashMap<>();

    public ChatHandler(EventLoop loop) {
        this.loop = loop;
    }

    public void getChat(long chatId, Consumer<TdApi.Chat> callback) {
        long requestId = loop.send(new TdApi.GetChat(chatId), this);
        callbacks.put(requestId, callback);
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        TdApi.Chat chat = (TdApi.Chat)object;
        Consumer<TdApi.Chat> callback = callbacks.get(eventId);
        if (callback != null) {
            callbacks.remove(eventId);
            callback.accept(chat);
        }
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.out.printf("Error in class %s: %s\n", getClass().getName(), error);
        callbacks.remove(eventId);
    }
}

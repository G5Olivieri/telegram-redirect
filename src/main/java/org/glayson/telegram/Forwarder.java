package org.glayson.telegram;

import java.util.concurrent.ConcurrentHashMap;

public final class Forwarder implements AbstractHandler {
    private final EventLoop loop;
    private final long chatId;
    private ConcurrentHashMap<Long, Long> messageMap = new ConcurrentHashMap<>();
    private long lastMessageId;

    public Forwarder(EventLoop loop, long chatId) {
        this.loop = loop;
        this.chatId = chatId;
    }

    public void forward(TdApi.Message message) {
        this.lastMessageId = message.id;
        TdApi.FormattedText messageText = ((TdApi.MessageText)message.content).text;
        System.out.println("Enviando: " + message + " Para: " + chatId);
        loop.send(new TdApi.SendMessage(
                chatId,
                0,
                null,
                null,
                new TdApi.InputMessageText(messageText, false, true)
        ), this);
    }

    @Override
    public void onError(TdApi.Error error) {
        System.out.println();
    }

    @Override
    public void onSuccess(TdApi.Object object) {
        System.out.println("CHEGOU: " + object);
        if(object.getConstructor() != TdApi.UpdateNewMessage.CONSTRUCTOR) {
           return;
        }
        TdApi.Message msg = ((TdApi.UpdateNewMessage)object).message;
        messageMap.put(lastMessageId, msg.id);
    }
}

package org.glayson.telegram;

import java.util.concurrent.ConcurrentHashMap;

public final class ForwarderMessagesHandler implements Handler {
    private final ConcurrentHashMap<Long, Forwarder> forwards = new ConcurrentHashMap();
    private final EventLoop loop;

    public ForwarderMessagesHandler(EventLoop loop) {
        this.loop = loop;
    }
    @Override
    public void handle(TdApi.Object object) {
        TdApi.UpdateNewMessage newMessage = (TdApi.UpdateNewMessage)object;
        Forwarder forwarder = forwards.get(newMessage.message.chatId);
        if(forwarder != null) {
            forwarder.forward(newMessage.message);
        }
    }

    public void setChatId(long inputChatId, long outputChatId) {
        forwards.put(inputChatId, new Forwarder(loop, outputChatId));
    }
}

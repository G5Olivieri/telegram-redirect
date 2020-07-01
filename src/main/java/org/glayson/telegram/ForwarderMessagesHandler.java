package org.glayson.telegram;

import java.util.concurrent.ConcurrentHashMap;

public final class ForwarderMessagesHandler implements Handler {
    private final ConcurrentHashMap<Long, Forwarder> forwards = new ConcurrentHashMap<>();
    private final EventLoop loop;
    private ConcurrentHashMap<Long, Long> messageIdCallback = new ConcurrentHashMap<>();

    public ForwarderMessagesHandler(EventLoop loop) {
        this.loop = loop;
    }
    @Override
    public void handle(long eventId, TdApi.Object object) {
        switch (object.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR: {
                TdApi.UpdateNewMessage newMessage = (TdApi.UpdateNewMessage)object;
                Forwarder forwarder = forwards.get(newMessage.message.chatId);
                if(forwarder != null) {
                    forwarder.forward(newMessage.message);
                }
                break;
            }
            case TdApi.UpdateMessageContent.CONSTRUCTOR: {
                TdApi.UpdateMessageContent editedMessage = (TdApi.UpdateMessageContent)object;
                Forwarder forwarder = forwards.get(editedMessage.chatId);
                if(forwarder != null) {
                    forwarder.edited(editedMessage);
                }
                break;
            }
            case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                TdApi.UpdateChatLastMessage msg = (TdApi.UpdateChatLastMessage)object;
                Long output = messageIdCallback.get(msg.chatId);
                if(output == null) {
                    return;
                }
                Forwarder forwarder = forwards.get(output);
                if (forwarder != null) {
                    forwarder.setMessageId(msg);
                }
            }
        }
    }

    public void setChatId(long inputChatId, long outputChatId) {
        messageIdCallback.put(outputChatId, inputChatId);
        forwards.put(inputChatId, new Forwarder(loop, outputChatId));
    }
}

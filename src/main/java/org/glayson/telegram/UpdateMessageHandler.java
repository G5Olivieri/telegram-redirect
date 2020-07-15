package org.glayson.telegram;

import java.util.concurrent.ConcurrentHashMap;

public final class UpdateMessageHandler implements Handler {
    private final ConcurrentHashMap<Long, Handler> chatHandlers = new ConcurrentHashMap<>();

    @Override
    public void handle(long eventId, TdApi.Object object) {
        Long chatId = 0L;
        switch (object.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR: {
                chatId = ((TdApi.UpdateNewMessage)object).message.chatId;
                break;
            }
            case TdApi.UpdateMessageContent.CONSTRUCTOR: {
               chatId = ((TdApi.UpdateMessageContent)object).chatId;
               break;
            }
            case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                chatId = ((TdApi.UpdateChatLastMessage)object).chatId;
                break;
            }
        }
        Handler chatHandler = chatHandlers.get(chatId);
        if (chatHandler != null) {
            chatHandler.handle(eventId, object);
        }
    }

    public void putChatHandler(Long chatId, Handler chatHandler) {
        chatHandlers.put(chatId, chatHandler);
    }
}

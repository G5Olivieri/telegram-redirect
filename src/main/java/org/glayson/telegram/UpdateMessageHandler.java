package org.glayson.telegram;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public final class UpdateMessageHandler implements Handler {
    private final ConcurrentHashMap<Long, List<Handler>> chatHandlers = new ConcurrentHashMap<>();

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
        List<Handler> handlers = chatHandlers.getOrDefault(chatId, Collections.emptyList());
        for (Handler chatHandler : handlers) {
            chatHandler.handle(eventId, object);
        }
    }

    public void putChatHandler(Long chatId, Handler chatHandler) {
        List<Handler> handlers = chatHandlers.getOrDefault(chatId, new ArrayList<>());
        handlers.add(chatHandler);
        chatHandlers.put(chatId, handlers);
    }
}

package org.glayson.telegram;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class UpdateMessageHandler implements Handler {
    private final Map<Long, Handler> chatHandlers = new HashMap<>();

    @Override
    public void handle(long eventId, TdApi.Object object) {
        Long chatId = switch (object.getConstructor()) {
            case TdApi.UpdateNewMessage.CONSTRUCTOR -> ((TdApi.UpdateNewMessage)object).message.chatId;
            case TdApi.UpdateMessageContent.CONSTRUCTOR -> ((TdApi.UpdateMessageContent)object).chatId;
            case TdApi.UpdateChatLastMessage.CONSTRUCTOR -> ((TdApi.UpdateChatLastMessage)object).chatId;
            default -> 0L;
        };

        Optional.ofNullable(chatHandlers.get(chatId))
                .ifPresent(handler -> handler.handle(eventId, object));
    }

    public void putChatHandler(long chatId, Handler handler) {
       this.chatHandlers.put(chatId, handler);
    }

    public void removeChatHandler(Long chatId) {
       this.chatHandlers.remove(chatId);
    }
}

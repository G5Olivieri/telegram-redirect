package org.glayson.telegram;

import java.util.ArrayList;
import java.util.List;

public final class UpdateMessageHandler implements Handler {
    private final List<Forwarder> handlers = new ArrayList<>();

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
        for (Forwarder chatHandler : handlers) {
            if (chatHandler.inputChatId() == chatId || chatHandler.outputChatId() == chatId) {
                chatHandler.handle(eventId, object);
            }
        }
    }

    public void printRedirects() {
        for (Forwarder forwarder : handlers) {
            System.out.println("Redirecionando de " + forwarder.inputChatId() + " para " + forwarder.outputChatId());
        }
    }

    public void removeHandler(Long inputChatId, Long outputChatId) {
        handlers.removeIf(forwarder -> forwarder.outputChatId() == outputChatId && forwarder.inputChatId() == inputChatId);
    }

    public void addHandler(Forwarder forwarder) {
        this.handlers.add(forwarder);
    }
}

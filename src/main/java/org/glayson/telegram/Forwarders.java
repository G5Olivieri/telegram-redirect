package org.glayson.telegram;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class Forwarders implements Handler {
    private final ConcurrentHashMap<Map.Entry<Long, Long>, Forwarder> forwardersMap = new ConcurrentHashMap<>();
    private final UpdateMessageHandler updateMessageHandler;
    private final EventLoop loop;

    public Forwarders(EventLoop loop, UpdateMessageHandler updateMessageHandler) {
        this.loop = loop;
        this.updateMessageHandler = updateMessageHandler;
    }

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

        for (Forwarder chatHandler : forwardersMap.values()) {
            if (chatHandler.inputChatId() == chatId || chatHandler.outputChatId() == chatId) {
                chatHandler.handle(eventId, object);
            }
        }
    }

    public List<String> redirectsAsString() {
        return forwardersMap
                .values()
                .stream()
                .map(f -> "Redirecionando de " + f.inputChatId() + " para " + f.outputChatId())
                .collect(Collectors.toList());
    }

    public void removeHandler(Long inputChatId, Long outputChatId) {
        Map.Entry<Long, Long> entry = Map.entry(inputChatId, outputChatId);
        Forwarder forwarder = forwardersMap.get(entry);
        if (forwarder != null) {
            forwardersMap.remove(entry);
            this.updateMessageHandler.removeChatHandler(inputChatId);
            this.updateMessageHandler.removeChatHandler(outputChatId);
        }
    }

    public void addForwarder(Long in, Long out) {
        this.forwardersMap.put(Map.entry(in, out), new Forwarder(loop, in, out));
        this.updateMessageHandler.putChatHandler(in, this);
        this.updateMessageHandler.putChatHandler(out, this);
    }
}

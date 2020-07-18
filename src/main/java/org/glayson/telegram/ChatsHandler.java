package org.glayson.telegram;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class ChatsHandler implements AbstractHandler {
    private final EventLoop loop;
    private final ChatHandler chatHandler;
    private final ConcurrentHashMap<Long, Consumer<String>> callbacks = new ConcurrentHashMap<>();

    public ChatsHandler(EventLoop loop) {
        this.loop = loop;
        chatHandler = new ChatHandler(loop);
    }
    public void getChats(Consumer<String> callback) {
        long requestId = loop.send(new TdApi.GetChats(new TdApi.ChatListMain(), Long.MAX_VALUE, 0, 20), this);
        callbacks.put(requestId, callback);
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        Consumer<String> callback = callbacks.get(eventId);
        if (callback == null) {
            return;
        }

        TdApi.Chats chats = (TdApi.Chats)object;
        for (long chatId : chats.chatIds) {
            chatHandler.getChat(chatId, (chat) -> {
                final StringBuilder type = new StringBuilder();
                switch (chat.type.getConstructor()) {
                    case TdApi.ChatTypePrivate.CONSTRUCTOR: {
                        type.append("User");
                        break;
                    }
                    case TdApi.ChatTypeBasicGroup.CONSTRUCTOR: {
                        type.append("Group");
                        break;
                    }
                    case TdApi.ChatTypeSecret.CONSTRUCTOR: {
                        type.append("Secret");
                        break;
                    }
                    case TdApi.ChatTypeSupergroup.CONSTRUCTOR: {
                        type.append("SuperGroup");
                        break;
                    }
                }

                callback.accept(String.format("Chat (%s): %s (%s)\n", chat.id, chat.title, type.toString()));
            });
        }

        callbacks.remove(eventId);
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.out.printf("Error in class %s: %s\n", getClass().getName(), error);
    }

    public void getChatsByIds(Long inputChatId, Long outputChatId, BiConsumer<Long, Long> callback) {
       chatHandler.getChat(inputChatId, (inputChat) -> {
           chatHandler.getChat(outputChatId, (outputChat) -> {
               callback.accept(inputChatId, outputChatId);
           });
       });
    }
}

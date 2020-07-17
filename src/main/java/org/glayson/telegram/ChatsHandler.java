package org.glayson.telegram;

import java.util.function.BiConsumer;

public class ChatsHandler implements AbstractHandler {
    private final EventLoop loop;
    private final ChatHandler chatHandler;

    public ChatsHandler(EventLoop loop) {
        this.loop = loop;
        chatHandler = new ChatHandler(loop);
    }
    public void getChats() {
        loop.send(new TdApi.GetChats(new TdApi.ChatListMain(), Long.MAX_VALUE, 0, 20), this);
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        TdApi.Chats chats = (TdApi.Chats)object;
        for (long chatId : chats.chatIds) {
            chatHandler.getChat(chatId, (chat) -> {
                String type = "";
                switch (chat.type.getConstructor()) {
                    case TdApi.ChatTypePrivate.CONSTRUCTOR: {
                        type = "User";
                        break;
                    }
                    case TdApi.ChatTypeBasicGroup.CONSTRUCTOR: {
                        type = "Group";
                        break;
                    }
                }
                System.out.printf("Chat (%s): %s (%s)\n", chat.id, chat.title, type);
            });
        }
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

package org.glayson.telegram;

public class ChatHandler implements AbstractHandler {
    private final EventLoop loop;
    private TdApi.Chat chat;

    public ChatHandler(EventLoop loop) {
        this.loop = loop;
    }

    public void getChat(long chatId) {
        loop.send(new TdApi.GetChat(chatId), this);
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        chat = (TdApi.Chat)object;
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
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.out.printf("Error in class %s: %s\n", getClass().getName(), error);
    }
}

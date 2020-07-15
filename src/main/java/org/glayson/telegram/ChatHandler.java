package org.glayson.telegram;

public class ChatHandler implements AbstractHandler {
    private final EventLoop loop;
    private TdApi.Chat chat;
    private volatile boolean isLocked;

    public ChatHandler(EventLoop loop) {
        this.loop = loop;
    }

    public TdApi.Chat getChat(long chatId) {
        loop.send(new TdApi.GetChat(chatId), this);
        isLocked = true;
        while(isLocked) {
            Thread.onSpinWait();
        }
        return chat;
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        chat = (TdApi.Chat)object;
        isLocked = false;
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.out.printf("Error in class %s: %s\n", getClass().getName(), error);
        isLocked = false;
    }
}

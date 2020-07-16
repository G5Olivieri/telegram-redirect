package org.glayson.telegram;

public class ChatHandler implements AbstractHandler {
    private final EventLoop loop;
    private TdApi.Chat chat;
    private volatile boolean isLocked;

    public ChatHandler(EventLoop loop) {
        this.loop = loop;
    }

    public TdApi.Chat getChat(long chatId) {
        int chegou = 10;
        System.out.println("CHEGOU " + ++chegou);
        loop.send(new TdApi.GetChat(chatId), this);
        System.out.println("CHEGOU " + ++chegou);
        isLocked = true;
        System.out.println("CHEGOU " + ++chegou);
        while(isLocked) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("CHEGOU " + ++chegou);
        return chat;
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        int chegou = 6;
        System.out.println("CHEGOU " + ++chegou);
        chat = (TdApi.Chat)object;
        System.out.println("CHEGOU " + ++chegou);
        isLocked = false;
        System.out.println("CHEGOU " + ++chegou);
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.out.printf("Error in class %s: %s\n", getClass().getName(), error);
        isLocked = false;
    }
}

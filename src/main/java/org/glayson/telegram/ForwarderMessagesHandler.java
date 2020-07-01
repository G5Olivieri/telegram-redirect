package org.glayson.telegram;

public class ForwarderMessagesHandler implements Handler {
    private long chatId;

    @Override
    public void handle(TdApi.Object object) {
        TdApi.UpdateNewMessage newMessage = (TdApi.UpdateNewMessage)object;
        if (newMessage.message.chatId == chatId) {
            System.out.println(newMessage);
        }
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }
}

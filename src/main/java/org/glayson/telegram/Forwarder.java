package org.glayson.telegram;

import java.util.concurrent.ConcurrentHashMap;

public final class Forwarder implements AbstractHandler {
    private final EventLoop loop;
    private final long chatId;
    private ConcurrentHashMap<Long, Long> messageMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, Long> requests = new ConcurrentHashMap<>();
    private ConcurrentHashMap<SendingMessage, Long> stateSending = new ConcurrentHashMap<>();

    public Forwarder(EventLoop loop, long chatId) {
        this.loop = loop;
        this.chatId = chatId;
        messageMap.put(0L, 0L);
    }

    public void forward(TdApi.Message message) {
        TdApi.FormattedText messageText = ((TdApi.MessageText)message.content).text;
        long requestId = loop.send(new TdApi.SendMessage(
                chatId,
                messageMap.get(message.replyToMessageId),
                null,
                null,
                new TdApi.InputMessageText(messageText, false, true)
        ), this);
        requests.put(requestId, message.id);
    }

    public void edited(TdApi.UpdateMessageContent content) {
        switch (content.newContent.getConstructor()) {
            case TdApi.MessageText.CONSTRUCTOR: {
                TdApi.MessageText msg = (TdApi.MessageText)content.newContent;
                loop.send(
                        new TdApi.EditMessageText(
                                chatId,
                                messageMap.get(content.messageId),
                                null,
                                new TdApi.InputMessageText(msg.text, false, true)
                        ),
                        this
                );
            }
        }
    }

    public void setMessageId(TdApi.UpdateChatLastMessage msg) {
        SendingMessage sendingMessage = new SendingMessage(msg.chatId, msg.lastMessage.senderUserId, msg.lastMessage.date);
        Long messageId = stateSending.get(sendingMessage);
        if (messageId == null) {
            return;
        }
        stateSending.remove(sendingMessage);
        messageMap.put(messageId, msg.lastMessage.id);
    }

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.out.println(error);
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        if(object.getConstructor() != TdApi.Message.CONSTRUCTOR) {
           return;
        }
        if (requests.get(eventId) == null) {
            return;
        }
        TdApi.Message msg = (TdApi.Message)object;
        long messageId = requests.get(eventId);
        requests.remove(eventId);
        stateSending.put(new SendingMessage(msg.chatId, msg.senderUserId, msg.date), messageId);
    }

}

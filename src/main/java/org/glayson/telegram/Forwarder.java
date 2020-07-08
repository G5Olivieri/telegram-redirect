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

    @Override
    public void onError(long eventId, TdApi.Error error) {
        System.err.println(error);
    }

    @Override
    public void onSuccess(long eventId, TdApi.Object object) {
        switch (object.getConstructor()) {
            case TdApi.Message.CONSTRUCTOR: {
                if(requests.get(eventId) == null) {
                    return;
                }
                TdApi.Message message = (TdApi.Message)object;
                long messageId = requests.get(eventId);
                requests.remove(messageId);
                stateSending.put(new SendingMessage(message.chatId, message.senderUserId, message.date), messageId);
                break;
            }
            case TdApi.UpdateNewMessage.CONSTRUCTOR: {
                TdApi.UpdateNewMessage newMessage = (TdApi.UpdateNewMessage)object;
                if (newMessage.message.chatId != chatId) {
                    forward(newMessage.message);
                }
                break;
            }
            case TdApi.UpdateMessageContent.CONSTRUCTOR: {
                TdApi.UpdateMessageContent content = (TdApi.UpdateMessageContent)object;
                if (content.chatId != chatId) {
                    edited(content);
                }
                break;
            }
            case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                TdApi.UpdateChatLastMessage message = (TdApi.UpdateChatLastMessage)object;
                if (message.chatId == chatId) {
                    setMessageId(message);
                }
                break;
            }
        }
    }

    private void forward(TdApi.Message message) {
        switch (message.content.getConstructor()) {
            case TdApi.MessageText.CONSTRUCTOR: {
                TdApi.FormattedText messageText = ((TdApi.MessageText)message.content).text;
                sendMessage(message, new TdApi.InputMessageText(messageText, false, true));
                break;
            }
        }
    }

    private void sendMessage(TdApi.Message message, TdApi.InputMessageContent content) {
        long requestId = loop.send(
                new TdApi.SendMessage(
                        chatId,
                        messageMap.getOrDefault(message.replyToMessageId, 0L),
                        null,
                        null,
                        content
                ),
                this
        );
        requests.put(requestId, message.id);
    }

    private void edited(TdApi.UpdateMessageContent content) {
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

    private void setMessageId(TdApi.UpdateChatLastMessage msg) {
        SendingMessage sendingMessage = new SendingMessage(msg.chatId, msg.lastMessage.senderUserId, msg.lastMessage.date);
        Long messageId = stateSending.get(sendingMessage);
        if (messageId == null) {
            return;
        }
        stateSending.remove(sendingMessage);
        messageMap.put(messageId, msg.lastMessage.id);
    }
}

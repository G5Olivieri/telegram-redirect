package org.glayson.telegram;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public final class Forwarder implements AbstractHandler {
    private final EventLoop loop;
    private final long outputChatId;
    private final long inputChatId;
    private ConcurrentHashMap<Long, Long> messageMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<Long, Long> requests = new ConcurrentHashMap<>();
    private ConcurrentHashMap<SendingMessage, Long> stateSending = new ConcurrentHashMap<>();

    public Forwarder(EventLoop loop, long inputChatId, long outputChatId) {
        this.loop = loop;
        this.outputChatId = outputChatId;
        this.inputChatId = inputChatId;
        messageMap.put(0L, 0L);
    }

    public long outputChatId() {
        return this.outputChatId;
    }

    public long inputChatId() {
        return this.inputChatId;
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
                if (newMessage.message.chatId == inputChatId) {
                    forward(newMessage.message);
                }
                break;
            }
            case TdApi.UpdateMessageContent.CONSTRUCTOR: {
                TdApi.UpdateMessageContent content = (TdApi.UpdateMessageContent)object;
                if (content.chatId == inputChatId) {
                    edited(content);
                }
                break;
            }
            case TdApi.UpdateChatLastMessage.CONSTRUCTOR: {
                TdApi.UpdateChatLastMessage message = (TdApi.UpdateChatLastMessage)object;
                if (message.chatId == outputChatId) {
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
            case TdApi.MessagePhoto.CONSTRUCTOR: {
                TdApi.MessagePhoto photo = (TdApi.MessagePhoto)message.content;
                TdApi.PhotoSize photoSize = Arrays.stream(photo.photo.sizes)
                        .reduce((a, b) -> b)
                        .orElse(null);
                if (photoSize != null) {
                    sendMessage(
                            message,
                            new TdApi.InputMessagePhoto(
                                    newInputFileRemote(photoSize.photo.remote.id),
                                    null,
                                    null,
                                    photoSize.width,
                                    photoSize.height,
                                    photo.caption,
                                    0
                            )
                    );
                }
                break;
            }
            case TdApi.MessageAudio.CONSTRUCTOR: {
                TdApi.MessageAudio content = (TdApi.MessageAudio)message.content;
                TdApi.Audio audio = content.audio;
                TdApi.Thumbnail albumCoverThumbnail = audio.albumCoverThumbnail;
                sendMessage(
                        message,
                        new TdApi.InputMessageAudio(
                                newInputFileRemote(audio.audio.remote.id),
                                newThumbnail(albumCoverThumbnail),
                                audio.duration,
                                audio.title,
                                audio.performer,
                                content.caption
                        )
                );
                break;
            }
            case TdApi.MessageVideo.CONSTRUCTOR: {
                TdApi.MessageVideo content = (TdApi.MessageVideo)message.content;
                TdApi.Video video = content.video;
                TdApi.Thumbnail thumbnail = video.thumbnail;
                sendMessage(
                        message,
                        new TdApi.InputMessageVideo(
                                newInputFileRemote(video.video.remote.id),
                                newThumbnail(thumbnail),
                                null,
                                video.duration,
                                video.width,
                                video.height,
                                video.supportsStreaming,
                                content.caption,
                                0
                        )
                );
                break;
            }
            case TdApi.MessageDocument.CONSTRUCTOR: {
                TdApi.MessageDocument content = (TdApi.MessageDocument)message.content;
                TdApi.Document document = content.document;
                TdApi.Thumbnail thumbnail = document.thumbnail;
                String id = document.document.remote.id;
                sendMessage(
                        message,
                        new TdApi.InputMessageDocument(
                                newInputFileRemote(id),
                                newThumbnail(thumbnail),
                                content.caption
                        )
                );
                break;
            }
            case TdApi.MessageVoiceNote.CONSTRUCTOR: {
                TdApi.MessageVoiceNote content = (TdApi.MessageVoiceNote)message.content;
                TdApi.VoiceNote voiceNote = content.voiceNote;
                sendMessage(
                        message,
                        new TdApi.InputMessageVoiceNote(
                                newInputFileRemote(voiceNote.voice.remote.id),
                                voiceNote.duration,
                                voiceNote.waveform,
                                content.caption
                        )
                );
                break;
            }
            case TdApi.MessageAnimation.CONSTRUCTOR: {
                TdApi.MessageAnimation content = (TdApi.MessageAnimation)message.content;
                TdApi.Animation animation = content.animation;
                sendMessage(
                        message,
                        new TdApi.InputMessageAnimation(
                                newInputFileRemote(animation.animation.remote.id),
                                newThumbnail(animation.thumbnail),
                                null,
                                animation.duration,
                                animation.width,
                                animation.height,
                                content.caption
                        )
                );
                break;
            }
        }
    }

    private TdApi.InputThumbnail newThumbnail(TdApi.Thumbnail thumbnail) {
        if (thumbnail == null) {
            return null;
        }
        return new TdApi.InputThumbnail(newInputFileRemote(thumbnail.file.remote.id), thumbnail.width, thumbnail.height);
    }

    private TdApi.InputFileRemote newInputFileRemote(String id) {
        return new TdApi.InputFileRemote(id);
    }

    private void sendMessage(TdApi.Message message, TdApi.InputMessageContent content) {
        long requestId = loop.send(
                new TdApi.SendMessage(
                        outputChatId,
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
        Long msgId = messageMap.get(content.messageId);
        if (msgId == null) {
            return;
        }
        switch (content.newContent.getConstructor()) {
            case TdApi.MessageText.CONSTRUCTOR: {
                TdApi.MessageText msg = (TdApi.MessageText)content.newContent;
                loop.send(
                        new TdApi.EditMessageText(
                                outputChatId,
                                msgId,
                                null,
                                new TdApi.InputMessageText(msg.text, false, true)
                        ),
                        this
                );
            }
        }
    }

    private void setMessageId(TdApi.UpdateChatLastMessage msg) {
        if (msg == null || msg.lastMessage == null) {
            System.err.println("Caiu nulo nessa porra: " + msg);
            return;
        }
        SendingMessage sendingMessage = new SendingMessage(msg.chatId, msg.lastMessage.senderUserId, msg.lastMessage.date);
        Long messageId = stateSending.get(sendingMessage);
        if (messageId == null) {
            return;
        }
        stateSending.remove(sendingMessage);
        messageMap.put(messageId, msg.lastMessage.id);
    }
}

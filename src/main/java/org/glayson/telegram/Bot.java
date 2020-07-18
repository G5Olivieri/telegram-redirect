package org.glayson.telegram;

public final class Bot implements Handler {
    private final EventLoop loop;
    private final ChatsHandler chatsHandler;
    private final Forwarders forwarders;
    private long me;

    public Bot(EventLoop loop, long me, ChatsHandler chatsHandler, Forwarders forwarders) {
        this.loop = loop;
        this.me = me;
        this.chatsHandler = chatsHandler;
        this.forwarders = forwarders;
    }

    @Override
    public void handle(long eventId, TdApi.Object object) {
        if (object.getConstructor() != TdApi.UpdateChatLastMessage.CONSTRUCTOR) {
            return;
        }
        TdApi.UpdateChatLastMessage updateChatLastMessage = (TdApi.UpdateChatLastMessage)object;
        if (updateChatLastMessage.lastMessage.content.getConstructor() != TdApi.MessageText.CONSTRUCTOR) {
            return;
        }
        TdApi.MessageText content = (TdApi.MessageText)updateChatLastMessage.lastMessage.content;
        if (!content.text.text.startsWith("!")) {
            return;
        }
        String command = content.text.text.substring(1);
        commandHandler(command);
    }

    private void commandHandler(String command) {
        String[] args = command.split(" ");
        switch (args[0]) {
            case "chats": {
                chatsHandler.getChats(this::sendMessage);
                break;
            }
            case "redirect": {
                Long inputChatId = Long.parseLong(args[1]);
                Long outputChatId = Long.parseLong(args[2]);
                chatsHandler.getChatsByIds(inputChatId, outputChatId, (in, out) -> {
                    forwarders.addForwarder(in, out);
                    sendMessage("Redirecionando de " + in + " para " + out );
                });
                break;
            }
            case "redirects": {
                forwarders.redirectsAsString().forEach(this::sendMessage);
                break;
            }
            case "remove": {
                Long inputChatId = Long.parseLong(args[1]);
                Long outputChatId = Long.parseLong(args[2]);
                forwarders.removeHandler(inputChatId, outputChatId);
                break;
            }
        }
    }

    private void sendMessage(String message) {
        if (message == null || message.isBlank()) {
            message = "Nada a declarar";
        }
        loop.send(
                new TdApi.SendMessage(
                        me,
                        0,
                        null,
                        null,
                        new TdApi.InputMessageText(
                                new TdApi.FormattedText("Bot: " + message, null),
                                false,
                                true
                        )
                ),
                (i, e) -> {}
        );
    }
}

package org.glayson.telegram;

public final class Bot implements Handler {
    private final EventLoop loop;
    private final ChatsHandler chatsHandler;
    private final Forwarders forwarders;
    private final long me;

    public Bot(EventLoop loop, long me, ChatsHandler chatsHandler, Forwarders forwarders) {
        this.loop = loop;
        this.me = me;
        this.chatsHandler = chatsHandler;
        this.forwarders = forwarders;
        sendMessage("Oi eu sou o BOT para saber mais envie '!commands'\n");
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
                chatsHandler.getChats(this::sendChatMessage);
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
            case "commands": {
                sendMessage(
                    "\n\n!chats - mostra os principais chats\n" +
                    "!redirect <chat_id_input> <chat_id_output> - redireciona de um chat para o outro\n" +
                    "!redirects - mostra todos os redirects que estão ativos\n" +
                    "!remove <chat_id_input> <chat_id_output> - remove um redirecionamento\n\n" +
                    "Tudo entre '<>' são variáveis, substitua com os valores. Exemplo: <chat_id_input> = 123454\n"
                );
                break;
            }
        }
    }

    private void sendChatMessage(TdApi.Chat chat) {
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

        sendMessage(String.format("Chat (%s): %s (%s)\n", chat.id, chat.title, type.toString()));
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

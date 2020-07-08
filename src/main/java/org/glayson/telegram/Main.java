package org.glayson.telegram;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public final class Main {
    static {
        System.loadLibrary("tdjni");
    }

    public static void main(String[] args) {
        TelegramNativeClient.nativeClientExecute(new TdApi.SetLogVerbosityLevel(0));
        if (TelegramNativeClient.nativeClientExecute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27))) instanceof TdApi.Error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        final UpdatesHandler updatesHandler = new UpdatesHandler();
        final EventLoop loop = new EventLoop(updatesHandler);

        final AuthorizationHandler authHandler = new AuthorizationHandler(loop);
        final ChatsHandler chatsHandler = new ChatsHandler(loop);
        final ChatHandler chatHandler = new ChatHandler(loop);
        final UpdateMessageHandler updateMessageHandler = new UpdateMessageHandler(loop);

        updatesHandler.setHandler(TdApi.UpdateAuthorizationState.CONSTRUCTOR, authHandler);
        updatesHandler.setHandler(TdApi.UpdateNewMessage.CONSTRUCTOR, updateMessageHandler);
        updatesHandler.setHandler(TdApi.UpdateMessageContent.CONSTRUCTOR, updateMessageHandler);
        updatesHandler.setHandler(TdApi.UpdateChatLastMessage.CONSTRUCTOR, updateMessageHandler);

        loop.start();

        final Future<Boolean> login = authHandler.login();
        try {
            System.out.println(login.get());
            String command = "";
            while(!command.equals("q")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("q para sair");
                command = reader.readLine();
                commandHandler(command, chatsHandler, chatHandler, updateMessageHandler, loop);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            loop.close();
        }
    }

    private static void commandHandler(
            String command,
            ChatsHandler chatsHandler,
            ChatHandler chatHandler,
            UpdateMessageHandler forwarderMessagesHandler,
            EventLoop loop
    ) throws ExecutionException, InterruptedException {
        String[] args = command.split(" ");
        switch (args[0]) {
            case "gcs": {
                TdApi.Chats chats = chatsHandler.getChats().get();
                for (long chatId : chats.chatIds) {
                    TdApi.Chat chat = chatHandler.getChat(chatId).get();
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
                break;
            }
            case "nm": {
                Long inputChatId = Long.parseLong(args[1]);
                Long outputChatId = Long.parseLong(args[2]);
                final Forwarder forwarder = new Forwarder(loop, outputChatId);
                forwarderMessagesHandler.putChatHandler(inputChatId, forwarder);
                forwarderMessagesHandler.putChatHandler(outputChatId, forwarder);
                break;
            }
            case "gm": {
                loop.send(new TdApi.GetMessage(Long.parseLong(args[1]), Long.parseLong(args[2])), (eventId, object) -> {
                    System.out.println("GET MESSAGE: " + object);
                });
                break;
            }
        }
    }
}

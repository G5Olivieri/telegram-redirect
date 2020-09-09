package org.glayson.telegram;

import java.io.IOError;
import java.io.IOException;

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

        final ChatsHandler chatsHandler = new ChatsHandler(loop);
        final UpdateMessageHandler updateMessageHandler = new UpdateMessageHandler();
        final Forwarders forwarders = new Forwarders(loop, updateMessageHandler);


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nSHUTDOWN");
            loop.close();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        loop.onStartup(() -> {
            loop.send(new TdApi.GetMe(), (eventId, object) -> {
                if (object.getConstructor() == TdApi.Error.CONSTRUCTOR) {
                    System.err.println("GET ME ERROR");
                    return;
                }

                TdApi.User user = (TdApi.User)object;
                chatsHandler.getChats((chat) -> {
                    if (chat.type.getConstructor() == TdApi.ChatTypePrivate.CONSTRUCTOR) {
                        if (chat.title.equals(user.firstName) && chat.lastMessage.senderUserId == user.id) {
                            updateMessageHandler.putChatHandler(chat.id, new Bot(loop, chat.id, chatsHandler, forwarders));
                            System.out.println("BOT IS RUNNING");
                        }
                    }
                });
            });
        });

        updatesHandler.setHandler(TdApi.UpdateNewMessage.CONSTRUCTOR, updateMessageHandler);
        updatesHandler.setHandler(TdApi.UpdateMessageContent.CONSTRUCTOR, updateMessageHandler);
        updatesHandler.setHandler(TdApi.UpdateChatLastMessage.CONSTRUCTOR, updateMessageHandler);

        System.out.println("INICIANDO");
        loop.start();
    }
}

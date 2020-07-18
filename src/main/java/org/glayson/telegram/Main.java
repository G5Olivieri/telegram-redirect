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

        updateMessageHandler.putChatHandler(1198182309L, new Bot(loop, 1198182309L, chatsHandler, forwarders));

        updatesHandler.setHandler(TdApi.UpdateNewMessage.CONSTRUCTOR, updateMessageHandler);
        updatesHandler.setHandler(TdApi.UpdateMessageContent.CONSTRUCTOR, updateMessageHandler);
        updatesHandler.setHandler(TdApi.UpdateChatLastMessage.CONSTRUCTOR, updateMessageHandler);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nFECHANDO");
            loop.close();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));

        System.out.println("INICIANDO");
        loop.start();
    }
}

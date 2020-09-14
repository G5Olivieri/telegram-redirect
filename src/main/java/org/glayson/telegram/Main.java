package org.glayson.telegram;

import java.io.IOError;
import java.io.IOException;

public final class Main {
    static {
        System.loadLibrary("tdjni");
    }

    public static void main(String[] args) {
        setupLog();

        final UpdatesHandler updatesHandler = new UpdatesHandler();
        final EventLoop loop = new EventLoop(updatesHandler);

        final UpdateMessageHandler updateMessageHandler = new UpdateMessageHandler();
        final AuthorizationHandler auth = new AuthorizationHandler(loop);

        updatesHandler
                .setHandler(TdApi.UpdateAuthorizationState.CONSTRUCTOR, auth)
                .setHandler(TdApi.UpdateNewMessage.CONSTRUCTOR, updateMessageHandler)
                .setHandler(TdApi.UpdateMessageContent.CONSTRUCTOR, updateMessageHandler)
                .setHandler(TdApi.UpdateChatLastMessage.CONSTRUCTOR, updateMessageHandler);

        loop.run();
    }

    private static void setupLog() {
        TelegramNativeClient.nativeClientExecute(new TdApi.SetLogVerbosityLevel(0));
        if (TelegramNativeClient.nativeClientExecute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27))) instanceof TdApi.Error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }
    }
}

package org.glayson.telegram;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class Main {
    static {
        System.loadLibrary("tdjni");
    }

    public static void main(String[] args) {
        TelegramNativeClient.nativeClientExecute(new TdApi.SetLogVerbosityLevel(0));
        if (TelegramNativeClient.nativeClientExecute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27))) instanceof TdApi.Error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        final EventLoop loop = new EventLoop();
        final EventHandler eventHandler = new EventHandler(loop);

        final AuthorizationHandler authHandler = new AuthorizationHandler(loop);
        final ChatsHandler chatsHandler = new ChatsHandler(loop);

        eventHandler.setHandler(TdApi.UpdateAuthorizationState.CONSTRUCTOR, authHandler);
        eventHandler.setHandler(TdApi.Chats.CONSTRUCTOR, chatsHandler);

        loop.start();

        final Future<Boolean> login = authHandler.login();
        try {
            System.out.println(login.get(3, TimeUnit.SECONDS));
            System.out.println(chatsHandler.getChats().get(3, TimeUnit.SECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        loop.close();
    }
}

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

        final long clientId = TelegramNativeClient.createNativeClient();
        final Authorization auth = new Authorization();
        final EventHandler eventHandler = new EventHandler(auth);
        final EventLoop loop = new EventLoop(clientId, eventHandler);
        loop.start();
        final Future<Boolean> login = auth.login();
        try {
            System.out.println(login.get(3, TimeUnit.SECONDS));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
        loop.enqueue(new TdApi.Close());
    }
}

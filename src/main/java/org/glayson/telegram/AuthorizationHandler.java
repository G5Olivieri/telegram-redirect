package org.glayson.telegram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class AuthorizationHandler implements Handler {
    private final EventLoop loop;
    private final Object lock = new Object();
    private volatile boolean auth = false;

    public AuthorizationHandler(EventLoop loop) {
        this.loop = loop;
    }

    public Future<Boolean> login() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return executorService.submit(() -> {
            try {
                synchronized (lock) {
                    lock.wait();
                }
            }
            finally {
                executorService.shutdown();
            }
            return auth;
        });
    }

    @Override
    public void handle(TdApi.Object object) {
        TdApi.AuthorizationState authorizationState = ((TdApi.UpdateAuthorizationState)object).authorizationState;
        switch (authorizationState.getConstructor()) {
            case TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR: {
                TdApi.TdlibParameters parameters = new TdApi.TdlibParameters();
                parameters.apiHash = System.getenv("API_HASH");
                parameters.apiId = Integer.parseInt(System.getenv("API_ID"));
                parameters.applicationVersion = "1.0";
                parameters.databaseDirectory = "tdlib";
                parameters.deviceModel = "Desktop";
                parameters.enableStorageOptimizer = true;
                parameters.systemLanguageCode = "en";
                parameters.systemVersion = "Unknown";
                parameters.useMessageDatabase = true;
                parameters.useSecretChats = true;
                loop.send(new TdApi.SetTdlibParameters(parameters));
                break;
            }
            case TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR: {
                this.loop.send(new TdApi.CheckDatabaseEncryptionKey());
                break;
            }
            case TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR: {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Enter phone number: ");
                String phoneNumber = "";
                try {
                    phoneNumber = reader.readLine();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                loop.send(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null));
                break;
            }
            case TdApi.AuthorizationStateWaitCode.CONSTRUCTOR: {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("Enter code: ");
                String code = "";
                try {
                    code = reader.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.loop.send(new TdApi.CheckAuthenticationCode(code));
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR: {
                synchronized (lock) {
                    auth = true;
                    lock.notify();
                }
                break;
            }
            case TdApi.AuthorizationStateClosing.CONSTRUCTOR: {
                System.out.println("Closing");
                break;
            }
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR: {
                System.out.println("Closed");
                this.loop.stop();
                break;
            }
        }
    }
}

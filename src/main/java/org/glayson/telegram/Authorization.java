package org.glayson.telegram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class Authorization {
    private volatile boolean auth = false;

    public Future<Boolean> login() {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        return executorService.submit(() -> {
            try {
                while(!auth);
            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                executorService.shutdown();
            }
            return auth;
        });
    }

    public void onAuthorization(EventLoop loop, TdApi.AuthorizationState authorizationState) {
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
                loop.enqueue(new TdApi.SetTdlibParameters(parameters));
                break;
            }
            case TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR: {
                loop.enqueue(new TdApi.CheckDatabaseEncryptionKey());
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
                loop.enqueue(new TdApi.SetAuthenticationPhoneNumber(phoneNumber, null));
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
                loop.enqueue(new TdApi.CheckAuthenticationCode(code));
                break;
            }
            case TdApi.AuthorizationStateReady.CONSTRUCTOR: {
                auth = true;
                break;
            }
            case TdApi.AuthorizationStateClosing.CONSTRUCTOR: {
                System.out.println("Closing");
                break;
            }
            case TdApi.AuthorizationStateClosed.CONSTRUCTOR: {
                System.out.println("Closed");
                loop.stop();
                break;
            }
        }
    }
}

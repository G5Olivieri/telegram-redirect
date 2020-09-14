package org.glayson.telegram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class AuthorizationHandler implements Handler {
    private final EventLoop loop;
    private final UpdatesHandler updatesHandler = getUpdatesHandler();

    public AuthorizationHandler(EventLoop loop) {
        this.loop = loop;
    }

    @Override
    public void handle(long eventId, TdApi.Object object) {
        if (object.getConstructor() == TdApi.Error.CONSTRUCTOR) {
            System.out.println(object);
            return;
        }

        if (object.getConstructor() != TdApi.UpdateAuthorizationState.CONSTRUCTOR) {
            return;
        }

        TdApi.AuthorizationState authorizationState = ((TdApi.UpdateAuthorizationState)object).authorizationState;
        updatesHandler.handle(eventId, authorizationState);
    }

    private UpdatesHandler getUpdatesHandler() {
        return new UpdatesHandler()
                .setHandler(TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR, getParameters())
                .setHandler(TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR, (e, o) -> {
                    loop.send(new TdApi.CheckDatabaseEncryptionKey(), this);
                })
                .setHandler(TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR, (e, o) -> {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    System.out.print("Enter phone number: ");
                    String phoneNumber = "";
                    try {
                        phoneNumber = reader.readLine();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    loop.send(new TdApi.SetAuthenticationPhoneNumber(
                            phoneNumber,
                            new TdApi.PhoneNumberAuthenticationSettings(false, true, true)
                    ), this);
                })
                .setHandler(TdApi.AuthorizationStateWaitCode.CONSTRUCTOR, (e, o) -> {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    System.out.print("Enter code: ");
                    String code = "";
                    try {
                        code = reader.readLine();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    loop.send(new TdApi.CheckAuthenticationCode(code), this);
                })
                .setHandler(TdApi.AuthorizationStateReady.CONSTRUCTOR, (e, o) -> {
                    System.out.println("AUTHORIZED");
                })
                .setHandler(TdApi.AuthorizationStateClosing.CONSTRUCTOR, (e, o) -> {
                    System.out.println("CLOSING");
                })
                .setHandler(TdApi.AuthorizationStateClosed.CONSTRUCTOR, (e, o) -> {
                    System.out.println("CLOSED");
                    loop.stop();
                });
    }

    private Handler getParameters() {
        return new Handler() {
            @Override
            public void handle(long eventId, TdApi.Object object) {
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
                loop.send(new TdApi.SetTdlibParameters(parameters), this);
            }
        };
    }
}
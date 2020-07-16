package org.glayson.telegram;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public final class AuthorizationHandler implements Handler {
    private final EventLoop loop;
    private volatile boolean auth = false;

    public AuthorizationHandler(EventLoop loop) {
        this.loop = loop;
    }

    public Boolean login() {
        return auth;
    }

    @Override
    public void handle(long eventId, TdApi.Object object) {
        if (object.getConstructor() != TdApi.UpdateAuthorizationState.CONSTRUCTOR) {
            System.out.println("NOT MAPPED AUTHORIZATION: " + object);
            return;
        }
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
                loop.send(new TdApi.SetTdlibParameters(parameters), this);
                break;
            }
            case TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR: {
                loop.send(new TdApi.CheckDatabaseEncryptionKey(), this);
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
                loop.send(new TdApi.SetAuthenticationPhoneNumber(
                        phoneNumber,
                        new TdApi.PhoneNumberAuthenticationSettings(false, true, true)
                ), this);
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
                loop.send(new TdApi.CheckAuthenticationCode(code), this);
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

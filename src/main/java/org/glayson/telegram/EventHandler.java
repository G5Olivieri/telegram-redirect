package org.glayson.telegram;

public class EventHandler {
    private final Authorization auth;

    public EventHandler(Authorization auth) {
        this.auth = auth;
    }

    public void handle(EventLoop loop, TdApi.Object object) {
        switch (object.getConstructor()) {
            case TdApi.UpdateAuthorizationState.CONSTRUCTOR: {
                this.auth.onAuthorization(loop, ((TdApi.UpdateAuthorizationState)object).authorizationState);
                break;
            }
        }
    }
}

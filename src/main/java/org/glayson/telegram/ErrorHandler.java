package org.glayson.telegram;

public interface ErrorHandler {
    void onError(TdApi.Error error);
}

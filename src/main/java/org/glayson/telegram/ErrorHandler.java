package org.glayson.telegram;

public interface ErrorHandler {
    void onError(long eventId, TdApi.Error error);
}

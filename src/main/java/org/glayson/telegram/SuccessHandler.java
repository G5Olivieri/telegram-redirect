package org.glayson.telegram;

public interface SuccessHandler {
    void onSuccess(long eventId, TdApi.Object object);
}

package org.glayson.telegram;

public interface AbstractHandler extends Handler, SuccessHandler, ErrorHandler {
    @Override
    default void handle(long eventId, TdApi.Object object) {
        if(object.getConstructor() == TdApi.Error.CONSTRUCTOR) {
            onError(eventId, (TdApi.Error)object);
            return;
        }
        onSuccess(eventId, object);
    }
}

package org.glayson.telegram;

public interface AbstractHandler extends Handler, SuccessHandler, ErrorHandler {
    @Override
    default void handle(TdApi.Object object) {
        if(object.getConstructor() == TdApi.Error.CONSTRUCTOR) {
            onError((TdApi.Error)object);
            return;
        }
        onSuccess(object);
    }
}

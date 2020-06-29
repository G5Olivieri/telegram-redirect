package org.glayson.telegram;

public interface Handler {
    void handle(TdApi.Object object);
}

package org.glayson.telegram;

@FunctionalInterface
public interface Handler {
    void handle(TdApi.Object object);
}

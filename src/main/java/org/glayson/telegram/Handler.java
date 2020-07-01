package org.glayson.telegram;

@FunctionalInterface
public interface Handler {
    void handle(long eventId, TdApi.Object object);
}

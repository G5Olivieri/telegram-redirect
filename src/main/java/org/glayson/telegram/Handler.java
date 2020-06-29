package org.glayson.telegram;

public interface Handler {
    void handle(EventLoop loop, TdApi.Object object);
}

package org.glayson.telegram;

import java.util.HashMap;
import java.util.Map;

public class EventHandler implements Handler {
    private final EventLoop loop;
    private final Map<Integer, Handler> handlers = new HashMap<>();

    public EventHandler(EventLoop loop) {
        this.loop = loop;
        this.loop.setEventHandler(this);
    }

    public void setHandler(int constructor, Handler handler) {
        handlers.put(constructor, handler);
    }

    @Override
    public void handle(TdApi.Object object) {
        Handler handler = handlers.get(object.getConstructor());
        if (handler == null) {
            // ignore not mapped event
            return;
        }
        handler.handle(object);
    }

}

package org.glayson.telegram;

import java.util.HashMap;
import java.util.Map;

public class EventHandler implements Handler {
    private final Map<Integer, Handler> handlers = new HashMap<>();

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

package org.glayson.telegram;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class UpdatesHandler implements Handler {
    private final Map<Integer, Handler> handlers = new HashMap<>();

    public UpdatesHandler setHandler(int constructor, Handler handler) {
        handlers.put(constructor, handler);
        return this;
    }

    @Override
    public void handle(long eventId, TdApi.Object object) {
        Optional.ofNullable(handlers.get(object.getConstructor()))
                .ifPresent((handler) -> handler.handle(eventId, object));
    }

}

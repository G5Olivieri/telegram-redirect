package org.glayson.telegram;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class EventLoop implements Runnable {
    private final int EVENT_SIZE = 100;
    private final long[] eventIds = new long[EVENT_SIZE];
    private final TdApi.Object[] events = new TdApi.Object[EVENT_SIZE];
    private final long clientId;

    private final AtomicLong currentQueryId = new AtomicLong();
    private final Map<Long, Handler> handlers = new ConcurrentHashMap<>();
    private volatile boolean isActive = true;

    public EventLoop(Handler updatesHandler) {
        this.clientId = TelegramNativeClient.createNativeClient();
        this.handlers.put(0L, updatesHandler);
    }

    public void stop() {
        isActive = false;
    }

    public void start() {
        AuthorizationHandler auth = new AuthorizationHandler(this);
        UpdatesHandler updatesHandler = (UpdatesHandler)this.handlers.get(0L);
        updatesHandler.setHandler(TdApi.UpdateAuthorizationState.CONSTRUCTOR, auth);

        while (!auth.login()) {
            receiveQueries(300);
        }

        System.out.println("It's authorization: " + auth.login());

        run();
    }

    public synchronized long send(TdApi.Function function, Handler handler) {
        Objects.requireNonNull(function);
        Objects.requireNonNull(handler);
        long queryId = currentQueryId.incrementAndGet();
        handlers.put(queryId, handler);
        TelegramNativeClient.nativeClientSend(clientId, queryId, function);
        return queryId;
    }

    public synchronized void close() {
        send(new TdApi.Close(), (id, e) -> System.out.println("Close received: " + e));
    }

    @Override
    public void run() {
        int timeout = 300;
        while(isActive) {
            receiveQueries(timeout);
        }
        while(handlers.size() != 1) {
            receiveQueries(timeout);
        }
        TelegramNativeClient.destroyNativeClient(clientId);
    }

    private void receiveQueries(int timeoutInSeconds) {
        int result = TelegramNativeClient.nativeClientReceive(clientId, eventIds, events, timeoutInSeconds);
        for(int i = 0; i < result; i++) {
            processEvent(eventIds[i], events[i]);
            eventIds[i] = 0;
            events[i] = null;
        }
    }

    private void processEvent(long eventId, TdApi.Object event) {
        Handler handler = this.handlers.get(eventId);
        if (handler == null) {
           return;
        }
        handler.handle(eventId, event);
        if (eventId != 0){
            this.handlers.remove(eventId);
        }
    }
}

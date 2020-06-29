package org.glayson.telegram;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class EventLoop implements Runnable {
    private Handler eventHandler;

    private final int eventSize = 100;
    private final long[] eventIds = new long[eventSize];
    private final TdApi.Object[] events = new TdApi.Object[eventSize];
    private final long clientId;

    private final AtomicLong currentQueryId = new AtomicLong();
    private final Set<Long> eventQueue = ConcurrentHashMap.newKeySet();
    private volatile boolean isActive = true;

    public EventLoop() {
        this.clientId = TelegramNativeClient.createNativeClient();
        this.eventQueue.add(0L);
    }

    public void setEventHandler(Handler eventHandler) {
        this.eventHandler = eventHandler;
    }
    public void stop() {
        isActive = false;
    }

    public void start() {
        new Thread(this).start();
    }

    public void send(TdApi.Function function) {
        long queryId = currentQueryId.incrementAndGet();
        eventQueue.add(queryId);
        TelegramNativeClient.nativeClientSend(clientId, queryId, function);
    }

    public void close() {
        send(new TdApi.Close());
    }

    @Override
    public void run() {
        int timeout = 300;
        while(isActive) {
            receiveQueries(timeout);
        }
        while(eventQueue.size() != 1) {
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
        this.eventHandler.handle(event);
        if (eventId != 0){
            this.eventQueue.remove(eventId);
        }
    }
}

package org.glayson.telegram;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class EventLoop implements Runnable {
    private final EventHandler eventHandler;

    private final int eventSize = 100;
    private final long[] eventIds = new long[eventSize];
    private final TdApi.Object[] events = new TdApi.Object[eventSize];
    private final long clientId;
    private final AtomicLong currentQueryId = new AtomicLong();
    private volatile boolean isActive = true;
    private final Set<Long> eventQueue = ConcurrentHashMap.newKeySet();

    public EventLoop(long clientId, EventHandler eventHandler) {
        this.clientId = clientId;
        this.eventHandler = eventHandler;
        this.eventQueue.add(0L);
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

    public void waitQueueEmpty() {
        while(eventQueue.size() != 1);
    }

    @Override
    public void run() {
        while(isActive) {
            receiveQueries(300);
        }
        while(eventQueue.size() != 1) {
            receiveQueries(300);
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
        this.eventHandler.handle(this, event);
        if (eventId != 0){
            this.eventQueue.remove(eventId);
        }
    }
}

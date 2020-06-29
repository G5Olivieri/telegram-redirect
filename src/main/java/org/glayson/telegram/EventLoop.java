package org.glayson.telegram;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public final class EventLoop implements Runnable {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler eventHandler;

    private final int EVENT_SIZE = 100;
    private final long[] eventIds = new long[EVENT_SIZE];
    private final TdApi.Object[] events = new TdApi.Object[EVENT_SIZE];
    private final long clientId;

    private final AtomicLong currentQueryId = new AtomicLong();
    private final Set<Long> eventQueue = ConcurrentHashMap.newKeySet();
    private volatile boolean isActive = true;

    public EventLoop(Handler eventHandler) {
        this.clientId = TelegramNativeClient.createNativeClient();
        this.eventQueue.add(0L);
        this.eventHandler = eventHandler;
    }

    public void stop() {
        isActive = false;
    }

    public void start() {
        new Thread(this).start();
    }

    public void send(TdApi.Function function) {
        Objects.requireNonNull(function);
        long queryId = currentQueryId.incrementAndGet();
        eventQueue.add(queryId);
        TelegramNativeClient.nativeClientSend(clientId, queryId, function);
    }

    public <T> Future<T> execute(Callable<T> callable) {
        Objects.requireNonNull(callable);
        return this.executor.submit(callable);
    }

    public void close() {
        this.executor.shutdown();
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

package org.glayson.telegram;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class EventLoop implements java.lang.Runnable {
    private final int EVENT_SIZE = 100;
    private final long[] eventIds = new long[EVENT_SIZE];
    private final TdApi.Object[] events = new TdApi.Object[EVENT_SIZE];
    private final long clientId;

    private final Random random = new Random();
    private final Map<Long, Handler> handlers = new HashMap<>();
    private volatile boolean isActive = true;

    public EventLoop(Handler updatesHandler) {
        this.clientId = TelegramNativeClient.createNativeClient();
        this.handlers.put(0L, updatesHandler);
    }

    @Override
    public void run() {
        onInterrupted();
        while(isActive) {
            receiveQueries();
        }

        while (handlers.size() != 1) {
            receiveQueries();
        }
        TelegramNativeClient.destroyNativeClient(clientId);
    }

    public void stop() {
        isActive = false;
    }

    public long send(TdApi.Function function, Handler handler) {
        Objects.requireNonNull(function);
        Objects.requireNonNull(handler);
        final long queryId = random.nextLong();
        handlers.put(queryId, handler);
        TelegramNativeClient.nativeClientSend(clientId, queryId, function);
        return queryId;
    }

    private void onInterrupted() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nSHUTDOWN");
            this.close();
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }));
    }

    private void close() {
        send(new TdApi.Close(), (id, object) -> {
            String status = object.getConstructor() == TdApi.Ok.CONSTRUCTOR ? "OK" : "ERROR";
            System.out.println("Close request received status: " + status);
        });
    }

    private void receiveQueries() {
        int result = TelegramNativeClient.nativeClientReceive(clientId, eventIds, events, 300);
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

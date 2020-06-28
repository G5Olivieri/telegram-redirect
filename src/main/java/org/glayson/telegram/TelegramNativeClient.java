package org.glayson.telegram;

public final class TelegramNativeClient {
    private TelegramNativeClient() {
        throw new UnsupportedOperationException("It is not allowed to instantiate the class " + getClass().getName());
    }
    public static native long createNativeClient();
    public static native void nativeClientSend(long nativeClientId, long eventId, TdApi.Function function);
    public static native int nativeClientReceive(long nativeClientId, long[] eventIds, TdApi.Object[] events, double timeout);
    public static native TdApi.Object nativeClientExecute(TdApi.Function function);
    public static native void destroyNativeClient(long nativeClientId);
}

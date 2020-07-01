package org.glayson.telegram;

import java.util.Objects;

public final class SendingMessage implements Comparable<SendingMessage> {
    private final int senderUserId;
    private final long chatId;
    private final int date;

    public SendingMessage(long chatId, int senderUserId, int date) {
        this.chatId = chatId;
        this.senderUserId = senderUserId;
        this.date = date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SendingMessage that = (SendingMessage) o;
        return senderUserId == that.senderUserId &&
                chatId == that.chatId &&
                date == that.date;
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderUserId, chatId, date);
    }

    @Override
    public int compareTo(SendingMessage sendingMessage) {
        return hashCode() - sendingMessage.hashCode();
    }

    @Override
    public String toString() {
        return "SendingMessage{" +
                "senderUserId=" + senderUserId +
                ", chatId=" + chatId +
                ", date=" + date +
                '}';
    }
}

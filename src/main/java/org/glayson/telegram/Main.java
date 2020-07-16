package org.glayson.telegram;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;

public final class Main {
    static {
        System.loadLibrary("tdjni");
    }

    public static void main(String[] args) {
        TelegramNativeClient.nativeClientExecute(new TdApi.SetLogVerbosityLevel(0));
        if (TelegramNativeClient.nativeClientExecute(new TdApi.SetLogStream(new TdApi.LogStreamFile("tdlib.log", 1 << 27))) instanceof TdApi.Error) {
            throw new IOError(new IOException("Write access to the current directory is required"));
        }

        final UpdatesHandler updatesHandler = new UpdatesHandler();
        final EventLoop loop = new EventLoop(updatesHandler);

        final ChatsHandler chatsHandler = new ChatsHandler(loop);
        final UpdateMessageHandler updateMessageHandler = new UpdateMessageHandler();

        updatesHandler.setHandler(TdApi.UpdateNewMessage.CONSTRUCTOR, updateMessageHandler);
        updatesHandler.setHandler(TdApi.UpdateMessageContent.CONSTRUCTOR, updateMessageHandler);
        updatesHandler.setHandler(TdApi.UpdateChatLastMessage.CONSTRUCTOR, updateMessageHandler);

        loop.start();

        try {
            String command = "";
            while(!command.equals("q")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("q para sair: ");
                command = reader.readLine();
                commandHandler(command, loop, chatsHandler, updateMessageHandler);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            loop.close();
        }
    }

    private static void commandHandler(String command, EventLoop loop, ChatsHandler chatsHandler, UpdateMessageHandler updateMessageHandler) {
        String[] args = command.split(" ");
        switch (args[0]) {
            case "chats": {
                chatsHandler.getChats();
                break;
            }
            case "redirect": {
                Long inputChatId = Long.parseLong(args[1]);
                Long outputChatId = Long.parseLong(args[2]);
                final Forwarder forwarder = new Forwarder(loop, inputChatId, outputChatId);
                updateMessageHandler.addHandler(forwarder);
                System.out.println("Redirecionando de " + inputChatId + " para " + outputChatId);
                break;
            }
            case "redirects": {
                updateMessageHandler.printRedirects();
                break;
            }
            case "remove": {
                Long inputChatId = Long.parseLong(args[1]);
                Long outputChatId = Long.parseLong(args[2]);
                updateMessageHandler.removeHandler(inputChatId, outputChatId);
                break;
            }
        }
    }
}

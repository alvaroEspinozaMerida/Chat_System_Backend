package com.espinozameridaal;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

//Represents Bridge between the SpringBoot Controller to the RawTCP Chat Server
public class TcpChatClient {
    private Socket socket;

    private final String host;
    private final int port;


    private PrintWriter out;
    private BufferedReader in;

    public TcpChatClient(String host, int port) throws IOException {
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() throws IOException {
        socket = new Socket(this.host,this.port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("TcpChatClient connected to ChatCoreServer at " + host + ":" + port);
    }

//    Where Message From React goes to ChatCore Server
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }


//    Where we are receiving messages from the ChatCoreServer

    public void startListener(Consumer<String> onMessage) {
        Thread listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {

                    onMessage.accept(line);
                }
            } catch (IOException e) {
                System.out.println("TcpChatClient listener error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException ignored) {}
            }
        });

        listenerThread.setDaemon(true);
        listenerThread.start();
    }


}

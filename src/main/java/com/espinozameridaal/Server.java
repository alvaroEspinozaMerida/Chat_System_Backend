package com.espinozameridaal;

import com.espinozameridaal.Database.Database;

import java.sql.SQLException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket;

    private static final int VOICE_UDP_PORT = 50005;
    private static final int VOICE_BUFFER_SIZE = 1024 + 8; // 8 bytes for userId + audio

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    /*
     * start: Represents the server running and accepting connections from
     * different clients on the server
     * creates a new ClientHandler for the client connected
     * uses virtual threads
     */
    public void start() {
        System.out.println("Server has started on port: " + serverSocket.getLocalPort());

        // Start UDP voice relay thread
        Thread voiceThread = new Thread(this::runVoiceRelayLoop, "VoiceRelayThread");
        voiceThread.setDaemon(true);
        voiceThread.start();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            while (true) {
                Socket clientSocket = this.serverSocket.accept();
                var clientIP = clientSocket.getInetAddress().getHostAddress();
                var clientPort = clientSocket.getPort();
                System.out.println("Accepted connection from " +
                        clientSocket.getInetAddress().getHostName() + ":" + clientIP +
                        " (port " + clientPort + ")");

                executor.submit(() -> {
                    ClientHandler clientHandler = new ClientHandler(clientSocket);
                    clientHandler.run();
                });
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void runVoiceRelayLoop() {
        byte[] buffer = new byte[VOICE_BUFFER_SIZE];
        try (DatagramSocket udpSocket = new DatagramSocket(VOICE_UDP_PORT)) {
            System.out.println("Voice UDP relay listening on port " + VOICE_UDP_PORT);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                int len = packet.getLength();
                if (len <= 8) {
                    continue; // need at least 8 bytes for userId
                }

                ByteBuffer bb = ByteBuffer.wrap(packet.getData(), 0, len);
                long senderId = bb.getLong();
                int audioLen = len - 8;
                byte[] audioData = new byte[audioLen];
                bb.get(audioData);

                SocketAddress senderAddr = packet.getSocketAddress();
                ClientHandler.handleIncomingVoicePacket(senderId, senderAddr, audioData, udpSocket);
            }
        } catch (IOException e) {
            System.out.println("Voice relay stopped: " + e.getMessage());
        }
    }

    public void closeServerSocket() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // init H2 and create tables
            Database.init();

            ServerSocket serverSocket = new ServerSocket(1234);
            Server server = new Server(serverSocket);
            server.start();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }
}

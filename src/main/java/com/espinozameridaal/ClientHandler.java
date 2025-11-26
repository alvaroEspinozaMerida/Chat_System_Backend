package com.espinozameridaal;

import com.espinozameridaal.Models.MessageParser;
import com.espinozameridaal.Models.ParsedMessage;
import com.espinozameridaal.Database.MessageDao;

import java.io.*;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.SocketAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

// Handles a single connected client -- reads lines, routes messages, and stores them in the DB
public class ClientHandler implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    private static final MessageDao messageDao = new MessageDao();

    // Voice chat: userId -> latest known UDP address
    private static final Map<Long, SocketAddress> voiceUdpEndpoints = new ConcurrentHashMap<>();

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    String clientUsername;
    long clientUserId;

    // Per-client voice state
    volatile boolean inVoiceChat = false;
    volatile long voicePartnerId = -1;

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String hello = reader.readLine();
            if (hello == null || !hello.startsWith("HELLO ")) {
                throw new IOException("Client did not send identity.");
            }

            String[] parts = hello.split("\\s+", 3);
            this.clientUserId = Long.parseLong(parts[1]);
            this.clientUsername = (parts.length >= 3) ? parts[2] : ("user-" + clientUserId);

            synchronized (clientHandlers) {
                clientHandlers.add(this);
            }
            writer.write("Welcome " + clientUsername + " (id " + clientUserId + ")");
            writer.newLine();
            writer.flush();

        } catch (IOException e) {
            closeClientHandler(socket, reader, writer);
        }
    }

    @Override
    public void run() {
        try {
            String message;
            // Read from client until the socket closes/error occurs
            while (socket.isConnected() && (message = reader.readLine()) != null) {
                if (message.startsWith("VOICE_START ")) {
                    handleVoiceStart(message);
                } else if (message.equals("VOICE_STOP")) {
                    handleVoiceStop();
                } else {
                    broadcastMessage(message);
                }
            }
        } catch (IOException e) {
            // client disconnects or error while reading
        } finally {
            closeClientHandler(socket, reader, writer);
        }
    }

    private void handleVoiceStart(String message) {
        String[] parts = message.split("\\s+");
        if (parts.length < 2) {
            return;
        }
        try {
            long targetId = Long.parseLong(parts[1]);
            this.inVoiceChat = true;
            this.voicePartnerId = targetId;

            ClientHandler partner = getByUserId(targetId);
            if (partner != null && partner.inVoiceChat && partner.voicePartnerId == this.clientUserId) {
                sendTextLine("VOICE_INFO Voice chat connected with " + partner.clientUsername + ".");
                partner.sendTextLine("VOICE_INFO Voice chat connected with " + this.clientUsername + ".");
            } else {
                sendTextLine("VOICE_INFO Voice chat started with userId=" + targetId +
                        ". Waiting for them to also start voice.");
            }
        } catch (NumberFormatException e) {
            // ignore malformed
        }
    }

    private void handleVoiceStop() {
        this.inVoiceChat = false;
        long oldPartner = this.voicePartnerId;
        this.voicePartnerId = -1;
        voiceUdpEndpoints.remove(this.clientUserId);

        sendTextLine("VOICE_INFO Voice chat stopped.");

        if (oldPartner > 0) {
            ClientHandler partner = getByUserId(oldPartner);
            if (partner != null) {
                partner.sendTextLine("VOICE_INFO " + this.clientUsername + " left the voice chat.");
            }
        }
    }

    private void closeClientHandler(Socket socket, BufferedReader in, BufferedWriter out) {
        removeClientHandler(this);
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        if (message == null || message.isBlank()) {
            return;
        }

        ParsedMessage parsed = MessageParser.parse(message);
        if (parsed == null) {
            System.out.println("Could not parse message: " + message);
            return;
        }

        System.out.println("SENDING");
        System.out.println("to user: " + parsed.userName);
        System.out.println("msg: " + parsed.message);

        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                try {
                    if (!clientHandler.clientUsername.equals(this.clientUsername)
                            && Objects.equals(parsed.userName, clientHandler.clientUsername)) {
                        // 1) Save to DB
                        try {
                            messageDao.saveMessage(this.clientUserId, clientHandler.clientUserId, parsed.message);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        // 2) Deliver to receiver
                        clientHandler.writer.write(this.clientUsername + ": " + parsed.message);
                        clientHandler.writer.newLine();
                        clientHandler.writer.flush();
                    }
                } catch (IOException e) {
                    closeClientHandler(socket, reader, writer);
                }
            }
        }
    }

    private void sendTextLine(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeClientHandler(socket, reader, writer);
        }
    }

    public static ClientHandler getByUserId(long userId) {
        synchronized (clientHandlers) {
            for (ClientHandler ch : clientHandlers) {
                if (ch.clientUserId == userId) {
                    return ch;
                }
            }
        }
        return null;
    }

    public void removeClientHandler(ClientHandler clientHandler) {
        synchronized (clientHandlers) {
            clientHandlers.remove(clientHandler);
        }
        voiceUdpEndpoints.remove(clientHandler.clientUserId);

        // Inform partner if in voice chat
        if (clientHandler.inVoiceChat && clientHandler.voicePartnerId > 0) {
            ClientHandler partner = getByUserId(clientHandler.voicePartnerId);
            if (partner != null) {
                partner.inVoiceChat = false;
                partner.voicePartnerId = -1;
                partner.sendTextLine("VOICE_INFO " + clientHandler.clientUsername +
                        " disconnected from voice chat.");
            }
        }

        String msg = "ChatServer.Server: " + clientHandler.clientUsername + " has left the chat !";
        synchronized (clientHandlers) {
            for (ClientHandler ch : clientHandlers) {
                try {
                    ch.writer.write(msg);
                    ch.writer.newLine();
                    ch.writer.flush();
                } catch (IOException e) {
                    ch.closeClientHandler(ch.socket, ch.reader, ch.writer);
                }
            }
        }
    }

    // Called from Server's UDP relay loop
    public static void handleIncomingVoicePacket(long senderUserId,
                                                 SocketAddress senderAddress,
                                                 byte[] audioData,
                                                 DatagramSocket udpSocket) {
        // update sender's UDP endpoint
        voiceUdpEndpoints.put(senderUserId, senderAddress);

        ClientHandler sender = getByUserId(senderUserId);
        if (sender == null || !sender.inVoiceChat) {
            return;
        }

        long partnerId = sender.voicePartnerId;
        if (partnerId <= 0) {
            return;
        }

        ClientHandler partner = getByUserId(partnerId);
        if (partner == null || !partner.inVoiceChat || partner.voicePartnerId != senderUserId) {
            return;
        }

        SocketAddress partnerAddr = voiceUdpEndpoints.get(partnerId);
        if (partnerAddr == null) {
            return;
        }

        try {
            DatagramPacket out = new DatagramPacket(audioData, audioData.length, partnerAddr);
            udpSocket.send(out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

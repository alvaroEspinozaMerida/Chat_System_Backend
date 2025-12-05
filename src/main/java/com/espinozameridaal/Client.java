package com.espinozameridaal;

import java.io.*;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sound.sampled.*;

import com.espinozameridaal.Models.User;
import com.espinozameridaal.Database.UserDao;
import com.espinozameridaal.Models.FriendRequest;
import com.espinozameridaal.Models.Message;
import com.espinozameridaal.Database.FriendRequestDao;
import com.espinozameridaal.Database.MessageDao;
import lombok.Setter;

public class Client {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private User user;
    private UserDao userDao;
    private FriendRequestDao friendRequestDao;
    private MessageDao messageDao;

    // === Voice chat fields ===
    private static final int VOICE_UDP_PORT = 50005;     // server's UDP voice port
    private static final int AUDIO_BUFFER_SIZE = 1024;   // bytes per audio packet

    private volatile boolean voiceThreadsRunning = false;
    private DatagramSocket voiceSocket;
    private Thread voiceCaptureThread;
    private Thread voiceReceiveThread;

    @Setter
    private StatsListener statsListener;



    public Client(Socket socket, User user, UserDao userDao) {
        try {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.user = user;
            this.userDao = userDao;
            this.friendRequestDao = new FriendRequestDao();
            this.messageDao = new MessageDao();

            writer.write("HELLO " + user.userID + " " + user.userName);
            writer.newLine();
            writer.flush();

            System.out.println("Sent HELLO to server: " + user.userName + " (id " + user.userID + ")");

        } catch (IOException e) {
            closeClient(socket, reader, writer);
        }
    }


    public User getUser() {
        return user;
    }

    public FriendRequestDao getFriendRequestDao() {
        return friendRequestDao;
    }

    public UserDao getUserDao() {
        return userDao;
    }

    public MessageDao getMessageDao() {
        return messageDao;
    }




//sending message in CLI : main difference is GUI does not run off loop
    public void sendMessage() {
        try {
            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                System.out.println(">");
                String message = scanner.nextLine();
                writer.write(this.user.userName + ": " + message);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            closeClient(socket, reader, writer);
        }
    }
//    TODO: update message sender to use have extra information to payload for performence

    public void sendToUser(User user, String message) {

        try {


            int seq = seqGen.incrementAndGet();
            long ts = System.nanoTime();

            String chatPayload = "ID <" + user.userID + "> :" + user.userName + ": " + message;
            String payload = "MSG|" + seq + "|" + ts + "|" + chatPayload;


            writer.write(payload);
            writer.newLine();
            writer.flush();

            pendingRtt.put(seq, ts);
            stats.recordSend(payload.length() + System.lineSeparator().length());


        } catch (IOException e) {
            // Handle gracefully (server disconnected, etc.)
//            close();
        }

    }

    public void listenForMessage(MessageListener listener ) {
        Thread.startVirtualThread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("VOICE_INFO")) {
                        listener.onMessageReceived(line);
                    } else {
                        // Regular incoming messages

                        stats.recordReceive(line.length() + System.lineSeparator().length());

                        String[] parts = line.split("\\|", 4);
                        String type = parts[0];


                        if ("MSG".equals(type)) {
                            // parts: 0=MSG, 1=seq, 2=sendTs, 3=chatPayload
                            int seq = Integer.parseInt(parts[1]);
                            long sendTs = Long.parseLong(parts[2]);
                            String chatPayload = parts[3];

//                            JAVAFX display
                            listener.onMessageReceived(chatPayload);

                            // (Optional) end-to-end client↔client RTT:
                            // sendAck(seq);  // this ACK will go back through the server

                        } else if ("ACK".equals(type)) {
//                            ACK FROM SERVER
                            int seq = Integer.parseInt(parts[1]);
                            handleAck(seq);

                        } else {
                            // fall back: maybe old format lines, or server system messages
                            listener.onMessageReceived(line);
                        }

//                        listener.onMessageReceived(line);
                    }
                }
            } catch (IOException e) {
                // log if you want
            } finally {
                closeClient(socket, reader, writer);
            }
        });

    }

//    TODO CONNECT TO JAVAFX
    private void handleAck(int seq) {
        Long sendTs = pendingRtt.remove(seq);
        if (sendTs == null) return;

//        MOST RECENT RTT
        long now = System.nanoTime();
        long rtt = now - sendTs;
        stats.recordRtt(rtt);

        double lastRttMs   = rtt / 1_000_000.0;
        double avgRttMs    = stats.avgRttMillis();

        double throughput  = stats.throughputMbps(startTimeMillis);

//        Change in the performance stats, notification to the listener is sent out
        if (statsListener != null) {
            statsListener.onStatsUpdated(lastRttMs, avgRttMs, throughput);
        }
    }


    public void closeClient(Socket socket, BufferedReader in, BufferedWriter out) {
        // ensure we stop any running voice call
        stopVoiceCall();

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


    public List<FriendRequest> getFriendRequests() throws SQLException {
        return friendRequestDao.getIncomingPending(user.userID);
    }

    public void addFriendship(long userId, long friendId) throws SQLException {
        System.out.println("adding friend "+ userId + " with " + friendId);
        userDao.addFriendship(userId,friendId);
    }


    public void updateFriendsList() throws SQLException {
        user.friends = new ArrayList<>(userDao.getFriends(user.userID));
    }

    public ArrayList<User> getFriendList(){
        return user.friends;
    }

    public void removeFriendship(long userId, long friendId) throws SQLException {
        // this del friendship both directions
        userDao.removeFriendship(userId, friendId);

        // refresh current user's friends list
        user.friends = new ArrayList<>(userDao.getFriends(user.userID));
    }


// === Voice chat helpers ===

    private static AudioFormat getAudioFormat() {
        float sampleRate = 16000.0f;
        int sampleSizeInBits = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void startVoiceCall(User friend) {
        if (voiceThreadsRunning) {
            System.out.println("A voice call is already running. Stop it first.");
            return;
        }

        // Notify server that we want to start voice with this friend
        try {
            writer.write("VOICE_START " + friend.userID);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.out.println("Could not notify server about voice call start: " + e.getMessage());
            return;
        }

        System.out.println("Starting voice call with " + friend.userName +
                ". Have them also start a voice call with you.");

        voiceThreadsRunning = true;

        try {
            voiceSocket = new DatagramSocket();
            InetAddress serverAddr = socket.getInetAddress(); // same host as TCP server

            AudioFormat format = getAudioFormat();

            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);

            if (!AudioSystem.isLineSupported(micInfo)) {
                System.out.println("Microphone line not supported on this system.");
                voiceThreadsRunning = false;
                return;
            }
            if (!AudioSystem.isLineSupported(speakerInfo)) {
                System.out.println("Speaker line not supported on this system.");
                voiceThreadsRunning = false;
                return;
            }

            TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
            microphone.open(format);
            microphone.start();

            SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
            speakers.open(format);
            speakers.start();

            // Capture + send thread
            voiceCaptureThread = new Thread(() -> {
                byte[] buffer = new byte[AUDIO_BUFFER_SIZE];
                try {
                    while (voiceThreadsRunning && !Thread.currentThread().isInterrupted()) {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            ByteBuffer bb = ByteBuffer.allocate(8 + bytesRead);
                            bb.putLong(user.userID);           // prepend our user id
                            bb.put(buffer, 0, bytesRead);      // audio data

                            byte[] sendData = bb.array();
                            DatagramPacket packet = new DatagramPacket(
                                    sendData,
                                    sendData.length,
                                    serverAddr,
                                    VOICE_UDP_PORT
                            );
                            voiceSocket.send(packet);
                        }
                    }
                } catch (IOException e) {
                    if (voiceThreadsRunning) {
                        System.out.println("Voice capture error: " + e.getMessage());
                    }
                } finally {
                    microphone.stop();
                    microphone.close();
                }
            }, "VoiceCaptureThread");

            // Receive + play thread
            voiceReceiveThread = new Thread(() -> {
                byte[] recvBuf = new byte[AUDIO_BUFFER_SIZE];
                try {
                    while (voiceThreadsRunning && !Thread.currentThread().isInterrupted()) {
                        DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
                        voiceSocket.receive(packet);
                        // Server sends raw audio bytes (no userId header)
                        speakers.write(packet.getData(), 0, packet.getLength());
                    }
                } catch (IOException e) {
                    if (voiceThreadsRunning) {
                        System.out.println("Voice receive error: " + e.getMessage());
                    }
                } finally {
                    speakers.drain();
                    speakers.stop();
                    speakers.close();
                }
            }, "VoiceReceiveThread");

            voiceCaptureThread.start();
            voiceReceiveThread.start();

        } catch (LineUnavailableException | IOException e) {
            System.out.println("Could not start voice call: " + e.getMessage());
            voiceThreadsRunning = false;
            if (voiceSocket != null && !voiceSocket.isClosed()) {
                voiceSocket.close();
            }
        }
    }

    public void stopVoiceCall() {
        if (!voiceThreadsRunning) {
            return;
        }

        // tell server we are stopping
        try {
            writer.write("VOICE_STOP");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            System.out.println("Error notifying server to stop voice: " + e.getMessage());
        }

        System.out.println("Stopping voice call...");

        voiceThreadsRunning = false;

        if (voiceCaptureThread != null) {
            voiceCaptureThread.interrupt();
        }
        if (voiceReceiveThread != null) {
            voiceReceiveThread.interrupt();
        }

        if (voiceSocket != null && !voiceSocket.isClosed()) {
            voiceSocket.close();
        }
    }

//======================================Stats Static Class ======================================

    private final AtomicInteger seqGen = new AtomicInteger(0);
    private final Map<Integer, Long> pendingRtt = new ConcurrentHashMap<>();

    private final Stats stats = new Stats();
    private final long startTimeMillis = System.currentTimeMillis();

    public void setStatsListener(StatsListener listener) {
        this.statsListener = listener;
    }

    static class Stats {
        long totalSentBytes = 0;
        long totalReceivedBytes = 0;

        long totalRttNanos = 0;
        long rttSamples = 0;

        synchronized void recordSend(int bytes) {
            totalSentBytes += bytes;
        }

        synchronized void recordReceive(int bytes) {
            totalReceivedBytes += bytes;
            System.out.println("TOTAL RECEIVED BYTES: " + totalReceivedBytes);
        }

        synchronized void recordRtt(long rttNanos) {
            totalRttNanos += rttNanos;
            rttSamples++;
        }

        synchronized double avgRttMillis() {
            if (rttSamples == 0) return 0.0;
            return (totalRttNanos / (double) rttSamples) / 1_000_000.0;
        }

        synchronized double throughputMbps(long startTimeMillis) {
            long now = System.currentTimeMillis();
            double seconds = (now - startTimeMillis) / 1000.0;
            if (seconds <= 0) return 0.0;
            double bits = totalReceivedBytes * 8.0;
            return bits/seconds;
//            System.out.println("throughput bits: " + bits);
//            return (bits / seconds) / 1_000_000.0;
        }
    }


}

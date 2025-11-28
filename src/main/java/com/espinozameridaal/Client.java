package com.espinozameridaal;

import java.io.*;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.nio.ByteBuffer;

import javax.sound.sampled.*;

import com.espinozameridaal.Models.User;
import com.espinozameridaal.Database.UserDao;
import com.espinozameridaal.Models.FriendRequest;
import com.espinozameridaal.Models.Message;
import com.espinozameridaal.Database.FriendRequestDao;
import com.espinozameridaal.Database.MessageDao;

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

    public void sendToUser(User user, String message) {

        try {
            String payload = "ID <" + user.userID + "> :" + user.userName + ": " + message;
            writer.write(payload);
            writer.newLine();
            writer.flush();

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
                        listener.onMessageReceived(line);
                    }
                }
            } catch (IOException e) {
                // log if you want
            } finally {
                closeClient(socket, reader, writer);
            }
        });

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
        return friendRequestDao.getIncomingPending(user.getUserID());
    }

    public void addFriendship(long userId, long friendId) throws SQLException {
        System.out.println("adding friend "+ userId + " with " + friendId);
        userDao.addFriendship(userId,friendId);
    }


    public void updateFriendsList() throws SQLException {
        user.friends =  new ArrayList<>(userDao.getFriends(user.getUserID()));
    }

    public ArrayList<User> getFriendList(){
        return user.friends;
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

    private void startVoiceCall(User friend) {
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

    private void stopVoiceCall() {
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

    // === Existing stuff ===

//    --------------------------------OLD CLI VERSION--------------------------------------
    public void displayMenu() {
        System.out.println("Menu");
        System.out.println("1. Show Friends");
        System.out.println("2. Friend Requests");
        System.out.println("3. Add Friend");
        System.out.println("4. Message Friend");
        System.out.println("5. Settings");
        System.out.println("6. Start Voice Call");
        System.out.println("7. Stop Voice Call");
        System.out.println("0. Exit");
    }



    // RUNs off main thread; builds off the send message function
    public void mainMenu() {

        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("CONNECTION ESTABLISHED");
            while (socket.isConnected()) {
                displayMenu();
                System.out.print("Enter choice: ");
                String choiceLine = scanner.nextLine().trim();
                if (choiceLine.isBlank()) {
                    System.out.println("Invalid choice! Please enter a number between 0 and 7.");
                    continue;
                }

                int choice;
                try {
                    choice = Integer.parseInt(choiceLine);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid choice! Please enter a number between 0 and 7.");
                    continue;
                }

                if (choice < 0 || choice > 7) {
                    System.out.println("Invalid choice! Please enter a number between 0 and 7.");
                    continue;
                }

                switch (choice) {
                    case 1 -> {
                        try {
                            user.friends = new ArrayList<>(userDao.getFriends(user.userID));
                        } catch (SQLException e) {
                            System.out.println("Error loading friends.");
                            e.printStackTrace();
                            break;
                        }

                        System.out.println("--------------------------------");
                        System.out.println(user.userName + "'s friends:");
                        if (user.friends.isEmpty()) {
                            System.out.println("(no friends yet)");
                        } else {
                            for (User f : user.friends) {
                                System.out.println(" - " + f.userName + " (id " + f.userID + ")");
                            }
                        }
                        System.out.println("--------------------------------");
                    }

                    case 2 -> {
                        // Friend Requests: view and accept/decline
                        System.out.println("Friend Requests");
                        System.out.println("Pending friend requests:");
                        java.util.List<FriendRequest> pending;
                        try {
                            pending = friendRequestDao.getIncomingPending(user.userID);
                        } catch (SQLException e) {
                            System.out.println("Error loading friend requests.");
                            e.printStackTrace();
                            break;
                        }

                        if (pending.isEmpty()) {
                            System.out.println("No pending friend requests.");
                            break;
                        }

                        for (FriendRequest fr : pending) {
                            System.out.println("From userId=" + fr.getSenderId() + " (requestId=" + fr.getId() + ")");
                        }

                        System.out.println("Enter 'a <senderUserId>' to accept, 'd <senderUserId>' to decline, or 'b' to go back:");
                        String cmd = scanner.nextLine().trim();
                        if (cmd.equalsIgnoreCase("b")) {
                            break;
                        }

                        if (cmd.startsWith("a ")) {
                            String idPart = cmd.substring(2).trim();
                            try {
                                long senderUserId = Long.parseLong(idPart);

                                FriendRequest target = null;
                                for (FriendRequest fr : pending) {
                                    if (fr.getSenderId() == senderUserId) {
                                        target = fr;
                                        break;
                                    }
                                }

                                if (target == null) {
                                    System.out.println("Request not found.");
                                    break;
                                }

                                long reqId = target.getId();

                                if (friendRequestDao.accept(reqId)) {

                                    userDao.addFriendship(user.userID, target.getSenderId());
                                    user.friends = new ArrayList<>(userDao.getFriends(user.userID));
                                    System.out.println("Friend request accepted.");
                                } else {
                                    System.out.println("Could not accept request.");
                                }
                            } catch (Exception e) {
                                System.out.println("Invalid sender user id or DB error.");
                            }
                        } else if (cmd.startsWith("d ")) {
                            String idPart = cmd.substring(2).trim();
                            try {
                                long senderUserId = Long.parseLong(idPart);
                                FriendRequest target = null;

                                for (FriendRequest fr : pending) {
                                    if (fr.getSenderId() == senderUserId) {
                                        target = fr;
                                        break;
                                    }
                                }

                                if (target == null) {
                                    System.out.println("Request not found.");
                                    break;
                                }

                                long reqId = target.getId();

                                if (friendRequestDao.decline(reqId)) {
                                    System.out.println("Friend request declined.");
                                } else {
                                    System.out.println("Could not decline request.");
                                }
                            } catch (Exception e) {
                                System.out.println("Invalid sender user id or DB error.");
                            }
                        } else {
                            System.out.println("Unknown command.");
                        }
                    }

                    case 3 -> {
                        // Add Friend (sends friend request)
                        System.out.println("Add Friend");
                        System.out.print("Enter friend's username: ");
                        String friendName = scanner.nextLine().trim();
                        if (friendName.isBlank()) {
                            System.out.println("Friend name cannot be empty.");
                            break;
                        }
                        if (friendName.equals(user.userName)) {
                            System.out.println("You cannot add yourself as a friend.");
                            break;
                        }
                        try {
                            User friend = userDao.findByUsername(friendName);
                            if (friend == null) {
                                System.out.println("User does not exist. Cannot send request.");
                                break;
                            }

                            boolean ok = friendRequestDao.createRequest(user.userID, friend.userID);
                            if (ok) {
                                System.out.println("Friend request sent to " + friend.userName + ".");
                            } else {
                                System.out.println("A pending request already exists or cannot send request.");
                            }
                        } catch (SQLException e) {
                            System.out.println("Failed to send friend request.");
                            e.printStackTrace();
                        }
                    }

                    case 4 -> {
                        // Message Friend (show history + chat)
                        System.out.println("Which friend do you want to message?");
                        System.out.println("Enter their ID (enter -1 to go back): ");
                        String idLine = scanner.nextLine().trim();
                        int userID;
                        try {
                            userID = Integer.parseInt(idLine);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid ID. Returning to menu.");
                            break;
                        }
                        if (userID == -1) {
                            System.out.println("Returning to menu.");
                            break;
                        }
                        User found = User.getUserById(userID, this.user.friends);
                        if (found == null) {
                            System.out.println("User not found in your friends list.");
                            break;
                        }

                        // Show message history
                        try {
                            java.util.List<Message> history = messageDao.getConversation(user.userID, found.userID);
                            System.out.println("----- Conversation with " + found.userName + " -----");
                            if (history.isEmpty()) {
                                System.out.println("No previous messages.");
                            } else {
                                for (Message m : history) {
                                    String who = (m.senderId == user.userID) ? "You" : found.userName;
                                    System.out.println("[" + m.createdAt + "] " + who + ": " + m.content);
                                }
                            }
                            System.out.println("--------------------------------------");
                        } catch (SQLException e) {
                            System.out.println("Could not load message history.");
                            e.printStackTrace();
                        }

                        String message = "";
                        while (!Objects.equals(message, "-1")) {
                            System.out.println("Sending messages to (" + found.userName + "): ");
                            message = scanner.nextLine();
                            if (message.isBlank()) continue;
                            if (Objects.equals(message, "-1")) break;

                            writer.write("ID <" + found.userID + "> :" + found.userName + ": " + message);
                            writer.newLine();
                            writer.flush();
                        }
                    }

                    case 5 -> {
                        // Settings (optional)
                        System.out.println("Settings");
                        System.out.println("(-- Future Features --)");
                    }

                    case 6 -> {
                        // Start Voice Call
                        System.out.println("Which friend do you want to start a voice call with?");
                        System.out.print("Enter their ID (enter -1 to go back): ");
                        String idLine = scanner.nextLine().trim();
                        int userID;
                        try {
                            userID = Integer.parseInt(idLine);
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid ID. Returning to menu.");
                            break;
                        }
                        if (userID == -1) {
                            System.out.println("Returning to menu.");
                            break;
                        }
                        User found = User.getUserById(userID, this.user.friends);
                        if (found == null) {
                            System.out.println("User not found in your friends list.");
                            break;
                        }
                        startVoiceCall(found);
                    }

                    case 7 -> {
                        // Stop Voice Call
                        stopVoiceCall();
                    }

                    case 0 -> {
                        System.out.println("Exit");
                        stopVoiceCall();
                        System.exit(0);
                    }

                    default -> System.out.println("Invalid choice.");
                }

            }
        } catch (IOException e) {
            closeClient(socket, reader, writer);
        }
    }
//    --------------------------------OLD CLI VERSION--------------------------------------






//    public static void main(String[] args) {
//
//        int port = 1234;
//        Scanner scanner = new Scanner(System.in);
//        UserDao userDao = new UserDao();          // DB instead of alice and rest of them list
//        User currentUser;
//
//        System.out.println("Enter username you’d like to use: ");
//        String username = scanner.nextLine().trim();
//
//        try {
//            // find or create user in H2 database
//            currentUser = userDao.findOrCreateByUsername(username);
//
//            // loads existing friends from DB into the in memory list
//            currentUser.friends = new ArrayList<>(userDao.getFriends(currentUser.userID));
//
//            System.out.println("Found / created user: " + currentUser.userName +
//                    " (id " + currentUser.userID + ")");
//        } catch (SQLException e) {
//            System.out.println("Failed to connect to database. Exiting.");
//            e.printStackTrace();
//            return;
//        }
//
//        try {
//            Socket socket = new Socket("localhost", port);
//            Client client = new Client(socket, currentUser, userDao);
//
//            client.listenForMessage();
//            client.mainMenu();
//        } catch (IOException e) {
//            System.out.println("Could not connect to server on port " + port);
//            e.printStackTrace();
//        }
//    }

}

package com.espinozameridaal;


import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
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

    public void sendMessage() {
        try {

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                System.out.println(">");
                String message = scanner.nextLine();
                writer.write(this.user.userName +": "+ message);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            closeClient(socket, reader, writer);
        }
    }

    public void displayMenu(){
        System.out.println("Menu");
        System.out.println("1. Show Friends");
        System.out.println("2. Friend Requests");
        System.out.println("3. Add Friend");
        System.out.println("4. Message Friend");
        System.out.println("5. Settings");
        System.out.println("0. Exit");
    }

//    RUNs off main thread; builds off the send message function
    public void mainMenu(){

        try {
            Scanner scanner = new Scanner(System.in);
            System.out.println("CONNECTION ESTABLISHED");
            while (socket.isConnected()) {
//                assumption is that you successfully logged in etc

                displayMenu();
                System.out.print("Enter choice: ");
                String choiceLine = scanner.nextLine().trim();
                if (choiceLine.isBlank()) {
                    System.out.println("Invalid choice! Please enter a number between 0 and 5.");
                    continue;
                }

                int choice;
                try {
                    choice = Integer.parseInt(choiceLine);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid choice! Please enter a number between 0 and 5.");
                    continue;
                }

                if (choice < 0 || choice > 5) {
                    System.out.println("Invalid choice! Please enter a number between 0 and 5.");
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

                    case 0 -> {
                        System.out.println("Exit");
                        System.exit(0);
                    }

                    default -> System.out.println("Invalid choice.");
                }

            }
        } catch (IOException e) {
            closeClient(socket, reader, writer);
        }


    }



    public void listenForMessage() {
        Thread.startVirtualThread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    // System.out.println("NEW MESSAGE:");
                    // System.out.println(line);
                }
            } catch (IOException e) {
                // log if you want
            } finally {
                closeClient(socket, reader, writer);
            }
        });
    }

    public void closeClient(Socket socket, BufferedReader in, BufferedWriter out){
        try {
            if(in != null){
                in.close();
            }
            if(out != null){
                out.close();
            }
            if(socket != null){
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//     public static void main(String[] args) throws IOException {


//         ArrayList<User> allUsers = new ArrayList<>();

//         User alice = new User(1, "Alice", "pass123");
//         User bob = new User(2, "Bob", "secure456");
//         User carol = new User(3, "Carol", "pw789");

//         // Make them all friends with one another
//         alice.addFriend(bob);
//         alice.addFriend(carol);

//         bob.addFriend(alice);
//         bob.addFriend(carol);

//         carol.addFriend(alice);
//         carol.addFriend(bob);

//         allUsers.add(alice);
//         allUsers.add(carol);
//         allUsers.add(bob);

//         int port = 1234;
//         Scanner scanner = new Scanner(System.in);
// //        System.out.println("Please enter the server port you wish to listen on:  ");
// //        int port = Integer.parseInt(scanner.nextLine());
// //

//         System.out.println("Enter user you'd like to use : ");
//         int userID = scanner.nextInt();

//         User found = User.getUserById(userID, allUsers);
//         if (found != null) {
//             System.out.println("Found user: " + found.userName);
//         } else {
//             System.out.println("User not found.");
//             System.exit(0);
//         }


//         Socket socket = new Socket("localhost", port);
//         Client client = new Client(socket, found);


//         client.listenForMessage();
//         client.mainMenu();

//     }

    public static void main(String[] args) {

        int port = 1234;
        Scanner scanner = new Scanner(System.in);
        UserDao userDao = new UserDao();          // DB instead of alice and rest of them list
        User currentUser;

        System.out.println("Enter username you’d like to use: ");
        String username = scanner.nextLine().trim();

        try {
            // fnd or create user in H2 database
            currentUser = userDao.findOrCreateByUsername(username);

            // loads existing friends from DB into the in memory list
            currentUser.friends = new ArrayList<>(userDao.getFriends(currentUser.userID));

            System.out.println("Found / created user: " + currentUser.userName +
                               " (id " + currentUser.userID + ")");
        } catch (SQLException e) {
            System.out.println("Failed to connect to database. Exiting.");
            e.printStackTrace();
            return;
        }

        try {
            Socket socket = new Socket("localhost", port);
            Client client = new Client(socket, currentUser, userDao);

            client.listenForMessage();
            client.mainMenu();
        } catch (IOException e) {
            System.out.println("Could not connect to server on port " + port);
            e.printStackTrace();
        }
    }
    
}

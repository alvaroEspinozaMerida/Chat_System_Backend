package com.espinozameridaal;


import java.io.*;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import com.espinozameridaal.Models.User;

import com.espinozameridaal.Database.UserDao;

public class Client {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private User user;
    private UserDao userDao;


    public Client(Socket socket, User user, UserDao userDao) {
        try {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.user = user;
            this.userDao = userDao;

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
        System.out.println("1. Show friends");
        System.out.println("2. Add Friend");
        System.out.println("3. Message Friend:");
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
                    System.out.println("Invalid choice! Please enter a number between 0 and 3.");
                    continue;
                }

                int choice;
                try {
                    choice = Integer.parseInt(choiceLine);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid choice! Please enter a number between 0 and 3.");
                    continue;
                }

                if (choice < 0 || choice > 3) {
                    System.out.println("Invalid choice! Please enter a number between 0 and 3.");
                    continue;
                }

                switch (choice){
                    case 1:
                        System.out.println("--------------------------------");
                        this.user.showFriends();
                        break;
                    case 2:
                        System.out.println("Add Friend");
                        // clears newline from last nextInt
                        // scanner.nextLine(); 
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
                            // check if friend exists in DB
                            User friend = userDao.findByUsername(friendName);
                            if (friend == null) {
                                System.out.println("User does not exist. Cannot add friend.");
                                break;
                            }

                            if (friend.userID == user.userID) {
                                System.out.println("You cannot add yourself.");
                                break;
                            }

                            // create friendship in DB
                            userDao.addFriendship(user.userID, friend.userID);

                            // refresh memory friends list
                            user.friends = new ArrayList<>(userDao.getFriends(user.userID));
                            System.out.println("Added friend: " + friend.userName);
                        } catch (SQLException e) {
                            System.out.println("Failed to add friend.");
                            e.printStackTrace();
                        }
                        break;
                    case 3:
//                        System.out.println("Message Friend:");
                        System.out.println("Which friend do you want to message ? ");
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
                        if (found != null) {
                            System.out.println("Found user: " + found.userName);
                        } else {
                            System.out.println("User not found.");
                        }
                        String message = "" ;
                        while (found != null && !Objects.equals(message, "-1")) {
                            System.out.println("Sending messages to (" + found.userName + "): " );
                            message = scanner.nextLine();

                            if (message.isBlank()) continue;  // skip empties
                            if (Objects.equals(message, "-1")) break;

                            System.out.println("SENDING TEXT:"+ message);
//                            this should contain the ID and user of the person you are messaging
                            writer.write("ID <"+found.userID+"> :"+ found.userName+ ": " + message);
                            writer.newLine();
                            writer.flush();
                        }
                        break;
                    case 0:
                        System.out.println("Exit");
                        System.exit(0);
                        break;
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
                    System.out.println("NEW MESSAGE:");
                    System.out.println(line);
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

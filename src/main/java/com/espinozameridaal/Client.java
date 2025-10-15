package com.espinozameridaal;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Scanner;
import com.espinozameridaal.User;

public class Client {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private User user;


    public Client(Socket socket, User user) {
        try {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.user = user;


            writer.write("HELLO " + user.userID + " " + user.userName);
            writer.newLine();
            writer.flush();

            System.out.println("Sent HELLO to server: " + user.userName + " (id " + user.userID + ")");

        }catch (IOException e) {
            closeClient(socket, reader, writer);
        }

    }

    public void sendMessage() {
        try {

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                System.out.println(">");
                String message = scanner.nextLine();
                writer.write(this.user.getUserName() +": "+ message);
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
                int choice = scanner.nextInt();
                while(choice < 0 || choice > 3  ){
                    System.out.println("Invalid choice!");
                    System.out.println("Enter again");
                    choice = scanner.nextInt();
                }

                switch (choice){
                    case 1:
                        System.out.println("--------------------------------");
                        this.user.showFriends();
                        break;
                    case 2:
                        System.out.println("Add Friend");
                        break;
                    case 3:
//                        System.out.println("Message Friend:");
                        System.out.println("Which friend do you want to message ? ");
                        System.out.println("Enter their ID(enter -1 to go back): ");
                        int userID = scanner.nextInt();
//                        clean buffer
                        scanner.nextLine();

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

    public static void main(String[] args) throws IOException {


        ArrayList<User> allUsers = new ArrayList<>();

        User alice = new User(1, "Alice", "pass123");
        User bob = new User(2, "Bob", "secure456");
        User carol = new User(3, "Carol", "pw789");

        // Make them all friends with one another
        alice.addFriend(bob);
        alice.addFriend(carol);

        bob.addFriend(alice);
        bob.addFriend(carol);

        carol.addFriend(alice);
        carol.addFriend(bob);

        allUsers.add(alice);
        allUsers.add(carol);
        allUsers.add(bob);

        int port = 1234;
        Scanner scanner = new Scanner(System.in);
//        System.out.println("Please enter the server port you wish to listen on:  ");
//        int port = Integer.parseInt(scanner.nextLine());
//

        System.out.println("Enter user you'd like to use : ");
        int userID = scanner.nextInt();

        User found = User.getUserById(userID, allUsers);
        if (found != null) {
            System.out.println("Found user: " + found.userName);
        } else {
            System.out.println("User not found.");
            System.exit(0);
        }


        Socket socket = new Socket("localhost", port);
        Client client = new Client(socket, found);


        client.listenForMessage();
        client.mainMenu();

    }



}

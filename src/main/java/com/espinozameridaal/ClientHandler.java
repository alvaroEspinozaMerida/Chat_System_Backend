package com.espinozameridaal;

import com.espinozameridaal.Models.MessageParser;
import com.espinozameridaal.Models.ParsedMessage;
import com.espinozameridaal.Database.MessageDao;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;
import java.sql.SQLException;

// Handles a single connected client -- reads lines, routes messages, and stores them in the DB
public class ClientHandler  implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();

    private static final MessageDao messageDao = new MessageDao();

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    String clientUsername;
    long clientUserId;

    public ClientHandler(Socket socket) {
        try{
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // after clientHandlers.add(this);



            String hello = reader.readLine();
            if (hello == null || !hello.startsWith("HELLO ")) {
                throw new IOException("Client did not send identity.");
            }

            String[] parts = hello.split("\\s+", 3);
            this.clientUserId = Long.parseLong(parts[1]);
            this.clientUsername = (parts.length >= 3) ? parts[2] : ("user-" + clientUserId);

            clientHandlers.add(this);
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
                broadcastMessage(message);
            }
        } catch (IOException e) {
            // client disconnects or error while reading
        } finally {
            closeClientHandler(socket, reader, writer);
        }
    }

    private void closeClientHandler(Socket socket, BufferedReader in, BufferedWriter out) {
        removeClientHandler( this);
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


        for(ClientHandler clientHandler : clientHandlers){
            try{
                if(!clientHandler.clientUsername.equals(this.clientUsername) && Objects.equals(parsed.userName, clientHandler.clientUsername)){
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
            }catch (IOException e){
                closeClientHandler(socket, reader, writer);
            }
        }
    }

    private void sendWelcomeMessage(String message) {
        try {
            writer.write(message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            closeClientHandler(socket, reader, writer);
        }
    }



    public void removeClientHandler(ClientHandler clientHandler){
        clientHandlers.remove(clientHandler);
        String msg = "ChatServer.Server: " + clientHandler.clientUsername + " has left the chat !";
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
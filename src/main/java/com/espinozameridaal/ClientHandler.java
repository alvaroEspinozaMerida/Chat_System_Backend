package com.espinozameridaal;

import com.espinozameridaal.Models.MessageParser;
import com.espinozameridaal.Models.ParsedMessage;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Objects;

public class ClientHandler  implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String clientUsername;
    private long clientUserId;

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
            this.clientUsername = parts.length >= 3 ? parts[2] : ("user-" + clientUserId);

            clientHandlers.add(this);
            // Optional: send a welcome
            writer.write("Welcome " + clientUsername + " (id " + clientUserId + ")");
            writer.newLine();
            writer.flush();


        } catch (IOException e) {
            closeClientHandler(socket, reader, writer);
        }
    }

    @Override
    public void run() {
        String message;

        while (socket.isConnected()) {
            try{
                message = reader.readLine();
                broadcastMessage(message);
            }catch (IOException e){
                closeClientHandler(socket, reader, writer);
                break;
            }

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

        ParsedMessage parsed = MessageParser.parse(message);
        System.out.println("SENDING");
        System.out.println(parsed.userName);
        System.out.println(parsed.message);


        for(ClientHandler clientHandler : clientHandlers){
            try{
                if(!clientHandler.clientUsername.equals(this.clientUsername) && Objects.equals(parsed.userName, clientHandler.clientUsername)){
                    clientHandler.writer.write(parsed.message);
                    clientHandler.writer.newLine();
                    clientHandler.writer.flush();
                }
            }catch (IOException e){
                closeClientHandler(socket, reader, writer);
            }
        }
    }

    private void sendWelcomeMessage(String message) {
        for(ClientHandler clientHandler : clientHandlers){
            try{
                if(clientHandler.clientUsername.equals(this.clientUsername)){
                    clientHandler.writer.write(message);
                    clientHandler.writer.newLine();
                    clientHandler.writer.flush();
                }
            }catch (IOException e){
                closeClientHandler(socket, reader, writer);
            }
        }
    }



    public void removeClientHandler(ClientHandler clientHandler){
        clientHandlers.remove(clientHandler);
        broadcastMessage("ChatServer.Server: "+clientHandler.clientUsername+" has left the chat !");
    }


}
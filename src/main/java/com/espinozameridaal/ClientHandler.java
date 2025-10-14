package com.espinozameridaal;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler  implements Runnable {
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        try{
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.clientUsername = reader.readLine();
            clientHandlers.add(this);
            sendWelcomeMessage("Welcome: "+clientUsername);
            broadcastMessage("ChatServer.Server: "+clientUsername+" has entered the chat !");

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
        for(ClientHandler clientHandler : clientHandlers){
            try{
                if(!clientHandler.clientUsername.equals(this.clientUsername)){
                    clientHandler.writer.write(message);
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
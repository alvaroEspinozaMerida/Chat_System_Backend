package com.espinozameridaal.examples.ChatServer;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

import java.sql.SQLException;
import com.espinozameridaal.Database.UserDao;
import com.espinozameridaal.Models.User;


public class ClientHandlerV1 implements Runnable {
    public static ArrayList<ClientHandlerV1> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String clientUsername;

    private static UserDao userDao = new UserDao();
    private User user;

    public ClientHandlerV1(Socket socket) {
        try{
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            this.clientUsername = reader.readLine();
            
            try {
                this.user = userDao.findOrCreateByUsername(this.clientUsername);
                System.out.println("User connected: " + user.userName + " (id " + user.userID + ")");
            } catch (SQLException e) {
                e.printStackTrace();
                closeClientHandler(socket, reader, writer);
                return;
            }

            clientHandlers.add(this);
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
        for(ClientHandlerV1 clientHandler : clientHandlers){
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

    public void removeClientHandler(ClientHandlerV1 clientHandler){
        clientHandlers.remove(clientHandler);
        broadcastMessage("ChatServer.Server: "+clientHandler.clientUsername+" has left the chat !");
    }


}

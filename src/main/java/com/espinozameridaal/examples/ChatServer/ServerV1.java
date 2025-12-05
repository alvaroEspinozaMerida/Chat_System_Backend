package com.espinozameridaal.examples.ChatServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;

import com.espinozameridaal.Database.Database;

public class ServerV1 {
    private ServerSocket serverSocket;

    public ServerV1(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public void start()  {
        System.out.println("ChatServer.Server is starting...");
        try {
            while (!serverSocket.isClosed()) {

                Socket socket = serverSocket.accept();
                System.out.println(socket.getInetAddress().getHostAddress() + " has connected!");
//                location of where when new client gets created and gets linked to clienthandler
                ClientHandlerV1 clientHandler = new ClientHandlerV1(socket);

                Thread thread = new Thread(clientHandler);
                thread.start();

            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void closeServerSocket() {
        try {
            if(serverSocket != null) {
                serverSocket.close();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // create DB file/tables
            Database.init();

            // start the chat server
            ServerSocket serverSocket = new ServerSocket(1234);
            ServerV1 server = new ServerV1(serverSocket);
            server.start();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }


}

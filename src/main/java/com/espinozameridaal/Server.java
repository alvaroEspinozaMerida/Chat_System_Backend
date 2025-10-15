package com.espinozameridaal;

import com.espinozameridaal.ChatServer.ServerV1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;

public class Server {
    private ServerSocket serverSocket;

    public Server(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }


    /*
    * start: Represents the server running and accepting connections from
    * different clients on the server
    * creates a new ClientHandler for the client connected
    * key difference here from the tutorial followed is that this version uses
    * the virtual threads to help program run more efficiently
    *
    * TODO:
    *   - update this to connect to DB for user authentication and login
    *   -
    * */
    public void start(){
        System.out.println("Server has started on port: " + serverSocket.getLocalPort());

            try(var executor = Executors.newVirtualThreadPerTaskExecutor()){
                while (true) {

                    Socket clientSocket =  this.serverSocket.accept();
                    //                        TODO: Use this to share information to user
                    var clientIP = clientSocket.getInetAddress().getHostAddress();
                    var clientPort = clientSocket.getPort();
                    System.out.println("Accepted connection from " + clientSocket.getInetAddress().getHostName() + ":" + clientIP);



                    executor.submit(() -> {

                        ClientHandler clientHandler = new ClientHandler(clientSocket);
                        clientHandler.run();

                    });
                }
            }catch (IOException ioe) {
                ioe.printStackTrace();
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

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(1234);
        Server server = new Server(serverSocket);
        server.start();
    }


}

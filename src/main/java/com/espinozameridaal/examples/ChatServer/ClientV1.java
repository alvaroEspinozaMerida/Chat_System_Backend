package com.espinozameridaal.examples.ChatServer;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ClientV1 {

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private String userName;


    public ClientV1(Socket socket, String userName){
        try {
            this.socket = socket;
            this.writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.userName = userName;
        }catch (IOException e) {
            closeClient(socket, reader, writer);
        }

    }
    public void sendMessage() {
        try {
            writer.write(userName);
            writer.newLine();
            writer.flush();

            Scanner scanner = new Scanner(System.in);
            while (socket.isConnected()) {
                String message = scanner.nextLine();
                writer.write(userName +": "+ message);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
           closeClient(socket, reader, writer);
        }
    }
    public void listenForMessage(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                String messageFromServer;
                while(socket.isConnected()){
                    try {
                        messageFromServer = reader.readLine();
                        System.out.println(messageFromServer);
                    }
                    catch (IOException e) {
                        closeClient(socket, reader, writer);
                    }
                }

            }
        }).start();
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
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter your username: ");
        String userName = scanner.nextLine();

        Socket socket = new Socket("localhost", 1234);
        ClientV1 client = new ClientV1(socket, userName);
        client.listenForMessage();
        client.sendMessage();

    }



}

package com.espinozameridaal.examples.VirtualThreads;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.concurrent.Executors;


public class MyServer {
    public void start(final int portNumber) {
        try (var serverSocket = new ServerSocket(portNumber)){

            try(var executor = Executors.newVirtualThreadPerTaskExecutor()){

                while (true) {
                    var client =  serverSocket.accept();
                    executor.submit(() -> {
                        var clientIP = client.getInetAddress().getHostAddress();
                        var clientPort = client.getPort();
                        System.out.println("Accepted connection from " + client.getInetAddress().getHostName());

                        try (var clientInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
                             var output = new PrintWriter(client.getOutputStream(), true)) {

                            System.out.println("Socket Created !");
                            for (String inputLine; (inputLine = clientInput.readLine()) != null; ) {
                                System.out.println(clientIP + ":" + clientPort + ": " + inputLine);
                                output.println(new StringBuilder(inputLine).reverse().toString());
                            }

                        } catch (IOException ioe) {
                            ioe.printStackTrace();
                        }


                    });
                }
            }
        }catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}

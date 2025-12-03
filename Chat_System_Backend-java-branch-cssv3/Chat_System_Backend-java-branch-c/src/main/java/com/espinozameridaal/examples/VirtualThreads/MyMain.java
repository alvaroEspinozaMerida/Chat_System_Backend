package com.espinozameridaal.examples.VirtualThreads;

import java.util.Scanner;

public class MyMain {
    private static final int PORT_NUMBER = 12345;

    public static void main(String[] args) {

        try(var scanner = new Scanner(System.in)) {
            System.out.println("Is this a server(y/n)");
            if(scanner.nextLine().equals("y")) {
                new MyServer().start(PORT_NUMBER);
            }
            else{
                new MyClient().start(PORT_NUMBER, scanner);
            }

        }

    }
}

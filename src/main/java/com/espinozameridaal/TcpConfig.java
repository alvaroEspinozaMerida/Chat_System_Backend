package com.espinozameridaal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class TcpConfig {

    @Bean
    public TcpChatClient tcpChatClient() throws IOException {

        return new TcpChatClient("localhost", 1234);
    }
}
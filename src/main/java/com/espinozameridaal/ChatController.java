package com.espinozameridaal;

import com.espinozameridaal.Models.ChatMessageDto;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "http://localhost:5173") // React dev origin
public class ChatController {

    private final TcpChatClient tcpClient;
    private final List<String> messages =
            Collections.synchronizedList(new ArrayList<>());

    public ChatController(TcpChatClient tcpClient) {
        this.tcpClient = tcpClient;

        // Start background listener from TCP and append to messages
//        represents messaages coming from other users
        this.tcpClient.startListener(msg -> {
            System.out.println("Gateway received from ChatCore: " + msg);
            messages.add(msg);
        });
    }

    // DTO for incoming messages
    public static class ChatMessageDto {
        public String text;
    }

    @PostMapping("/send")
    public void send(@RequestBody ChatMessageDto dto) {
        // When React calls this, we forward the message over TCP
        tcpClient.sendMessage(dto.text);
    }

    @GetMapping("/messages")
    public List<String> getMessages() {
        // For demo simplicity, return everything
        synchronized (messages) {
            return new ArrayList<>(messages);
        }
    }
}
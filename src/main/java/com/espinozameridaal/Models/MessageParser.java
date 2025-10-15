package com.espinozameridaal.Models;

import java.util.regex.*;

public class MessageParser {

    // Example input: "ID <1>: Alice: Hello world"
    public static ParsedMessage parse(String rawMessage) {
        // Regex groups:
        // 1 -> ID number, 2 -> username, 3 -> message
        Pattern pattern = Pattern.compile("^ID <(\\d+)> ?: ?([^:]+): ?(.+)$");
        Matcher matcher = pattern.matcher(rawMessage);

        if (matcher.matches()) {
            long id = Long.parseLong(matcher.group(1).trim());
            String name = matcher.group(2).trim();
            String msg = matcher.group(3).trim();
            return new ParsedMessage(id, name, msg);
        } else {
            System.err.println("⚠️ Could not parse message: " + rawMessage);
            return null;
        }
    }
}

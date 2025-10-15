package com.espinozameridaal.Models;

import java.util.regex.*;

public class MessageParser {

    public static ParsedMessage parse(String rawMessage) {
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

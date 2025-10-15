package com.espinozameridaal;

import lombok.*;

import java.util.ArrayList;


@Getter
@Setter
@ToString
//@AllArgsConstructor
@NoArgsConstructor

public class User {
    long userID;
    String userName;
    String userPassword;
//    Friends that you can chat with
    ArrayList<User> friends;

    public User(long UserID, String UserName, String UserPassword) {
        this.userID = UserID;
        this.userName = UserName;
        this.userPassword = UserPassword;
        this.friends = new ArrayList<>();
    }

    public void addFriend(User user) {
        friends.add(user);
    }

    public void showFriends() {
        System.out.println(userName + "'s friends:");
        for (User friend : friends) {
            System.out.println(" - " + friend.userName);
        }
    }


    public static User getUserById(long id, ArrayList<User> users) {
        for (User u : users) {
            if (u.userID == id) {
                return u;
            }
        }
        return null;
    }
}

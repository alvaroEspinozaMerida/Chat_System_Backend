package com.espinozameridaal.ui;

import com.espinozameridaal.Client;
import com.espinozameridaal.Models.FriendRequest;
import com.espinozameridaal.Models.Message;
import com.espinozameridaal.Models.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MainMenuController {

//    LEFT AREA
    @FXML
    private Label currentUserLabel;
    @FXML
    private ComboBox<User> friendComboBox; // or custom User type
    @FXML
    private Label friendRequestStatus;
    @FXML
    private ListView<FriendRequest> pendingRequestsList;
    @FXML
    private TextField addFriendField;

    private ObservableList<FriendRequest> pendingRequests;


//    Chat Area
    @FXML
    private Label currentChat;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField messageField;


//  Friend who you currently chatting with
    private User currentFriend;
//    Connection to Client , Client represents Socket Connections to Server ; each GUI get's one Client
    private Client client;

    /**
     *  Initalizes the MainMenuController
     *
     * @param client created when Program is run , represents the
     *               keeps track of socket connection to server for specific user,
     *               each user has DAOs for managing their connection to DB,
     *               for User, FriendRequest, Messages
     * Initializes the MainMenu view with information for specific user's data
     *               and also starts the client listenForMessage thread
     *
     */

    public void init(Client client) {
        this.client = client;
        currentUserLabel.setText(client.getUser().userName);

        createComboBoxView();
        createFriendRequestView();

        client.listenForMessage(line ->
                Platform.runLater(() -> chatArea.appendText(line + "\n"))
        );

    }


//    LEFT AREA FUNCTIONS AND WIDGET INITIALIZATION
    /**
     *  Used for createComboBox widget to get filled with data and
     *  keep track of events for when specific friend gets selected
     *
     *  ComboBox is primary widget used to handle currently selected friends
     */
    public void createComboBoxView() {

        List<User> friends = client.getUser().friends;

        if (friends != null && !friends.isEmpty()) {

            friendComboBox.setItems(FXCollections.observableArrayList(friends));

//            fill in drop down menu
            friendComboBox.setCellFactory(listView -> new ListCell<>() {
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    setText(empty || user == null ? null : user.userName);
                }
            });
//            set button to specific user that gets selcted by user, their is only one button
            friendComboBox.setButtonCell(new ListCell<>() {
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    setText(empty || user == null ? null : user.userName);
                }
            });
//            action event listener that sets the currentChat label to selected user
//            and loads in conversation calling helper function
            friendComboBox.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldFriend, newFriend) -> {
                        if (newFriend != null) {
                            currentChat.setText(newFriend == null ? null : "Current Chatting with:  "+ newFriend.userName);
                            loadConversation(newFriend);
                        }
                    }
            );
            friendComboBox.getSelectionModel().selectFirst();

        } else {
            friendComboBox.setItems(FXCollections.observableArrayList());
            chatArea.appendText("You have no friends yet. Add some from the menu.\n");
        }
    }



    /**
     * uses client to get pending friend requests to user
     * keeps track of button widget actions for accepting and declining request
     * DURING DEV SEE DOCS FOR COMMANDS FOR ERASING DB of friends and previous request!
     */
    public void createFriendRequestView(){
        List<FriendRequest> fromDb = List.of();
        try {
//            TODO REMOVE REDUNDENT CALL functions streamline
//            fromDb = client.getFriendRequestDao().getIncomingPending(client.getUser().userID);
            fromDb = client.getFriendRequests();
        }
        catch (Exception e) {
            System.out.println("Error loading friend requests.");

        }

        pendingRequests = FXCollections.observableArrayList(fromDb);
        pendingRequestsList.setItems(FXCollections.observableArrayList(pendingRequests));

        pendingRequestsList.setCellFactory(listView -> new ListCell<>() {
//            widgets within each cell of the friendRequestView
            private final HBox root = new HBox(8);
            private final Label fromLabel = new Label();
            private final Button acceptButton = new Button("Accept");
            private final Button declineButton = new Button("Decline");
            {
                root.getChildren().addAll(fromLabel, acceptButton, declineButton);
                acceptButton.setOnAction(e -> {
                    FriendRequest fr = getItem();
                    if (fr != null) {
                        handleAccept(fr);
                    }
                });
                declineButton.setOnAction(e -> {
                    FriendRequest fr = getItem();
                    if (fr != null) {
                        handleDecline(fr);
                    }
                });
            }

            @Override
            protected void updateItem(FriendRequest item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    fromLabel.setText("From: " + item.getSenderId());
                    setGraphic(root);
                }
            }

        });
    }


    private void handleAccept(FriendRequest fr) {
        try {
            if( client.getFriendRequestDao().accept(fr.getId()) ){

                pendingRequests.remove(fr);
                client.addFriendship(client.getUser().userID, fr.getSenderId());
                client.updateFriendsList();

//                friendComboBox.setItems(FXCollections.observableArrayList(client.getFriendList()));
                createComboBoxView();

            }
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void handleDecline(FriendRequest fr) {
        try {
            pendingRequests.remove(fr);
            client.getFriendRequestDao().decline(fr.getId());
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }





    public void refreshPendingRequests() {
        List<FriendRequest> fromDb =
                null;
        try {
            fromDb = client.getFriendRequestDao().getIncomingPending(client.getUser().userID);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        pendingRequests.setAll(fromDb);
    }

    @FXML
    public void onRefreshRequest() {
        refreshPendingRequests();
    }


    @FXML
    public void onSendFriendRequest() {
        String name = addFriendField.getText().trim();

        if (name.isEmpty()) {
            friendRequestStatus.setText("Please enter a username.");
            return;
        }
        try {
            User friend = client.getUserDao().findByUsername(name);
            System.out.println(friend);

            boolean sent = client.getFriendRequestDao().createRequest(
                    client.getUser().userID,
                    friend.userID
            );

            if (sent) {
                friendRequestStatus.setText("Friend request sent.");
                addFriendField.clear();
            } else {
                friendRequestStatus.setText("Unable to send request.");
            }
        } catch (Exception e) {
            friendRequestStatus.setText("User not found.");
        }

    }




//    Chat Area

    /**
     *  Loads Specific User's Conversation into the Chat Area Widget after selected by combo box
     * @param friend represents selected friend from combo box selection
     * After friend gets selected by the user in the combobox this function gets
     *               called to load in message history into the chatarea widget ;
     *               buiilt off of Mauro's original chat history function from CLI program
     */
    private void loadConversation(User friend) {
        chatArea.clear();
        currentFriend = friend;

        try {
            List<Message> history = client.getMessageDao()
                    .getConversation(client.getUser().userID, friend.userID);
            if (history.isEmpty()) {
                chatArea.appendText("No previous messages.\n");
            } else {
                for (Message m : history) {
                    String who = (m.senderId == client.getUser().userID)
                            ? "You"
                            : friend.userName;
                    chatArea.appendText("[" + m.createdAt + "] " + who + ": " + m.content + "\n");
                }
            }
            chatArea.appendText("--------------------------------------\n");
        } catch (SQLException e) {
            chatArea.appendText("Could not load message history.\n");
            e.printStackTrace();
        }
    }


    /**
     *  When user clicks on the Send Message button widget, this function gets called
     *  Specific Friend to send message to is determined by the FriendComboBox
     *  currently selected value ; text is determined by the messageField widget getText()
     *  function
     *
     *  message is sent to server through the use of the client sentToUser function
     */
    @FXML
    private void onSendMessage() {
        String text = messageField.getText().trim();
        User friend = friendComboBox.getValue();
        if (text.isEmpty() || currentFriend == null) {
            return;
        }
        messageField.clear();
        client.sendToUser(friend, text);
        chatArea.appendText("[now] You: " + text + "\n");
    }


    @FXML
    private void onRefreshFriends() {


    }




}

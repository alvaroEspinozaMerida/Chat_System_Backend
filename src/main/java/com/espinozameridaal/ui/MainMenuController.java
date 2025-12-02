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
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainMenuController {

//    LEFT AREA
    @FXML
    private Label currentUserLabel;
    @FXML
    private ListView<User> friendListView;
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


//    Performence Metrics Data
    @FXML
    private Label rttLabel;
    @FXML
    private Label avgRttLabel;


//  Friend who you currently chatting with
    private User currentFriend;
//    Connection to Client , Client represents Socket Connections to Server ; each GUI get's one Client
    private Client client;

    private ScheduledExecutorService autoRefreshScheduler;

    private String vcStatus = "";

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

        createFriendsView();
        createFriendRequestView();

        client.listenForMessage(line ->
                Platform.runLater(() -> chatArea.appendText(line + "\n"))
        );

        startAutoRefresh();

    }


//    LEFT AREA FUNCTIONS AND WIDGET INITIALIZATION
    /**
     *  Used for createComboBox widget to get filled with data and
     *  keep track of events for when specific friend gets selected
     *
     *  ComboBox is primary widget used to handle currently selected friends
     */
    public void createFriendsView() {
        List<User> friends = client.getUser().friends;

        // Always use a nice renderer for User objects
        friendListView.setCellFactory(listView -> new ListCell<>() {

            {
                getStyleClass().add("list-cell");
            }


            private final ContextMenu contextMenu;
            {
                // new call item
                MenuItem callItem = new MenuItem("Call friend");
                callItem.setOnAction(e -> {
                    User friend = getItem();
                    if (friend != null) {
                        client.startVoiceCall(friend);
                        vcStatus = " - In call with: " +friend.userName;
                        setCallActive(true);
                    }
                });

                MenuItem removeItem = new MenuItem("Remove friend");
                removeItem.setOnAction(e -> {
                    User friend = getItem();
                    if (friend != null) {
                        confirmAndRemoveFriend(friend);
                    }
                });

                contextMenu = new ContextMenu(callItem, removeItem);
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setContextMenu(null);
                } else {
                    setText(user.userName + " (id " + user.userID + ")");
                    setContextMenu(contextMenu);
                }
            }
        });

        if (friends != null && !friends.isEmpty()) {
            ObservableList<User> items = FXCollections.observableArrayList(friends);
            friendListView.setItems(items);

            friendListView.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldFriend, newFriend) -> {
                        if (newFriend != null) {
                            currentFriend = newFriend;
                            currentChat.setText("Current chatting with: " + newFriend.userName + vcStatus);
                            loadConversation(newFriend);
                        }
                    }
            );

            friendListView.getSelectionModel().selectFirst();

        } else {
            friendListView.setItems(FXCollections.observableArrayList());
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
        pendingRequestsList.setItems(pendingRequests);

        pendingRequestsList.setCellFactory(listView -> new ListCell<>() {
//            widgets within each cell of the friendRequestView
            private final VBox root = new VBox(8);
            private final Label fromLabel = new Label();

            private final HBox buttonRow = new HBox(8);
            private final Button acceptButton = new Button("Accept");
            private final Button declineButton = new Button("Decline");

            {
                acceptButton.getStyleClass().add("friend-btn");
                declineButton.getStyleClass().add("friend-btn");

                buttonRow.getChildren().addAll(acceptButton, declineButton);
                root.getChildren().addAll(fromLabel,buttonRow);

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

                createFriendsView();

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

    private void startAutoRefresh() {
        autoRefreshScheduler = Executors.newSingleThreadScheduledExecutor();
        autoRefreshScheduler.scheduleAtFixedRate(() -> {
            try {
                client.updateFriendsList();
                List<User> friendsSnapshot = new ArrayList<>(client.getUser().friends);
                List<FriendRequest> reqSnapshot =
                        client.getFriendRequestDao().getIncomingPending(client.getUser().userID);
                Platform.runLater(() -> {
                    // previously selected friend
                    User selected = friendListView.getSelectionModel().getSelectedItem();
                    Long selectedId = (selected != null) ? selected.userID : null;

                    ObservableList<User> items = friendListView.getItems();
                    if (items == null) {
                        items = FXCollections.observableArrayList();
                    }
                    items.setAll(friendsSnapshot);
                    friendListView.setItems(items);

                    // Restore selection
                    if (selectedId != null) {
                        for (User u : items) {
                            if (u.userID == selectedId) {
                                friendListView.getSelectionModel().select(u);
                                currentFriend = u;
                                break;
                            }
                        }
                    }

                    if (pendingRequests == null) {
                        pendingRequests = FXCollections.observableArrayList();
                    }
                    pendingRequests.setAll(reqSnapshot);
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 2, 2, TimeUnit.SECONDS);
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
        if (text.isEmpty()) {
            return;
        }

        // remembr currentFriend
        User friend = currentFriend;
        if (friend == null) {
            friend = friendListView.getSelectionModel().getSelectedItem();
        }
        if (friend == null) {
            return;
        }

        messageField.clear();
        client.sendToUser(friend, text);
        chatArea.appendText("[now] You: " + text + "\n");




    }


    @FXML
    private void onRefreshFriends() {
        try {
            client.updateFriendsList();
            createFriendsView();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void confirmAndRemoveFriend(User friend) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Remove friend");
        alert.setHeaderText(null);
        alert.setContentText("Remove " + friend.userName + " from your friends?");

        alert.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                try {
                    client.removeFriendship(client.getUser().userID, friend.userID);
                    client.getMessageDao().deleteConversation(client.getUser().userID, friend.userID);
                    client.updateFriendsList();
                    createFriendsView();
                    chatArea.clear();
                    currentChat.setText("Current chatting with: " + vcStatus);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    private void onRemoveFriend() {
        User friend = friendListView.getSelectionModel().getSelectedItem();
        if (friend == null) {
            return;
        }
        confirmAndRemoveFriend(friend);
    }

    // vc buttons
    private boolean callActive = false;

    @FXML
    private Button leaveCallButton;

    public void setCallActive(boolean active) {
        this.callActive = active;
        leaveCallButton.setVisible(active);
        leaveCallButton.setManaged(active);
    }

    @FXML
    private void onLeaveCall() {
        // whatever you do to terminate the call:
        // e.g., client.leaveCall(), stopAudio(), notifyServer(), etc.

        // then update UI state
        vcStatus = "";
        client.stopVoiceCall();
        setCallActive(false);
    }
}

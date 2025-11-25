# Project 1 
## Tutorial Followed 

### Chat System with Project Structure 

#### ChatServer.Server.java - 
- Represents the server from where messages from where client sockets connect to the ServerSocket 
- Without the server running messages cannot be transmitted between users 
- Program runs on single thread so that other processes(clients) can also run concurrently 
- Creates a single ChatServer.ClientHandler Object 
- Constructor is composed of single object that takes in as argument a single ServerSocket object 


#### ChatServer.Client.java - 
- Main role is read and write data from server to client and client to server
  client.listenForMessage();   // separate thread
  client.sendMessage();        // main thread


#### Variables:

- Socket: A socket is an endpoint for communication between two machines.  Each Socket object that has been created
  using with java.net.Socket class has been associated exactly with 1 remote host,
  for connecting to another different host, we must create a new socket object
- Reader: in charge of reading in data from the user from the CLI
- Writer: in charge of transmitting data sent from other users through the usee of the clientHandlers arraylist
- ClientUserName : user name for clients that have established connection with server

#### Functions:
sendMessage - reads messages from the CLI scanner and sends them over to the writer to write to server 
listenForMessage - runs on thread so that mulitple process can run off the same machine ie listen and send 
if the socket is connected then read messages from the server and transmit to the user 
- closeClient: closes client cleanly 


#### ChatServer.ClientHandler.java -
- Main role is to establish a connection with the server and then transmit messages from single client to other clients

#### Variables: 
- Composed of 4 instance variables and 1 static variable : 
- Static Variable: ArrayList represents a list of ClientHanndlerss; this is a very important variable 
as it is used for transmitting messages between clients can be transmitted
- Socket: A socket is an endpoint for communication between two machines.  Each Socket object that has been created
using with java.net.Socket class has been associated exactly with 1 remote host, 
for connecting to another different host, we must create a new socket object

- Reader: in charge of reading in data from the user from the CLI 
- Writer: in charge of transmitting data sent from other users through the usee of the clientHandlers arraylist 
- ClientUserName : user name for clients that have established connection with server 

#### Functions: 

- run : while connection is established with server socket then read data from the CLI and broadcast 
- closeClientHanlder: closes the sockets, buffer readers and writers to make sure data is cleaned up nicely 
- boroadcaast: loop through the ChatServer.ClientHandler list and if the message sent in is not from the user 
then reference the other clients writers and write the message on to their CLI 
- removeClientHandler: removes a ChatServer.ClientHandler from the ClientHandlers list 

| Role                | Lives on                                                             | Purpose                                                                                                                                                              | Lifecycle                                                                                                             |
| ------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| **`ChatServer.Client`**        | 🖥️ **The user’s computer** (the machine connecting *to* the server) | Runs a program that **initiates a TCP connection** to the server using `Socket("localhost", 1234)`. It reads user input, sends messages, and prints what comes back. | Created by the user when they run the client program. One `ChatServer.Client` per user.                                          |
| **`ChatServer.ClientHandler`** | 🖥️ **The server machine**                                           | A helper object the **server** creates *after accepting* a connection. It’s responsible for talking to one specific client through that TCP socket.                  | Created by the server when `serverSocket.accept()` returns a new `Socket`. One `ChatServer.ClientHandler` per client connection. |



| Person            | Analogy                                      |
| ----------------- | -------------------------------------------- |
| **ChatServer.Client**        | A customer placing an order.                 |
| **ChatServer.Server**        | The shop itself, taking all orders.          |
| **ChatServer.ClientHandler** | The barista handling *one customer’s* order. |

### MULTITHREADING: 
Multithreading is needed any time you want your server to handle multiple clients at once — whether those clients are:

all running on one computer,

or 100 different computers across the network.

          ┌───────────────────┐
          │ ServerSocket(1234)│
          └───────┬───────────┘
                  │
         accept() │ (blocks until new connection)
                  ▼
        ┌──────────────────────┐
        │ new Socket() from OS │ ←── TCP connection from a new client
        └──────────┬───────────┘
                   │
                   ▼
        ┌────────────────────────────┐
        │ new ChatServer.ClientHandler(socket)  │ ←── creates streams, reads username,
        │ adds to clientHandlers[]   │     broadcasts join message
        └──────────┬─────────────────┘
                   │
                   ▼
        ┌────────────────────────────┐
        │ new Thread(clientHandler)  │ ←── server starts a dedicated thread
        └────────────────────────────┘


# Project 2

### Virtual Threads 


#### VirtualThreads.MyClient.java - represents the client 

#### Functions:
- start: starts the client thread; client is able to read from the CLI
and then send data over to the server and then write data sent from server on CLI 

#### VirtualThreads.MyServer.java - represents the server 

#### Functions:
- start: starts the server and each new client that connects gets it's own 
virtual thread, this new virtual thread is able to read in data sent 
in from client and then reverses the text back to the user 


IMORTANT CONCEPTS/ FUNCTIONS 

ServerSocket Class in Java provides a system-independent way to implement
the server side of a client/server socket connection. 
The constructor for ServerSocket throws an exception if it can’t listen 
on the specified port (for example, the port is already being used).


The java.net.Socket class allows us to create socket objects that help us in 
implementing all fundamental socket operations.
We can perform various networking operations such as sending,
reading data and closing connections.
Each Socket object that has been created using with java.net.Socket class has 
been associated exactly with 1 remote host, for connecting to another different host, 
we must create a new socket object.


Virtual Threads:
This executor leverages Java's virtual threads (Project Loom),
which are lightweight, user-mode threads managed by the JVM, significantly
reducing the overhead compared to traditional platform threads.

One Virtual Thread Per Task:
Unlike thread pools that reuse a fixed number of platform threads, 
newVirtualThreadPerTaskExecutor() creates a new virtual thread for every
Runnable or Callable submitted. This means there's no pooling of virtual threads.

High Throughput:
The primary benefit of this executor is enabling high throughput in applications
with many concurrent, often I/O-bound, tasks. Virtual threads efficiently handle 
blocking operations, allowing the underlying platform threads to be utilized by
other virtual threads.


---

## Database support (SQLite)

### Overview

- The chat system now uses a simple SQLite database stored in a file named `chat.db` in the project root.
- `Database.java` manages the JDBC connection using `jdbc:sqlite:chat.db`.
- `UserDao.java` manages users, friendships, and lookups.
- `Client.java` uses `UserDao` to load the current user and their friends from the database.
- `FriendRequestDao.java` — handles sending, accepting, rejecting, and querying friend requests.
- `MessageDao.java` — stores and loads private message history between users.

### Tables created
- `users`: stores user id, username, optional password, and created time.
- `friendships`: stores pairs of user ids that are friends.
- `friend_requests`: stores pending friend request information.
- `messages` — stores message history with:
  - sender id  
  - receiver id  
  - message text  
  - timestamp  

These tables are created automatically the first time the server runs, by calling `Database.init()` in `Server.javs`.

### How to run with database enabled
1. **Build the project**
   - From the project root:
     ```bash
     mvn clean compile
     ```

2. **Start the server (creates or opens chat.db)**
   - Run `Server.java`.
   - `Server.java` calls `Database.init()`, which creates `chat.db` and the tables if they do not exist

3. **Start a client**
   - Run `Client.java`
   - When asked, enter a username.
   - If the username already exists in `users`, it is loaded; otherwise, a new row is created.
   - The client menu still supports:
     - Register a new user if the username does not exist.
     - Load existing user data from the database.
     - Allow:
       - Viewing friends
       - Sending friend requests
       - Accepting friend requests
       - Messaging friends
       - Viewing stored message history (Using `MessageDao`)

### How to inspect the database
- The database file is `chat.db` in the project folder.
- You can open this file with any SQLite viewer, for example:
  - A SQLite extension in VS Code (e.g. "SQLite Viewer").
  - A standalone tool like DB Browser for SQLite.
- Typical queries for inspection:
  ```sql
  SELECT * FROM users;
  SELECT * FROM friendships;
  SELECT * FROM friend_requests;
  SELECT * FROM messages;
  ```
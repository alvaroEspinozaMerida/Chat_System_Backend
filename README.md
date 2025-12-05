# TEMJA – TCP / UDP Chat Application

TEMJA is a real-time chat application built with Java.  
It combines **TCP** for reliable messaging (Discord-style DM chat) and **UDP** for low-latency voice calls between friends. The UI is built with **JavaFX (FXML + CSS)**, and all persistent data is stored in a **SQLite** database.

---

## Features

- **User Accounts**
  - User login
  - Persistent user data in SQLite

- **Friends System**
  - Send / accept / reject friend requests
  - Add and remove friends
  - Friend list

- **Real-Time Messaging (TCP)**
  - Reliable, ordered messaging over TCP
  - Java server managing connected clients
  - Direct messaging between friends
  - Message history loaded from SQLite

- **Voice Calls (UDP)**
  - Voice calls between friends using UDP for lower latency.
  - Calls are established only when both users call each other
  - Server runs a voice relay loop on UDP port 50005, reading packets and forwarding audio frames between users

- **Metrics & Monitoring**
  - Real-time TCP messaging metrics panel:
    - Throughput
    - RTT of recent messages
  - Live graph of data

- **Persistence (SQLite)**
  - Users
  - Friends & friend requests
  - Message history (per conversation)

## Getting Started

### Prerequisites

Make sure you have the following installed:

- **Java 21** (JDK 21+)
- **Maven**
- **Git**

> The **JavaFX SDK is bundled through Maven**, so you don’t need to install JavaFX separately.

## Setup

### 1. Clone the Repository
### 2. Build the Project
- mvn clean install

### 3. Run the Server
- You must start the server before any client connects.
- java Server.java

### 4. Run the Client
- mvn javafx:run

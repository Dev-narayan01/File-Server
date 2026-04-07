# File Server

A multithreaded client-server file storage system written in Java, built as part of the [JetBrains Hyperskill](https://hyperskill.org/) Java Backend Developer track.

## Overview

The File Server allows clients to store, retrieve, and delete files on a remote server over a TCP socket connection. Files are persisted on disk and referenced by either their filename or a server-assigned integer ID. The server handles multiple simultaneous client connections using a thread pool.

## Features

- **TCP socket communication** between client and server
- **Concurrent client handling** via `ExecutorService` thread pool
- **ID-based file access** — server assigns a unique integer ID on `PUT`, which the client can use for `GET` and `DELETE`
- **Persistence across restarts** — the ID-to-filename map is serialized to disk using `ObjectOutputStream` / `ObjectInputStream`
- **Binary file transfer** support
- **Graceful shutdown** — server exits cleanly on client request

## Project Structure

```
File-Server/
├── src/
│   └── server/
│       ├── Main.java          # Server entry point
│       ├── Server.java        # Accepts connections, manages thread pool
│       ├── RequestHandler.java # Handles individual client sessions
│       └── Storage.java       # File I/O and ID map management
│   └── client/
│       ├── Main.java          # Client entry point
│       └── Client.java        # Sends requests to the server
└── README.md
```

## How It Works

### Server

The server listens on a configured port and accepts incoming client connections. Each connection is handed off to a `RequestHandler` running in a thread pool managed by `ExecutorService`.

```java
ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
pool.submit(() -> handleClient(socket));
```

The server maintains a `HashMap<Integer, String> idMap` that maps integer file IDs to their filenames on disk. This map is serialized to a file after every write operation so it survives server restarts.

### Protocol

Clients send plain-text commands over the socket:

| Command | Format | Description |
|---|---|---|
| PUT | `PUT <filename> <data>` | Store a file; server returns the assigned ID |
| GET | `GET <id\|filename>` | Retrieve a file by ID or name |
| DELETE | `DELETE <id\|filename>` | Delete a file by ID or name |
| EXIT | `EXIT` | Shut down the server |

### ID Map Persistence

On every `PUT`, the server updates `idMap` and serializes the entire map to disk:

```java
// Save
ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ID_MAP_PATH));
oos.writeObject(idMap);

// Load on startup
ObjectInputStream ois = new ObjectInputStream(new FileInputStream(ID_MAP_PATH));
idMap = (HashMap<Integer, String>) ois.readObject();
```

## Getting Started

### Prerequisites

- Java 11+
- Any IDE (IntelliJ IDEA recommended) or `javac`/`java` from the command line

### Running the Server

```bash
cd src
javac server/*.java
java server.Main
```

### Running the Client

```bash
cd src
javac client/*.java
java client.Main
```

## Concepts Covered

- Java Sockets (`ServerSocket`, `Socket`)
- Multithreading with `ExecutorService` and thread pools
- Java Serialization (`ObjectOutputStream` / `ObjectInputStream`)
- Binary file I/O with `DataInputStream` / `DataOutputStream`
- Synchronized access to shared state

## Stage Breakdown

| Stage | What was added |
|---|---|
| 1 | Basic single-client GET/PUT over sockets |
| 2 | File storage to disk |
| 3 | DELETE support; named file access |
| 4 | Multithreading, ID-based access, serialized persistence |

## Author

[Dev-narayan01](https://github.com/Dev-narayan01)

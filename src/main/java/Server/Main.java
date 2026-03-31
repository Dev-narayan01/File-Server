package Server;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.HashMap;
import java.util.Collections;
import java.io.*;
import java.net.*;

public class Main {
    // Map to store file ID -> filename mapping
    static HashMap<Integer, String> idMap = new HashMap<>();
    // Counter for generating unique IDs
    static Integer nextId = 1;
    // Server socket declared as static so handleClient can access it
    static ServerSocket serverSocket;

    private static void handleClient(Socket socket){
        try(DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream())){

            // Read the command from client (PUT, GET, DELETE, exit)
            String command = in.readUTF();

            if (command.equals("exit")) {
                // Close the server socket to stop the server
                serverSocket.close();
            }
            else if (command.equals("PUT")) {
                // Read the filename the client wants to save as
                String fileName = in.readUTF();
                // Read the number of bytes coming
                int length = in.readInt();
                // Create a byte array of that size
                byte[] filebytes = new byte[length];
                // Read exactly that many bytes
                in.readFully(filebytes, 0, length);

                // If client didn't specify a name, generate one
                if(fileName.isEmpty()){
                    fileName = "file_" + nextId;
                }

                File file = new File("server/data/" + fileName);

                if(file.exists()){
                    // File already exists, send forbidden
                    out.writeUTF("403");
                }
                else {
                    // Write the bytes to disk
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(filebytes);
                    }
                    // Register the file in our ID map
                    idMap.put(nextId, fileName);
                    // Send success response with the assigned ID
                    out.writeUTF("200 " + nextId);
                    nextId++;
                    // Persist the map to disk
                    saveIdMap();
                }
            }

            else if (command.equals("GET")) {
                // Read whether client is using BY_ID or BY_NAME
                String type = in.readUTF();
                // Read the actual ID or filename
                String key = in.readUTF();

                // Resolve the filename from the map
                String fileName = null;
                if(type.equals("BY_ID")){
                    fileName = idMap.get(Integer.parseInt(key));
                }
                else{
                    fileName = idMap.containsValue(key) ? key : null;
                }

                if(fileName == null){
                    // ID or name not found in map
                    out.writeUTF("404");
                }
                else{
                    File file = new File("server/data/" + fileName);
                    if(!file.exists()){
                        // File not found on disk
                        out.writeUTF("404");
                    }
                    else{
                        // Read file bytes and send to client
                        byte[] fileBytes = new FileInputStream(file).readAllBytes();
                        out.writeUTF("200");
                        out.writeInt(fileBytes.length);
                        out.write(fileBytes);
                    }
                }
            }

            else if (command.equals("DELETE")) {
                // Read whether client is using BY_ID or BY_NAME
                String type = in.readUTF();
                // Read the actual ID or filename
                String key = in.readUTF();

                // Resolve the filename from the map
                String fileName = null;
                if (type.equals("BY_ID")) {
                    fileName = idMap.get(Integer.parseInt(key));
                } else {
                    fileName = idMap.containsValue(key) ? key : null;
                }

                if (fileName == null) {
                    // ID or name not found in map
                    out.writeUTF("404");
                } else {
                    File file = new File("server/data/" + fileName);
                    if (file.delete()) {
                        // Remove from map and persist
                        idMap.values().remove(fileName);
                        saveIdMap();
                        out.writeUTF("200");
                    } else {
                        // File could not be deleted
                        out.writeUTF("404");
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    // Saves the idMap to disk using Java serialization
    private static void saveIdMap() {
        try (ObjectOutputStream oos = new ObjectOutputStream(
                new FileOutputStream("server/data/idMap.ser"))) {
            oos.writeObject(idMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        // Load idMap from disk if it exists
        File mapFile = new File("server/data/idMap.ser");
        if (mapFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(mapFile))) {
                idMap = (HashMap<Integer, String>) ois.readObject();
                // Resume nextId from where we left off
                nextId = Collections.max(idMap.keySet()) + 1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        serverSocket = new ServerSocket(23456);
        System.out.println("Server started!");
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        while (true) {
            Socket socket = serverSocket.accept();
            executorService.submit(() -> handleClient(socket));
        }
    }
}
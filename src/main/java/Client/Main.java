package Client;
import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter action (1 - get a file, 2 - save a file, 3 - delete a file): ");
        String action = scanner.nextLine();

        try (Socket socket = new Socket("127.0.0.1", 23456);
             DataInputStream in = new DataInputStream(socket.getInputStream());
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            if (action.equals("exit")) {
                // Send exit command to stop the server
                out.writeUTF("exit");
                System.out.println("The request was sent.");
            }

            else if (action.equals("2")) {
                // Ask for the local file to send
                System.out.print("Enter name of the file: ");
                String localFileName = scanner.nextLine();
                // Ask for the name to save it as on the server
                System.out.print("Enter name of the file to be saved on server: ");
                String serverFileName = scanner.nextLine();

                // Read the file bytes from client/data folder
                File file = new File("client/data/" + localFileName);
                byte[] fileBytes = new FileInputStream(file).readAllBytes();

                // Send PUT command, filename, and file bytes
                out.writeUTF("PUT");
                out.writeUTF(serverFileName);
                out.writeInt(fileBytes.length);
                out.write(fileBytes);
                System.out.println("The request was sent.");

                // Read response from server
                String response = in.readUTF();
                if (response.startsWith("200")) {
                    // Server sends back the assigned ID
                    String id = response.split(" ")[1];
                    System.out.println("Response says that file is saved! ID = " + id);
                } else {
                    System.out.println("Response says that creating the file was forbidden!");
                }
            }

            else if (action.equals("1")) {
                // Ask if user wants to use ID or name
                System.out.print("Do you want to get the file by name or by id (1 - name, 2 - id): ");
                String byType = scanner.nextLine();

                // Send GET command and access type
                out.writeUTF("GET");
                if (byType.equals("2")) {
                    out.writeUTF("BY_ID");
                    System.out.print("Enter id: ");
                } else {
                    out.writeUTF("BY_NAME");
                    System.out.print("Enter name: ");
                }

                // Send the actual ID or filename
                String key = scanner.nextLine();
                out.writeUTF(key);
                System.out.println("The request was sent.");

                // Read response from server
                String response = in.readUTF();
                if (response.equals("200")) {
                    // Read the file bytes from server
                    int length = in.readInt();
                    byte[] fileBytes = new byte[length];
                    in.readFully(fileBytes, 0, length);

                    // Ask where to save the file
                    System.out.print("The file was downloaded! Specify a name for it: ");
                    String saveName = scanner.nextLine();

                    // Save the file to client/data folder
                    try (FileOutputStream fos = new FileOutputStream("client/data/" + saveName)) {
                        fos.write(fileBytes);
                    }
                    System.out.println("File saved on the hard drive!");
                } else {
                    System.out.println("The response says that this file is not found!");
                }
            }

            else if (action.equals("3")) {
                // Ask if user wants to use ID or name
                System.out.print("Do you want to delete the file by name or by id (1 - name, 2 - id): ");
                String byType = scanner.nextLine();

                // Send DELETE command and access type
                out.writeUTF("DELETE");
                if (byType.equals("2")) {
                    out.writeUTF("BY_ID");
                    System.out.print("Enter id: ");
                } else {
                    out.writeUTF("BY_NAME");
                    System.out.print("Enter name: ");
                }

                // Send the actual ID or filename
                String key = scanner.nextLine();
                out.writeUTF(key);
                System.out.println("The request was sent.");

                // Read response from server
                String response = in.readUTF();
                if (response.equals("200")) {
                    System.out.println("The response says that this file was deleted successfully!");
                } else {
                    System.out.println("The response says that this file is not found!");
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
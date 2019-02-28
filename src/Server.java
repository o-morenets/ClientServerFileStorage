import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {

    private static final int PORT = 5555;
    private static final String ACCOUNT_FILE_NAME = "account.txt";
    static final int MAX_CONNECTIONS = 5;

    private static Map<String, User> accounts = new TreeMap<>();
    static int numConnections;

    public static void main(String[] args) {
        Server server = new Server();
        server.readAccounts();
        server.showAccounts();
        server.runServer();
    }

    private void readAccounts() {
        try (Scanner fileScan = new Scanner(new File(ACCOUNT_FILE_NAME))) {
            while (fileScan.hasNextLine()) {
                String line = fileScan.nextLine();
                try (Scanner lineScan = new Scanner(line)) {
                    String id = lineScan.next();
                    String password = lineScan.next();
                    List<String> userFileNames = new ArrayList<>();
                    while (lineScan.hasNext()) {
                        String userFileName = lineScan.next();
                        userFileNames.add(userFileName);
                    }
                    User user = new User(id, password, userFileNames);
                    accounts.put(id, user);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Could not read " + ACCOUNT_FILE_NAME);
        }
    }

    public static void writeAccounts() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ACCOUNT_FILE_NAME))) {
            for (User user : accounts.values()) {
                writer.write(user.getId());
                writer.write(" ");
                writer.write(user.getPassword());
                List<String> fileNames = user.getFileNames();
                for (String fileName : fileNames) {
                    writer.write(" ");
                    writer.write(fileName);
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAccounts() {
        for (User user : accounts.values()) {
            System.out.printf("%-10s %-10s", user.getId(), user.getPassword());
            List<String> userFileNames = user.getFileNames();
            for (String userFileName : userFileNames) {
                System.out.print(" " + userFileName);
            }
            System.out.println();
        }
    }

    private void runServer() {
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.out.println("Could not listen on port " + PORT);
            System.exit(0);
        }

        Socket clientSocket = null;
        while (true) {
            try {
                clientSocket = serverSocket.accept();
            } catch (IOException e) {
                System.out.println("Accept failed.");
                System.exit(1);
            }
            new ClientHandler(clientSocket, accounts);
            numConnections++;
        }
    }
}

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Scanner;

public class Client {

    private static final String ACK = "ACK";
    private static final String NACK = "NACK";
    private static final String ERROR = "ERROR";
    private static final String READY = "READY";

    private static final String CLIENT_FILES = "./client_files/";

    private Scanner kb = new Scanner(System.in);

    private BufferedWriter out;
    private BufferedReader in;
    private BufferedOutputStream fileOut;
    private BufferedInputStream fileIn;

    public static void main(String[] args) {
        new Client().runClient();
    }

    private void runClient() {
        System.out.print("Enter host name or IP address: ");
        String host = kb.nextLine();
        System.out.print("Enter port number: ");
        int port = kb.nextInt();
        kb.nextLine();

        InetAddress ina = null;
        try {
            ina = InetAddress.getByAddress(host, new byte[4]);
        } catch (UnknownHostException u) {
            System.out.print("Cannot find host name");
            System.exit(-1);
        }

        Socket s = null;
        try {
            s = new Socket(ina, port);
        } catch (IOException ex) {
            System.out.print("Cannot connect to host");
            System.exit(-2);
        }

        try {
            out = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(s.getInputStream()));
            fileOut = new BufferedOutputStream(s.getOutputStream());
            fileIn = new BufferedInputStream(s.getInputStream());

            // Part 2.1 Log-in Phase

            String line = in.readLine();
            System.out.println(line);

            if (line.equalsIgnoreCase(ERROR)) {
                System.out.println("Maximum connections reached.");
                System.exit(-3);
            }

            String option = kb.nextLine();
            switch (option) {
                case "1":
                    sendMessage("#1");
                    newUserQuery();
                    break;
                case "2":
                    sendMessage("#2");
                    existingUserQuery();
                    break;
                case "3":
                    sendMessage("#3");
                    return;
            }

            // Part 2.2 File Access Phase

            do {
                option = kb.nextLine();
                switch (option) {
                    case "1":
                        sendMessage("#1");
                        downloadFile();
                        break;
                    case "2":
                        sendMessage("#2");
                        uploadFile();
                        break;
                    case "3":
                        sendMessage("#3");
                        listFiles();
                        break;
                    case "4":
                        sendMessage("#4");
                        return;
                }

            } while (!option.equals("4"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String msg) throws IOException {
        out.write(msg);
        out.newLine();
        out.flush();
    }

    private void newUserQuery() throws IOException {
        String message;

        do {
            in.readLine(); // "NEWUSERQUERY"

            String id;
            String password;
            do {
                System.out.print("ID: ");
                id = kb.nextLine();
                System.out.print("Password: ");
                password = kb.nextLine();
            } while (!isValidId(id) || !isValidPassword(password));
            sendMessage(id + "*" + password);
            message = in.readLine();
        } while (message.equals(NACK));

        String[] tokens = message.split("\\*");
        if (tokens.length == 2) {
            if (tokens[0].equals(ACK)) {
                System.out.println(tokens[1]);
            }
        }
    }

    private boolean isValidId(String id) {
        return id.matches("[A-Za-z0-9]+");
    }

    private boolean isValidPassword(String password) {
        return password.matches("[A-Za-z0-9!#$%]+");
    }

    private void existingUserQuery() throws IOException {
        String message;

        do {
            in.readLine(); // "EXISTINGUSERQUERY"

            System.out.print("ID: ");
            String id = kb.nextLine();
            System.out.print("Password: ");
            String password = kb.nextLine();
            sendMessage(id + "*" + password);
            message = in.readLine();
        } while (message.equals(NACK));

        String[] tokens = message.split("\\*");
        if (tokens.length == 2) {
            System.out.println(tokens[1]);
            if (tokens[0].equals(NACK)) {
                System.exit(-3);
            }
        }
    }

    private void downloadFile() throws IOException {
        System.out.print("File name: ");
        String fileName = kb.nextLine();
        sendMessage(fileName);
        String message = in.readLine(); // "(N)ACK..."
        String[] tokens;
        if (message.equals(ACK)) {
            sendMessage(READY);
            receiveFile(new File(fileName));
            message = in.readLine(); // "ACK*1. Download, 2. Upload, 3. File List, 4. Disconnect"
        }

        tokens = message.split("\\*");
        if (tokens.length == 2) {
            System.out.println(tokens[1]);
        }
    }

    private void receiveFile(File file) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file))) {
            int length = in.read();
            for (int i = 0; i < length; i++) {
                int aByte = fileIn.read();
                bos.write(aByte);
            }
        }
    }

    private void uploadFile() throws IOException {
        in.readLine(); // "READY"
        System.out.print("File name: ");
        String fileName = kb.nextLine();
        sendMessage(fileName);
        in.readLine(); // "ACK"
        sendFile(fileName);
    }

    private void sendFile(String fileName) throws IOException {
        File file = new File(CLIENT_FILES + fileName);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            int length = (int) file.length();
            out.write(length);
            out.flush();
            int aByte;
            while ((aByte = bis.read()) != -1) {
                fileOut.write(aByte);
            }
            fileOut.flush();
        }

        String message = in.readLine(); // "ACK*1. Download, 2. Upload, 3. File List, 4. Disconnect"
        String[] tokens = message.split("\\*");
        if (tokens.length == 2) {
            System.out.println(tokens[1]);
        }
    }

    private void listFiles() throws IOException {
        String message;
        do {
            message = in.readLine();
            if (!message.contains("*")) {
                System.out.println(message);
            }
        } while (!message.contains("*"));

        String[] tokens = message.split("\\*");
        if (tokens.length == 2) {
            System.out.println(tokens[1]);
        }
    }
}

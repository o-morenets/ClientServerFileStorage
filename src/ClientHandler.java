import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class ClientHandler implements Runnable {

    private static final String ACK = "ACK";
    private static final String NACK = "NACK";
    private static final String ERROR = "ERROR";
    private static final String NEWUSERQUERY = "NEWUSERQUERY";
    private static final String EXISTINGUSERQUERY = "EXISTINGUSERQUERY";
    private static final String ACCESS_DENIED = "Access denied.";

    private static final String LOGIN_MENU = "1. New User, 2. Existing User, 3. Disconnect";
    private static final String FILE_ACCESS_MENU = "1. Download, 2. Upload, 3. File List, 4. Disconnect";

    private static final String SERVER_FILES = "./server_files/";
    private static final String CLIENT_FILES = "./client_files/";

    private Socket socket;

    private BufferedWriter out;
    private BufferedReader in;
    private BufferedOutputStream fileOut;
    private BufferedInputStream fileIn;

    private Map<String, User> accounts;
    private String currentId;

    public ClientHandler(Socket socket, Map<String, User> accounts) {
        this.socket = socket;
        this.accounts = accounts;
        Thread thread = new Thread(this);
        thread.start();
    }

    @Override
    public void run() {
        try {
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            fileOut = new BufferedOutputStream(socket.getOutputStream());
            fileIn = new BufferedInputStream(socket.getInputStream());

            if (Server.numConnections > Server.MAX_CONNECTIONS) {
                sendMessage(ERROR);
                terminateConnection();
                return;
            } else {
                sendMessage(LOGIN_MENU);
            }

            // Part 1.1 Log-in Phase

            String option = in.readLine();
            switch (option) {
                case "#1":
                    createNewUser();
                    break;
                case "#2":
                    loginExistingUser();
                    break;
                case "#3":
                    terminateConnection();
                    return;
            }

            // Part 1.2 File Access Phase

            do {
                option = in.readLine();
                switch (option) {
                    case "#1":
                        downloadFile();
                        break;
                    case "#2":
                        uploadFile();
                        break;
                    case "#3":
                        listFiles();
                        break;
                    case "#4":
                        terminateConnection();
                        return;
                }
            } while (!option.equals("4"));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNewUser() throws IOException {
        String id = null;
        String password = null;
        boolean userExists;

        do {
            sendMessage(NEWUSERQUERY);
            String idAndPassword = in.readLine();
            String[] tokens = idAndPassword.split("\\*");
            if (tokens.length == 2) {
                id = tokens[0];
                password = tokens[1];
            }
            userExists = accounts.containsKey(id);
            if (userExists) {
                sendMessage(NACK);
            }
        } while (userExists);

        accounts.put(id, new User(id, password));
        currentId = id;
        Server.writeAccounts();
        sendMessage(ACK + "*" + FILE_ACCESS_MENU);
    }

    private void loginExistingUser() throws IOException {
        final int MAX_TRIES = 3;
        int numTries = 0;

        String id = null;
        String password = null;
        boolean idAndPasswordCorrect = false;

        do {
            numTries++;
            sendMessage(EXISTINGUSERQUERY);
            String idAndPassword = in.readLine();
            String[] tokens = idAndPassword.split("\\*");
            if (tokens.length == 2) {
                id = tokens[0];
                password = tokens[1];
            }
            User user = accounts.get(id);
            if (user != null) {
                String accountPassword = user.getPassword();
                idAndPasswordCorrect = accountPassword.equals(password);
            }
            if (!idAndPasswordCorrect && numTries < MAX_TRIES) {
                sendMessage(NACK);
            }
        } while (!idAndPasswordCorrect && numTries < MAX_TRIES);

        if (numTries > MAX_TRIES) {
            sendMessage(NACK + "*" + ACCESS_DENIED);
            terminateConnection();
        } else {
            currentId = id;
            sendMessage(ACK + "*" + FILE_ACCESS_MENU);
        }
    }

    private void downloadFile() throws IOException {
        String fileName = in.readLine();
        File file = new File(SERVER_FILES + fileName);
        if (file.exists()) {
            sendMessage(ACK);
            String message = in.readLine(); // "READY"
            sendFile(fileName);
            sendMessage("ACK*1. Download, 2. Upload, 3. File List, 4. Disconnect");
        } else {
            sendMessage("NACK*1. Download, 2. Upload, 3. File List, 4. Disconnect");
        }
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
    }

    private void uploadFile() throws IOException {
        sendMessage("READY");
        String fileName = in.readLine();
        File file = new File(SERVER_FILES + fileName);
        if (file.exists()) {
            int counter = 1;
            File copyFile;
            do {
                copyFile = new File(SERVER_FILES + getFileName(file) + "_copy_" + counter++ + getFileExtension(file));
            } while (copyFile.exists());
            file = copyFile;
        }

        sendMessage(ACK);
        receiveFile(file);

        // update file list
        User user = accounts.get(currentId);
        user.getFileNames().add(file.getName());
        Server.writeAccounts();

        sendMessage("ACK*1. Download, 2. Upload, 3. File List, 4. Disconnect");
    }

    private String getFileName(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(0, lastIndexOf);
    }

    private String getFileExtension(File file) {
        String name = file.getName();
        int lastIndexOf = name.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return ""; // empty extension
        }
        return name.substring(lastIndexOf);
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

    private void listFiles() throws IOException {
        User user = accounts.get(currentId);
        List<String> fileNames = user.getFileNames();
        for (String fileName : fileNames) {
            sendMessage(fileName);
        }
        sendMessage("ACK*1. Download, 2. Upload, 3. File List, 4. Disconnect");
    }

    private void sendMessage(String msg) throws IOException {
        out.write(msg);
        out.newLine();
        out.flush();
    }

    private void terminateConnection() throws IOException {
        socket.close();
        Server.numConnections--;
    }
}

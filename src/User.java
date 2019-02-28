import java.util.ArrayList;
import java.util.List;

public class User {
    private String id;
    private String password;
    private List<String> fileNames;

    public User(String id, String password, List<String> fileNames) {
        this.id = id;
        this.password = password;
        this.fileNames = fileNames;
    }

    public User(String id, String password) {
        this(id, password, new ArrayList<>());
    }

    public String getId() {
        return id;
    }

    public String getPassword() {
        return password;
    }

    public List<String> getFileNames() {
        return fileNames;
    }
}

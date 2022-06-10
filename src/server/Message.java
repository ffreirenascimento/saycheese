package src.server;

import java.util.List;

public class Message {
    private String content;
    private String sender;
    private List<String> readBy;    

    public Message(String content, String sender, List<String> readBy) {
        this.content = content;
        this.sender = sender;
        this.readBy = readBy;
    }

    public String getContent() {
        return content;
    }

    public String getSender() {
        return sender;
    }

    public List<String> getReadBy() {
        return readBy;
    }

    /**
     * Removes user from list readBy
     * @param user
     */
    public void updateReadBy(String user) {
        readBy.remove(user);
    }

}

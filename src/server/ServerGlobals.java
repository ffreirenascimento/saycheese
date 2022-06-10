package src.server;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerGlobals {
    
    private int gpc;
    private Map<String, String> users;
    private Map<String, List<String>> user_photos;
    private Map<String, List<String>> photo_likes;
    private Map<String, List<String>> user_followers;
    private Map<String, List<String>> user_follows;
    // Stores the groups that some user is owner of.
    private Map<String, List<String>> user_owner;
    // Stores the groups that some user participates in.
    private Map<String, List<String>> user_participant;
    private Map<String, List<Message>> userInbox;
    private Map<String, List<Message>> groupHistory;
    
    public ServerGlobals() {
        gpc = 0;
        users = new HashMap<>();
        user_photos = new HashMap<>();
        photo_likes = new HashMap<>();
        user_followers = new HashMap<>();
        user_follows = new HashMap<>();
        user_owner = new HashMap<>();
        user_participant = new HashMap<>();
        userInbox = new HashMap<>();
        groupHistory = new HashMap<>();
    }
    
    public int getGpc() {
        return gpc;
    }
    
    public void setGpc(int gpc) {
        this.gpc = gpc;
    }
    
    public Map<String, String> getUsers() {
        return users;
    }

    public void setUsers(Map<String, String> users) {
        this.users = users;
    }

    public Map<String, List<String>> getUser_photos() {
        return user_photos;
    }

    public void setUser_photos(Map<String, List<String>> user_photos) {
        this.user_photos = user_photos;
    }
    
    public Map<String, List<String>> getPhoto_likes() {
        return photo_likes;
    }

    public void setPhoto_likes(Map<String, List<String>> photo_likes) {
        this.photo_likes = photo_likes;
    }

    public Map<String, List<String>> getUser_followers() {
        return user_followers;
    }

    public void setUser_followers(Map<String, List<String>> user_followers) {
        this.user_followers = user_followers;
    }

    public Map<String, List<String>> getUser_follows() {
        return user_follows;
    }
    
    public void setUser_follows(Map<String, List<String>> user_follows) {
        this.user_follows = user_follows;
    }

    public Map<String, List<String>> getUser_owner() {
        return user_owner;
    }
    
    public void setUser_owner(Map<String, List<String>> user_owner) {
        this.user_owner = user_owner;
    }

    public Map<String, List<String>> getUser_participant() {
        return user_participant;
    }
    
    public void setUser_participant(Map<String, List<String>> user_participant) {
        this.user_participant = user_participant;
    }
    
    public Map<String, List<Message>> getUserInbox() {
        return userInbox;
    }
    
    public void setUserInbox(Map<String, List<Message>> userInbox) {
        this.userInbox = userInbox;
    }
    
    public Map<String, List<Message>> getGroupHistory() {
        return groupHistory;
    }
    
    public void setGroupHistory(Map<String, List<Message>> groupHistory) {
        this.groupHistory = groupHistory;
    }

}

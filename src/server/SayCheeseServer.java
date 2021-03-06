package src.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.javatuples.Pair;
import org.javatuples.Tuple;

import com.google.gson.*;

public class SayCheeseServer {

    private File files_folder;
    private File server_folder;
    private File user_folder;
    private File users_file;
    private File groups_folder;
    private File global_counter_file;
    private ServerGlobals globals;

    public static void main(String[] args) {

        
        System.out.println("------Server Initialized------");
        SayCheeseServer server = new SayCheeseServer();
        if (args.length == 0) {
            System.err.println("Port not given.\nPlease provide a port as an argument");
            System.exit(-1);
        }
        server.startServer(args[0]);
    }
    
    /**
     * Opens communication channel,
     * creates necessary files and folders
     * if not previously created.
     * 
     * @param socket
     */
    public void startServer(String socket) {
        
        Gson gson = new Gson();

        globals = new ServerGlobals();

        File globals_file = new File("files/server/globals.json");
        if (!globals_file.exists()) {
            try {
                globals_file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        // In case of server shutdown, save global variables into a file.
        Thread shutdown = new Thread(() -> {
            try (FileWriter globals_fw = new FileWriter(globals_file)) {
                gson.toJson(globals, globals_fw);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("\nShuting down server...");
        });
        Runtime.getRuntime().addShutdownHook(shutdown);

        // Initiate socket for communication.
        ServerSocket ssoc = null;
        try {
            ssoc = new ServerSocket(Integer.parseInt(socket));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        // Initialize global variables with previous values from JSON file.
        // 1. Check if JSON file is empty.
        if (globals_file.length() != 0) {
            try (Reader globals_reader = new FileReader(globals_file)) {
                globals = gson.fromJson(globals_reader, ServerGlobals.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        // Create files and folders
        FileWriter global_counter_file_fw; 
        try {
            files_folder = new File("files");
            files_folder.mkdir();

            server_folder = new File("files/server");
            server_folder.mkdir();

            user_folder = new File("files/user");
            user_folder.mkdir();

            users_file = new File("files/server/users.txt");
            users_file.createNewFile();

            global_counter_file = new File("files/server/globalPhotoCounter.txt");
            if (!global_counter_file.exists()) {
                global_counter_file.createNewFile();
                global_counter_file_fw = new FileWriter(global_counter_file);
                global_counter_file_fw.write("0");
                global_counter_file_fw.close();
            }

            groups_folder = new File("files/groups");
            groups_folder.mkdir();
        } catch (IOException e) {
            System.err.println("Error while creating necessary files and folders");
            System.exit(-1);
        }

        System.out.println("-------Server files and folders created-------");

        while (true) {
            try {
                Socket in_soc = ssoc.accept();
                ServerThread new_server_thread = new ServerThread(in_soc);
                new_server_thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Verifies if credentials passed are valid.
     * 
     * @param clientId
     * @param password
     * @return
     *         -1 authentication error;
     *         0 successful authentication;
     *         1 user does not exist on system yet,
     *         The new user was created and authenticated.
     */
    public int isAuthenticated(String clientId, String password) {
        if (globals == null) {
            globals = new ServerGlobals();
        }

        Map<String, String> users = globals.getUsers();
        
        if(users == null) {
            users = new HashMap<>();
        }
       
        // Check if user exists
        if (!users.containsKey(clientId))
            return 1;
        // Check if password is correct
        if (!users.get(clientId).equals(password))
            return -1;
        return 0;
    }

    /**
     * Stores the credentials and creates all the necessary files for
     * the new user.
     * 
     * @param clientId
     * @param userName
     * @param password
     */
    public void addUserPswrd(String clientId, String password) {
        Map<String,String> users = globals.getUsers();
        Map<String, List<String>> user_photos = globals.getUser_photos();
        Map<String, List<String>> user_followers = globals.getUser_followers();
        Map<String, List<String>> user_follows = globals.getUser_follows();
        Map<String, List<String>> user_owner = globals.getUser_owner();
        Map<String, List<String>> user_participant = globals.getUser_participant();
        users.put(clientId, password);
        user_photos.put(clientId, new ArrayList<String>());
        user_followers.put(clientId, new ArrayList<String>());
        user_follows.put(clientId, new ArrayList<String>());
        user_owner.put(clientId, new ArrayList<String>());
        user_participant.put(clientId, new ArrayList<String>());
        globals.getUserInbox().put(clientId, new ArrayList<>());
    }

    /**
     * Threads used for communication.
     */
    class ServerThread extends Thread {
        private Socket socket = null;
        private ObjectInputStream in = null;
        private ObjectOutputStream out = null;

        ServerThread(Socket in_soc) {
            socket = in_soc;
            try {
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("------Thread running for current connection------");
        } 

        /**
         * Contains all the logic to operate over the current connection.
         */
        @Override
        public void run() {
                Com com = new Com(socket, in, out, globals);

                String clientId = null;
                String password = null;

                // Authenticate.
                clientId = (String) com.receive();
                password = (String) com.receive();

                if (clientId != null && password != null)
                    System.out.println("------User & Pass received------");

                int is_auth = isAuthenticated(clientId, password);
                switch (is_auth) {
                    case -1:
                        com.send(-1);
                        System.out.println("------Credentials incorrect------");
                        break;
                    case 0:
                        com.send(0);
                        System.out.println("------Authentication successful------");
                        break;
                    default:
                        com.send(1);
                        addUserPswrd(clientId, password);
                        break;
                }
                System.out.println("----------------------------------------------");

                // Execute operation.
                boolean stop = false;
                while (!stop) {
                    // Receive operation.
                    Tuple request = (Tuple) com.receive();

                    String aux = null;
                    String[] content = null;
                    String group_id = null;
                    String groups = null;
                    String n_photos = null;
                    List<String> array_to_send;
                    List<String> receivedContent = new ArrayList<>();

                    // Execute:
                    switch ((String) request.getValue(0)) {
                        case "f":
                            // <user_id of who user wants to follow>:<user user_id>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(follow(content[0], content[1]));
                            break;
                        case "u":
                            // <user_id to unfollow>:<current user>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(unfollow(content[0], content[1]));
                            break;
                        case "v":
                            // <current user>
                            aux = (String) com.receive();
                            com.send(viewFollowers(aux));
                            break;
                        case "p":
                            com.send(post(clientId, com));
                            break;
                        case "w":
                            // current user
                            aux = (String) com.receive();
                            // receive n_photos
                            n_photos = (String) com.receive();
                            // wall
                            array_to_send = wall(aux, Integer.valueOf(n_photos));

                            // In case of error.
                            if (array_to_send.size() < 3) {
                                // Send error code.
                                com.send(-1);
                                // Send array which will contain "1" or "2".
                                com.send(array_to_send.get(0));
                            } else {
                            // Success.
                                // send number of photo paths
                                com.send(array_to_send.size() / 3);
                                for (int i = 0; i < array_to_send.size(); i += 3) {
                                    // Send photo identifier.
                                    com.send(array_to_send.get(i));
                                    // Send the owner of the photo.
                                    com.send(array_to_send.get(i+1));
                                    // Send number of photo likes
                                    com.send(array_to_send.get(i+2));
                                    // Photo identifier to "com.java"
                                    com.sendFile(array_to_send.get(i), array_to_send.get(i+1));
                                }
                            }
                            break;
                        case "l":
                            // Receive photo_id.
                            aux = (String) com.receive();
                            com.send(like(aux, clientId));
                            break;
                        case "n":
                            // receive <group_id>
                            aux = (String) com.receive();
                            com.send(newGroup(aux, clientId));
                            break;
                        case "a":
                            // receive
                            // <user_id to add>
                            receivedContent.add((String) com.receive());
                            // <group_id>
                            receivedContent.add((String) com.receive());
                            com.send(addu(receivedContent.get(0), receivedContent.get(1), clientId));
                            receivedContent.clear();
                            break;
                        case "r":
                            // receive
                            // <user_id to add>
                            receivedContent.add((String) com.receive());
                            // <group_id>
                            receivedContent.add((String) com.receive());
                            com.send(removeu(receivedContent.get(0), receivedContent.get(1), clientId));
                            receivedContent.clear();
                            break;
                        case "g":
                            // Current user.
                            com.send(request.getSize() == 1 ? 
                                     ginfo(clientId) : 
                                     ginfo(clientId, 
                                           (String) request.getValue(1)));
                            break;
                        case "m":
                            // <group_id>:<current user>:<message>
                            com.send(msg((String) request.getValue(1), 
                                         (String) request.getValue(2),
                                         clientId));
                            break;
                        case "ch":
                            // <group_id>:<current user>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(canCollectOrHistory(content[0], content[1]));
                            break;
                        case "c":
                            // <group_id>:<current user>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(collect(content[0], content[1]));
                            break;
                        case "h":
                            // <group_id>:<current user>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(history(content[0], content[1]));
                            break;
                        case "s":
                            stop = true;
                            break;
                        default:
                            break;
                    }
                }

            System.out.println("Client thread closed");
            System.out.println("------------------------------------------");
        }

        /**
         * Receives a photo and saves it to the current user's files.
         * @param current_user
         * @param com Communication channel
         * @return true if successful, false if not.
         */
        private boolean post(String current_user, Com com) {
            // increment global photo counter.
            int gpc = globals.getGpc();
            globals.setGpc(gpc + 1);

            // Receive photo.
            try {
                com.receiveFilePost(current_user);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        private Object history(String string, String string2) {
            return null;
        }

        private Object collect(String string, String string2) {
            return null;
        }

        private Object canCollectOrHistory(String string, String string2) {
            return null;
        }

        /**
         * Registers a message to a group.
         * @param groupID
         * @param content
         * @param sender
         * @return 0 if successful,
         *         1 if user not in group or group does not exist,
         *         -1 if error on operation.
         */
        private int msg(String groupID, String content, String sender) {
            // Verify if user is in group or group exists:
            if (!globals.getUser_participant().get(sender).contains(groupID))
                return 1;
            
            // Get users from group
            List<String> groupMembers = new ArrayList<>();
            for (String user : globals.getUsers().keySet()) {
                if (globals.getUser_participant().get(user).contains(groupID))
                    groupMembers.add(user);
            }

            // Create new message
            Message newMessage = new Message(content, sender, groupMembers);
            // Add to current users' inbox
            groupMembers.forEach((String member) -> {
                globals.getUserInbox().get(member).add(newMessage);
            });

            return 0;
        }

        /**
        * Retrieves the names of the groups that currentUser
        * is owner or member of.
        * @param currentUser 
        * @return Pair containing the names of the groups 
        * the currentUser owns in the first position.
        * Names of the groups it takes part in on the 
        * second position of the Pair.
        */
        private Pair<List<String>, List<String>> ginfo(String currentUser) {
            // Retrieve groups the currentUser owns
            List<String> owns = globals.getUser_owner().get(currentUser);
            // Retrieve groups the currentUser participates in
            List<String> participates = globals.getUser_participant().get(currentUser);
            
            return new Pair<List<String>,List<String>>(owns, participates);
        }


        /**
         * Retrieves the name of the group's owner
         * as well the all the participants in groupId
         * @param currentUser
         * @param groupId
         * @return Pair with requested information.
         * null if currentUser is not the owner or
         * does not participates in the group.
         */
        private Pair<String, List<String>> ginfo(String currentUser, String groupId) {
            Set<String> users = globals.getUsers().keySet();

            // Check if currentUser is allowed to retrieve info about groupId.
            String owner = globals.getUser_owner().get(currentUser).contains(groupId) 
                           ? currentUser : null;
            if (owner == null &&
                !globals.getUser_participant().get(currentUser).contains(groupId))
                return null;
            
            // Retrieve owner of the group.
            if (owner == null) {
                for (String user : users) {
                    if (globals.getUser_owner().get(user).contains(groupId))
                        owner = user;
                }
            }

            // Retrieve participants of the group.
            List<String> participants = new ArrayList<>();
            for (String user : users) {
                if (globals.getUser_participant().get(user).contains(groupId))
                    participants.add(user);
            }

            return new Pair<String, List<String>>(owner, participants);
        }



        /**
         * Adds a user to a group that the 
         * current user is the owner of.
         * @param userID user to be added to group.
         * @param groupID 
         * @param clientID current user.
         * @return 0 if user added to group.
         *         1 if group does not exist.
         *         2 if current user is not the owner of the group.
         *         3 user does not exist.
         *         4 user already in group.
         */
        private int addu(String userID, String groupID, String clientID) {
            Set<String> users = globals.getUsers().keySet();
            // Verify if user exists.
            if (!users.contains(userID))
                return 3;
            // Verify is group exists.
            boolean exists = false;
            for (String user : users) {
                List<String> groups = globals.getUser_owner().get(user);
                if (groups.contains(groupID))
                    exists = true;
            }
            if (!exists) {
                // group does not exist.
                return 1;
            }

            // Verify if current user has a group with this name.
            List<String> groups = globals.getUser_owner().get(clientID);
            if (!groups.contains(groupID)) {
                // Current user is not the owner of group.
                return 2;
            }

            // Verify if user is already in group.
            if (globals.getUser_participant().get(userID).contains(groupID))
                return 4;

            // Current user is the owner of groupID.
            // Add new member.
            globals.getUser_participant().get(userID).add(groupID);
            
            return 0;
        }

        /**
         * Removes a user from a group.
         * @param userID user to be removed.
         * @param groupID
         * @param clientID current user.
         * @return 0 if user removed from group.
         *         1 if group does not exist.
         *         2 if current user is not the owner of the group.
         *         3 user does not exist.
         *         4 user already not in group.
         */
        private int removeu(String userID, String groupID, String clientID) {
            Set<String> users = globals.getUsers().keySet();
            // Verify if user exists.
            if (!users.contains(userID))
                return 3;
            // Verify if group exists.
            boolean exists = false;
            for (String user : users) {
                List<String> groups = globals.getUser_owner().get(user);
                if (groups.contains(groupID))
                    exists = true;
            }
            if (!exists) {
                // group does not exist.
                return 1;
            }

            // Verify if current user has a group with this name.
            List<String> groups = globals.getUser_owner().get(clientID);
            if (!groups.contains(groupID)) {
                // Current user is not the owner of group.
                return 2;
            }

            // Verify if user is not already in group.
            if (!globals.getUser_participant().get(userID).contains(groupID))
                return 4;

            // Current user is the owner of groupID.
            // Add new member.
            globals.getUser_participant().get(userID).remove(groupID);
            
            return 0;
        }

        /**
         * Creates new group which the current user is the owner of
         * @param groupId name of group
         * @param owner
         * @return 0 if created successfully,
         *         1 if group with the same name exists,
         */
        private int newGroup(String groupId, String owner) {
            // Check if group already exists.
            Set<String> users = globals.getUsers().keySet();
            boolean exists = false;
            for (String user : users) {
                List<String> groups = globals.getUser_owner().get(user);
                for (String group : groups) {
                    if (group.contentEquals(groupId))
                        exists = true;
                }
            }
            if (exists)
                return 1;
            
            // Add group
            globals.getUser_owner().get(owner).add(groupId);
            globals.getUser_participant().get(owner).add(groupId);

            // Create group's history
            globals.getGroupHistory().put(groupId, new ArrayList<>());
            
            return 0; 
        }

        /**
         * Likes or dislikes the photo_id.
         * @param photo_id 
         * @return 0 if liked,
         *         1 if disliked,
         *         2 if photo does not exist,
         *        -1 if error.
         */
        private int like(String photo_id, String current_user) {
            // Look for photo
            String[] photo_parameters = photo_id.split("-");
            if (photo_parameters.length != 2)
                return -1;
            if (!globals.getUser_photos().get(photo_parameters[0]).contains(photo_id))
                return 2; // photo does not exist.
            Map<String, List<String>> photoLikes = globals.getPhoto_likes();
            List<String> users = photoLikes.get(photo_id);
            // If photo already liked then dislike.
            if (users != null) {
                if (users.contains(current_user)) {
                    users.remove(current_user);
                    return 1;
                }
                // Else like photo.
                else
                    users.add(current_user);
            } else {
                users = new ArrayList<>();
                users.add(current_user);
                photoLikes.put(photo_id, users);
            }
            
            return 0;
        }

        /**
         * Return the n_photos most recent photos from 
         * the followers.
         * @param current_user current user
         * @param n_photos
         * @return list where each photo has 3 entries.
         * First one as the name of the picture, 
         * Second as the name of the author,
         * Third as the number of likes on the photo.
         * null if there is an error on the process.
         * List containing one element representing that
         * there are no photos to be shown.
         */
        private List<String> wall(String current_user, int n_photos) {
            // Get the following from the current user.
            List<String> following = globals.getUser_follows().get(current_user);
            
            // Get current global_photo_counter.
            int global_counter = globals.getGpc();
            
            // Get all the photos from the users in the range: 
            // [global_photo_counter - n_photos + 1..global_photo_counter].
            List<String> photos = new ArrayList<>();
            for (String follow : following) {
                IntStream range_photos = IntStream.range(global_counter - n_photos + 1, global_counter + 1);
                // Search for the photos in the previous announced range
                List<String> user_photos = globals.getUser_photos().get(follow);
                range_photos.forEach(number -> {
                    for (String photo : user_photos) {
                        if (photo.contains(follow + "-" + number)) {
                            int photo_likes = globals.getPhoto_likes().get(photo).size();
                            photos.add(photo);
                            photos.add(follow);
                            photos.add(String.valueOf(photo_likes));
                        }
                    }
                });
            }

            
            // Check if there are no photos to be shown.
            if (photos.isEmpty())
                photos.add("1");
            
            return photos;
        }


        /**
         * Gets the list of followers of clientId.
         * @param clientId
         * @return List of followers. null if error.
         */
        private List<String> viewFollowers(String clientId) {
            // Check if user exists.
            if (!globals.getUsers().containsKey(clientId))
                return new ArrayList<>();
            // Get followers.
            return globals.getUser_followers().get(clientId);
        }

        /**
         * Current_id unfollows user_id.
         * @param user_id
         * @param current_user
         * @return 0 if successfully unfollowed.
         *         1 if user_id does not exist.
         *         2 if user is not followed.
         *         -1 if error during operation.
         */
        private Object unfollow(String user_id, String current_user) {
            // Verify if user_id exists.
            Map<String,String> users = globals.getUsers();
            if (!users.containsKey(user_id))
                return 1;

            //  Verify if user_id is not followed.
            Map<String,List<String>> user_follows = globals.getUser_follows();
            List<String> following = user_follows.get(current_user);
            if (!following.contains(user_id)) 
                return 2;
            
            // Remove user_id from current user's following.
            following.remove(user_id);
            user_follows.put(current_user, following);
            globals.setUser_follows(user_follows);
            
            // Remove current user from user_id followers file.
            Map<String,List<String>> user_followers = globals.getUser_followers();
            List<String> followers = user_followers.get(user_id);
            followers.remove(current_user);
            user_followers.put(user_id, followers);

            return 0;
        }

        /**
         * Current user to follow user_id.
         * @param user_id
         * @param current_user
         * @return 0 if success,
         *         -1 if user does not exist,
         *         1 if user is already followed.
         */
        private int follow(String user_id, String current_user) {
            // Verify if user_id exists.
            Map<String,String> users = globals.getUsers();
            if (!users.containsKey(user_id))
                return -1;

            // Verify if user_id is already been followed.
            Map<String,List<String>> user_follows = globals.getUser_follows();
            List<String> following = user_follows.get(current_user);
            if (following.contains(user_id)) 
                return 1;

            // user_id exists and is not been followed yet.
            // Add user_id to current user's following.txt.
            following.add(user_id);
            user_follows.put(current_user, following);
            globals.setUser_follows(user_follows);

            // Add current user to user_id's followers.txt.
            Map<String,List<String>> user_followers = globals.getUser_followers();
            List<String> followers = user_followers.get(user_id);
            followers.add(current_user);
            user_followers.put(user_id, followers);

            return 0;
        }
    }
}
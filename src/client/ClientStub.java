package src.client;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.javatuples.Pair;
import org.javatuples.Triplet;
import org.javatuples.Unit;

public class ClientStub {

    private Socket client_socket;
    private Com com;

    public ClientStub(String ip_port) {

        connectServer(ip_port);

        com = new Com(client_socket);

        // Directory where received photos will be stored.
        File wall = new File ("wall");
        // In case this directory is not created yet, create it.
        if (!wall.isDirectory()) {
            try {
                wall.mkdir();
            } catch (Exception e) {
                System.err.println(e.getMessage());
                System.err.println("Error creating 'wall' directory");
                System.exit(-1);
            }
        }


    }

    /**
     * Creates connection with server. 
     * @param ip_port String representing the par IP:PORT.
     */
    private void connectServer(String ip_port) {
        String[] server = ip_port.split(":");
        if (server.length == 2) {
            connect(server[0], Integer.parseInt(server[1]));
        } else {
            System.err.println("IP or Port information missing");
            System.exit(-1);
        }
    }

    /**
     * Creates Socket with server.
     * @param ip server's IP.
     * @param port connection port.
     */
    private void connect(String ip, int port) {
        try {
            this.client_socket = new Socket(ip, port);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("Error connecting to server");
        }
    }

    /**
     * Tries to login with client credentials.
     * If there is not client with the given 
     * credentials, a new client is created 
     * with the info given.
     * @param client_id 
     * @param password
     * @exception client_id cannot contain any of the following characters:
     *  ":" or "/" or "-" or " ".
     * @exception password cannot be empty.
     */
    public void login(String client_id, String password) {
        // Send credentials to server.
        try {
            com.send(client_id);
            com.send(password);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("Error while sending credentials to server");
            System.exit(-1);
        }

        System.out.println("...Credentials were sent to server...");

        // Receive answer from server about login.
        int server_answer = 0;
        server_answer = (int) com.receive();

        switch (server_answer) {
            case 0:
            // Successful login.
                System.out.println("...Login successful...");
                break;
            case -1:
            // Invalid password.
                System.out.println("...Invalid password...\n...closing application...");
                break;
            default:
                break;
        }
    }

    /**
     * Registers that the sender_id is following user_id.
     * @param user_id user to be followed
     * @param sender_id user that followed
     * @return 0 if follow info is registered. -1 if not.
     */
    public int follow(String user_id, String sender_id) {
        int result = -1;
        try {
            // Send operation type.
            com.send(new Unit<String>("f"));
            // Send follow info
            com.send(user_id + ":" + sender_id);
            // Receive answer from server
            result = (int) com.receive();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude follow operation");
        }

        return result;
    }

    /**
     * Registers that sender_id unfollowed user_id
     * @param user_id
     * @param sender_id
     * @return 0 if successfuly unfollowed.
     *         1 if user does not exist.
     *         2 if user is not yet followed.
     *         -1 error during operation.
     */
    public int unfollow(String user_id, String sender_id) {
        int result = -1;

        try {
            com.send(new Unit<String>("u"));
            com.send(user_id + ":" + sender_id);
            result = (int) com.receive();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude unfollow operation");
        }
        
        return result;
    }

    /**
     * Retrieves the followers of a given user
     * @param sender_id
     * @return
     */
    public List<String> viewFollowers(String sender_id) {
        List<String> followers = null;

        try {
            com.send(new Unit<String>("v"));
            com.send(sender_id);
            followers = (List<String>) com.receive();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude view followers operation");
        }
        return followers;
    }

    /**
     * Creates a new group.
     * @param group_id group name.
     * @param sender_id group creator.
     * @return 0 if group was created successfully. -1 if not.
     */
    public int newGroup(String group_id) {
        try {
            com.send(new Unit<String>("n"));
            com.send(group_id);
            return (int) com.receive();
        } catch (IOException e) {
            // Error on operation.
            return -1;
        }
    }

    /**
     * Adds user_id to group_id.
     * @param user_id
     * @param group_id
     * @return 0 if successfully added.
     *         1 if group does not exist.
     *         2 if current user is not owner of the group. 
     *         3 user does not exist.
     *         4 user is part of group already.
     *         -1 if error on operation.
     */
    public int addu(String user_id, String group_id) {
        try {
            com.send(new Unit<String>("a"));
            com.send(user_id);
            com.send(group_id);
            return (int) com.receive();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Removes user_id from group_id.
     * @param user_id
     * @param group_id
     * @param group_owner_id has to be grouper id.
     * @return 0 if successfully removed. 
     *         1 if group does not exist.
     *         2 if current user is not owner of the group. 
     *         3 user does not exist.
     *         4 user is not part of group already.
     *         -1 if error on operation.
     */
    public int removeu(String user_id, String group_id) {
        try {
            com.send(new Unit<String>("r"));
            com.send(user_id);
            com.send(group_id);
            return (int) com.receive();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Request sever to register a message to a group.
     * @param groupId
     * @param message
     * @return 0 if successful,
     *         1 if user not in group or group does not exist,
     *         -1 if error on operation.
     */
    public int msg(String groupId, String message) {
        try {
            com.send(new Triplet<String, String, String>("m", groupId, message));
            return (int) com.receive();
        } catch (IOException e) {
            return -1;
        }
    }

    /**
     * Verifies if user_id can make collect or 
     * history operations on group_id.
     * @param group_id
     * @param user_id
     * @return 0 if possible. -1 if not.
     */
    public int canCollectOrHistory(String group_id, String user_id) {
        int result = -1;

        try {
            com.send(new Unit<String>("ch"));
            com.send(group_id + ":" + user_id);
            result = (int) com.receive();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude can collect history operation");
        }

        return result;
    }

    /**
     * Collects all the unread messages from group_id
     * @param group_id
     * @param user_id
     * @return Messages unread by user_id from group_id
     */
    public String[] collect(String group_id, String user_id) {

        String[] messages = null;

        try {
            com.send(new Unit<String>("c"));
            com.send(group_id + ":" + user_id);
            messages = (String[]) com.receive();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude collect operation");
        }

        return messages;
    }


    /**
     * Retrieves all read messages from the group_id.
     * @param group_id group from where messages will
     * be retrieved.
     * @param user_id member of the group_id.
     * @return List of String containing all messages read.
     */
    public String[] history(String group_id, String user_id) {
        String[] messages = null;
        try {
            com.send(new Unit<String>("h"));
            com.send(group_id + ":" + user_id);
            messages = (String[]) com.receive();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude history operation");
        }
        return messages;
    }

    /**
     * Retrieves the names of the groups that user_id
     * is owner or member of.
     * @return Names of the groups or empty if 
     * user_id is neither a member or owner.
     */
    public Pair<List<String>,List<String>> ginfo() {
        Unit<String> request = new Unit<String>("g");
        try {
            com.send(request);
            return (Pair<List<String>,List<String>>) com.receive();
        } catch (IOException e) {
            return null;
        }
    }


    /**
     * Retrieves owner and members of group_id.
     * Only if user_id is member of the group.
     * @param group_id
     * @param user_id
     * @return Owner and members of group_id. 
     * null if user_id is not part of the group.
    */
    public Pair<String, List<String>> ginfo(String group_id) {
        Pair<String, String> request = new Pair<String, String>("g",group_id);
        try {
            com.send(request);
            return (Pair<String, List<String>>) com.receive();
        } catch (IOException e) {
            return null;
        }
    } 

    /**
     * Requests the server to post a photo.
     * @param file_path path to the photo
     * to be posted.
     * @return true if post was successful,
     * false if not.
     */
    public boolean post(String file_path) {
        boolean result = false;
        System.out.println(file_path);
        try {
            this.com.send(new Unit<String>("p"));
            this.com.sendFile("Photos/" + file_path);
            result = (boolean) this.com.receive();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude post operation");
        }
        return result;
    }

    /**
     * Retrieves the n most recent photos from the 
     * followed users of user_id.
     * @param n number of photos to retrieve.
     * @param user_id 
     * @return List containing the identifier to each
     * photo as well as the number of likes on each.
     */
    public List<String> wall(String n, String user_id) {
        List<String> result = new ArrayList<>();
        int size = 0;
        try {
            com.send(new Unit<String>("w"));
            com.send(user_id);
            com.send(n);

            size = (int) com.receive();

            if(size == -1) {
                result.add((String) com.receive());
            } else {
                // Each picture will be represented by three entries
                // in the array:
                // 1. photo_id
                // 2. owner
                // 3. likes
                for(int i = 0; i < size; i++) {
                    String photo_id = (String) com.receive();
                    String owner_id = (String) com.receive();
                    String likes = (String) com.receive(); 
                    result.add(photo_id + " - " + owner_id + " - likes:" + likes);
                    // Receive photo file
                    this.com.receiveFileWall();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude wall operation");
        }
        return result;
    }

    /**
     * Current user likes or dislikes photo_id.
     * @param photo_id photo to be liked.
     * @return 0 if liked,
     *         1 if disliked,
     *         2 if photo does not exist,
     *         -1 if error.
     */
    public int like(String photo_id) {
        int result;
        try {
            com.send(new Unit<String>("l"));
            com.send(photo_id);
        } catch (IOException e) {
            e.printStackTrace();
        }
        result = (int) com.receive();
        return result;
    }
    

    /**
	 * Stops connection with server.
	 * 
	 */
	public void stopClient() {

		try {
			com.send(new Unit<String>("s"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

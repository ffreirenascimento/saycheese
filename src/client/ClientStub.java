package src.client;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientStub {

    private Socket client_socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private Com com;

    public ClientStub(String ip_port) {

        connectServer(ip_port);

        // Create reading and writing streams
        this.in = null;
        this.out = null;

        try {
            this.in = new ObjectInputStream(client_socket.getInputStream());
            this.out = new ObjectOutputStream(client_socket.getOutputStream());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        this.com = new Com(this.client_socket, this.in, this.out);

        // Directory where received photos will be stored.
        File wall = new File ("wall");
        // In case this directory is not created yet, create it.
        if (wall.isDirectory()) {
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
            this.out.writeObject(client_id);
            this.out.writeObject(password);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("Error while sending credentials to server");
            System.exit(-1);
        }

        System.out.println("...Credentials were sent to server...");

        // Receive answer from server about login.
        int server_answer = 0;
        try {
            server_answer = (int) this.in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            System.err.println("Error while receiving login result from server");
            System.exit(-1);
        }

        switch (server_answer) {
            case 0:
            // Successful login
                System.out.println("...Login successful...");
                break;
            case 1:
            // New Client was created with given credentials
                System.out.println("...Client created with success");
                break;
            case -1:
            // Invalid password
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
            this.out.writeObject("f");
            // Send follow info
            this.out.writeObject(user_id + ":" + sender_id);
            // Receive answer from server
            result = (int) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude follow operation");
        }

        return result;
    }

    /**
     * Registers that sender_id unfollowed user_id
     * @param user_id
     * @param sender_id
     * @return
     */
    public int unfollow(String user_id, String sender_id) {
        int result = -1;

        try {
            this.out.writeObject("u");
            this.out.writeObject(user_id + ":" + sender_id);
            result = (int) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
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
    public String viewFollowers(String sender_id) {
        String followers = null;

        try {
            this.out.writeObject("v");
            this.out.writeObject(sender_id);
            followers = (String) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
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
    public int newGroup(String group_id, String sender_id) {
        int result = -1;

        try {
            this.out.writeObject("n");
            this.out.writeObject(group_id + ":" + sender_id);
            result = (int) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude new group operation");
        }

        return result;
    }

    /**
     * Adds user_id to group_id.
     * @param user_id
     * @param group_id
     * @param group_owner_id has to be owner of group_id
     * @return 0 if successfully added. -1 if not.
     */
    public int addu(String user_id, String group_id, String group_owner_id) {
        int result = -1;

        try {
            this.out.writeObject("a");
            this.out.writeObject(user_id + ":" + group_id + ":" + group_owner_id);
            result = (int) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude addu operation");
        }

        return result;
    }

    /**
     * Removes user_id from group_id.
     * @param user_id
     * @param group_id
     * @param group_owner_id has to be grouper id.
     * @return 0 if successfully removed. -1 if not. 
     */
    public int removeu(String user_id, String group_id, String group_owner_id) {
        int result = -1;

        try {
            this.out.writeObject("r");
            this.out.writeObject(user_id + ":" + group_id + ":" + group_owner_id);
            result = (int) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude removeu operation");
        }

        return result;
    }

    public int msg(String group_id, String user_id, String message) {
        int result = -1;

        try {
            this.out.writeObject("m");
            this.out.writeObject(group_id + ":" + user_id);
            this.out.writeObject(message);
            result = (int) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude message operation");
        }

        return result;
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
            this.out.writeObject("ch");
            this.out.writeObject(group_id + ":" + user_id);
            result = (int) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
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
            this.out.writeObject("c");
            this.out.writeObject(group_id + ":" + user_id);
            messages = (String[]) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
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
            this.out.writeObject("h");
            this.out.writeObject(group_id + ":" + user_id);
            messages = (String[]) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude history operation");
        }
        return messages;
    }

    /**
     * Retrieves the names of the groups that user_id
     * is owner of member of.
     * @param user_id 
     * @return Names of the groups or empty if 
     * user_id is neither a member or owner.
     */
    public String[] ginfo(String user_id) {
        String groups;
        String[] result = null;
        try {
            this.out.writeObject("g");
            this.out.writeObject(user_id);
            // warn the server that there is only one argument.
            this.out.writeObject("/");
            groups = (String) this.in.readObject();
            if (groups.equals(""))
                return new String[0];
            result = groups.split(",");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude ginfo operation");
        }
        
        return result;
    }


    /**
     * Retrieves owner and members of group_id.
     * Only if user_id is member of the group.
     * @param group_id
     * @param user_id
     * @return Owner and members of group_id. 
     * null if user_id is not part of the group.
    */
    public String[] ginfo(String group_id, String user_id) {
        String info;
        String[] result = null;
        try {
            this.out.writeObject("g");
            this.out.writeObject(user_id);
            this.out.writeObject(group_id);
            info = (String) this.in.readObject();
            if (info.equals(""))
               return new String[0]; 
            result = info.split(",");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude ginfo operation");
        }
        return result;
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
        try {
            this.com.send("p");
            this.com.sendFile("photos/" + file_path);
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
    public String[] wall(String n, String user_id) {
        String[] result = null;
        int size = 0;
        try {
            this.out.writeObject("w");
            this.out.writeObject(user_id);
            this.out.writeObject(n);

            size = (int) this.in.readObject();

            if(size == -1) {
                result = new String[1];
                result[0] = (String) this.in.readObject();
            } else {
                // Each picture will be represented by two entries
                // in the array:
                // 1. photo_id
                // 2. likes
                result = new String[size*2];
                for(int i = 0; i < result.length; i+=2) {
                    result[i] = (String) this.in.readObject();
                    result[i+1] = (String) this.in.readObject();
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
     * Current user likes photo_id.
     * @param photo_id photo to be liked.
     * @return 0 if successful,
     * -1 if not.
     */
    public int like(String photo_id) {
        int result = -1;
        try {
            this.out.writeObject("l");
            this.out.writeObject(photo_id);
            result = (int) this.in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error while trying to conclude view followers operation");
        }
        return result;
    }
    

    /**
	 * Stops connection with server.
	 * 
	 */
	public void stopClient() {

		try {
			out.writeObject("s");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

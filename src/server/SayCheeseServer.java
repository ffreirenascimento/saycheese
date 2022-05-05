package src.server;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.IntStream;

public class SayCheeseServer {

    private File files_folder;
    private File server_folder;
    private File user_folder;
    private File users_file;
    private File groups_folder;
    private File global_counter_file;

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

        ServerSocket ssoc = null;

        try {
            ssoc = new ServerSocket(Integer.parseInt(socket));
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        FileWriter global_counter_file_fw;

        // Create files and folders
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
     * @param client_id
     * @param password
     * @return
     *         -1 authentication error;
     *         0 successful authentication;
     *         1 user does not exist on system yet,
     *         The new user was created and authenticated.
     */
    public int isAuthenticated(String client_id, String password) {
        String line;
        String[] user_password;
        try (Scanner sc = new Scanner(users_file)) {
            while (sc.hasNextLine()) {
                line = sc.nextLine();
                user_password = line.split(":");
                if (user_password[0].equals(client_id)) {
                    if (user_password[1].equals(password)) {
                        // Authentication successful.
                        return 0;
                    }
                    // Wrong password.
                    return -1;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // User does not exist.
        return 1;
    }

    /**
     * Stores the credentials and creates all the necessary files for
     * the new user.
     * 
     * @param client_id
     * @param user_name
     * @param password
     * @return 0 if successful, -1 if not.
     */
    public int addUserPswrd(String client_id, String user_name, String password) {
        try (Writer output = new BufferedWriter(new FileWriter(users_file, true))) {
            // Save credentials.
            output.append(client_id + ":" + user_name + ":" + password + "\n");
            
            String user_page_path = "files/user/" + client_id;

            File user_page = new File(user_page_path);
            user_page.mkdir();

            Writer user_followers = new BufferedWriter(new FileWriter(user_page_path + "/followers.txt", true));
            user_followers.close();

            Writer user_following = new BufferedWriter(new FileWriter(user_page_path + "/following.txt", true));
            user_following.close();

            Writer user_participant = new BufferedWriter(new FileWriter(user_page_path + "/participant.txt", true));
            user_participant.close();

            Writer user_owner = new BufferedWriter(new FileWriter(user_page_path + "/owner.txt", true));
            user_owner.close();

            File photos_folder = new File(user_page_path + "/photos");
            photos_folder.mkdir();

            System.out.println("Files and folders created for new user");
            return 0;
        } catch (IOException e) {
            System.err.println("Not possible to create a new user");
            e.printStackTrace();
        }
        return -1;
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
                Com com = new Com(socket, in, out);

                String client_id = null;
                String password = null;

                // Authenticate.
                client_id = (String) com.receive();
                password = (String) com.receive();

                if (client_id != null && password != null)
                    System.out.println("------User & Pass received------");

                int is_auth = isAuthenticated(client_id, password);
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
                        String user_name = (String) com.receive();
                        addUserPswrd(client_id, user_name, password);
                        break;
                }
                System.out.println("----------------------------------------------");

                // Execute operation.
                boolean stop = false;
                while (!stop) {
                    // Receive operation.
                    String operation = (String) com.receive();

                    String aux = null;
                    String[] content = null;
                    StringBuilder message_sb = new StringBuilder();
                    String message = null;
                    String group_id = null;
                    String groups = null;
                    int n_photos = 0;
                    List<String> array_to_send;

                    // Execute:
                    switch (operation) {
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
                            // increment global photo counter.
                            File global_counter_file = new File("files/server/globalPhotoCounter.txt");
                            int counter = 0;
                            try {
                                Scanner counter_sc = new Scanner(global_counter_file);
                                counter = Integer.parseInt(counter_sc.nextLine());
                                counter += 1;
                                FileWriter counter_wr = new FileWriter(global_counter_file, false);
                                counter_wr.write(String.valueOf(counter));
                                counter_sc.close();
                                counter_wr.close();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            // Receive photo.
                            try {
                                com.receiveFilePost(client_id);
                                com.send(true);
                            } catch (Exception e) {
                                com.send(false);
							    e.printStackTrace();
                                System.err.println("Error while posting photo");   
                            }
                            break;
                        case "w":
                            // current user
                            aux = (String) com.receive();
                            // receive n_photos
                            n_photos = (int) com.receive();
                            // wall
                            array_to_send = wall(aux, n_photos);

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
                                    com.send(trimPhotoPath(array_to_send.get(i), array_to_send.get(i+1)));
                                    // Send the owner of the photo.
                                    com.send(array_to_send.get(i+1));
                                    // Send number of photo likes
                                    com.send(array_to_send.get(i+2));
                                    // Photo identifier to "com.java"
                                    com.sendFile(array_to_send.get(i).toString());
                                }
                            }
                        case "l":
                            // Receive photo_id.
                            aux = (String) com.receive();
                            com.send(like(aux, client_id));
                            break;
                        case "n":
                            // receive <group_id>:<current user>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(newGroup(content[0], content[1]));
                            break;
                        case "a":
                            // receive <user_id to add>:<group_id>:<current user>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(addu(content[0], content[1], content[2]));
                            break;
                        case "r":
                            // receive <user_id to add>:<group_id>:<current user>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(removeu(content[0], content[1], content[2]));
                            break;
                        case "g":
                            // Current user.
                            aux = (String) com.receive();
                            group_id = (String) com.receive();
                            if (group_id.equals("/")) {
                                groups = ginfo(aux);
                            } else {
                                groups = ginfo(aux, group_id);
                            }

                            // Send answer.
                            if (groups == null) {
                                com.send("");
                            } else {
                                com.send(groups);
                            }
                            break;
                        case "m":
                            // <group_id>:<current user>:<message>
                            aux = (String) com.receive();
                            content = aux.split(":");
                            com.send(msg(content[0], content[1], content[2]));
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
         * Transforms the photo path of the photo into a safer id to be
         * sent back to the client. 
         * In such a format: <photo_owner>:<photo_number>
         * @param photo_id
         * @param photo_owner
         * @return String with the formatted photo_id
         */
        private Object trimPhotoPath(String photo_id, String photo_owner) {
            // According to our file structure the path to a photo will 
            // be of the sort: files/user/freire/photos/p-1.jpg
            //                  (0)                      (4)
            String photo = photo_id.split("/")[4];
            return photo_owner + ":" + photo;
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

        private Object msg(String string, String string2, String string3) {
            return null;
        }

        private String ginfo(String aux, String group_id) {
            return null;
        }

        private String ginfo(String aux) {
            return null;
        }

        private Object removeu(String string, String string2, String string3) {
            return null;
        }

        private Object addu(String string, String string2, String string3) {
            return null;
        }

        private Object newGroup(String string, String string2) {
            return null;
        }

        /**
         * Likes or dislikes the photo_id.
         * @param photo_id 
         * @param client_id
         * @return 0 if liked,
         *         1 if disliked,
         *         2 if photo does not exist,
         *        -1 if error.
         */
        private int like(String photo_id, String client_id) {
            int result = -1;
            
            // Look for photo
            String photo_path = "files/user/" + client_id + "/photos/" + photo_id;
            File photos_folder = new File("files/user/" + client_id + "/photos/");

        
            return result;
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
            List<String> following = new ArrayList<>();
            try (Scanner following_sc = new Scanner(new File("files/user/" + current_user + "/following.txt"))) {
                while (following_sc.hasNextLine()) {
                    following.add(following_sc.nextLine());
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
            
            // Get current global_photo_counter.
            int global_counter;
            try (Scanner gpc_sc = new Scanner(global_counter_file)) {
                global_counter = gpc_sc.nextInt();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return null;
            }
            
            // Get all the photos from the users in the range: 
            // [global_photo_counter - n_photos + 1..global_photo_counter].
            List<String> photos = new ArrayList<>();
            for (String follow : following) {
                IntStream range_photos = IntStream.range(global_counter - n_photos + 1, global_counter + 1);
                // Search for the photos in the previous announced range
                // in the files of the following.
                String photos_path = "files/user/" + follow + "/photos/";
                String likes_path = "files/user/" + follow + "/photos/l-";
                File follower_folder = new File(photos_path);
                String[] photo_names = follower_folder.list();
                range_photos.forEach(number -> {
                    for (String photo : photo_names) {
                        if (photo.contains("p-" + number)) {
                            File likes = new File(likes_path + number + ".txt");
                            try (Scanner likes_sc = new Scanner(likes)) {
                                photos.add(photos_path + photo);
                                photos.add(follow);
                                photos.add(likes_sc.nextLine());
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                range_photos.close();
            }

            // Check if there are no photos to be shown.
            if (photos.isEmpty())
                photos.add("1");
            
            return photos;
        }


        /**
         * Gets the list of followers of client_id.
         * @param client_id
         * @return List of followers. Null if error.
         */
        private Object viewFollowers(String client_id) {
            // Check if user exists.
            File user_folder = new File("files/user/" + client_id);
            if (!user_folder.exists())
                return null;
            
            // Get followers.
            List<String> followers = new ArrayList<>();
            try (Scanner followers_sc = new Scanner(new File("files/user/" + client_id + "/followers.txt"))) {
                while (followers_sc.hasNextLine()) {
                    followers.add(followers_sc.nextLine());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

            return followers;
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
            File user_id_folder = new File("files/user/" + user_id);
            if (!user_id_folder.exists())
                return 1;

            File following_file = new File("files/user/" + current_user + "/following.txt");

            //  Verify if user_id is not followed.
            boolean is_followed = false;
            try(Scanner following_sc = new Scanner(following_file)) {
                while (following_sc.hasNextLine()) {
                    if (following_sc.nextLine().contentEquals(user_id)) {
                        is_followed = true;
                        break;
                    }
                }
                if (!is_followed) 
                    return 2;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return -1;
            }
            
            // Remove user_id from current user's following.
            File following_temp_file = new File("files/user/" + current_user + "/followingTemp.txt");
            try (Scanner following_sc = new Scanner(following_file);
                 FileWriter following_fw = new FileWriter(following_temp_file)) {
                    while (following_sc.hasNextLine()) {
                        String line = following_sc.nextLine();
                        // If not user_id append to temp file.
                        if (!line.contentEquals(user_id)) {
                            following_fw.append(line + "\n");
                        }
                    }
                    // Remove following_file.
                    following_file.delete();
                    // Rename temp file.
                    following_temp_file.renameTo(following_file);
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
            
            // Remove current user from user_id followers file.
            File followers_file = new File("files/user/" + user_id + "/followers.txt");
            File followers_file_temp = new File("files/user/" + user_id + "/followersTemp.txt");
            try (Scanner followers_sc = new Scanner(followers_file);
                FileWriter followers_fw = new FileWriter(followers_file_temp, true)) 
                {
                    while (followers_sc.hasNextLine()) {
                        String line = followers_sc.nextLine();
                        if (!line.contentEquals(current_user)) {
                            followers_fw.append(line + "\n");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
            followers_file.delete();
            followers_file_temp.renameTo(followers_file);

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
            File user_id_folder = new File("files/user/" + user_id);
            if (!user_id_folder.exists())
                return -1;
            
            // Verify if user_id is already been followed.
            File following_file = new File("files/user/" + current_user + "/following.txt");
            try (Scanner following_sc = new Scanner(following_file)) {
                while (following_sc.hasNextLine()) {
                    if (following_sc.nextLine().contentEquals(user_id))
                        return 1;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            // user_id exists and is not been followed yet.
            // Add user_id to current user's following.txt.
            try (FileWriter following_fw = new FileWriter(following_file, true)) {
                following_fw.append(user_id + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            // Add current user to user_id's followers.txt.
            try (FileWriter follower_fw = new FileWriter(new File("files/user/" + user_id + "/followers.txt"), true)) {
                follower_fw.append(current_user + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }

            return 0;
        }
    }
}
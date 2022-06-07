package src.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class SayCheese {

    private static void show_sep() {
        System.out.println("=======");
    }

    public static void main(String[] args) throws IOException {

        Thread shutdown = new Thread(() -> {
            System.out.println("\nShuting down client...");
        });
        Runtime.getRuntime().addShutdownHook(shutdown);

        String client_id;
        int args_len = args.length;

        // Verify arguments:
        // SayCheese <serverAddress> <clientID> [password]
        if(args_len != 3) {
            System.err.println("Error on arguments passed" +
            "\n" + "Please use the following format" +
            "\n" + "SeiTchiz <serverAddress> <clientID> [password]");
            System.exit(-1);
        }

        client_id = args[1];

        // Create connection with server
        ClientStub cs = new ClientStub(args[0]);

        // Login
        cs.login(client_id, args[2]);

        // Main client loop
        boolean stop = false;

        while(!stop) {

            // Show client options
            System.out.println(
                "Choose an operation: \n"
                + "f or follow <userID> \n"
                + "u or unfollow <userID> \n" 
                + "v or viewfollowers \n" 
                + "p or post <photo> \n"
                + "w or wall <nPhotos> \n" 
                + "l or like <photoID> \n" 
                + "n or newgroup <groupID> \n"
                + "a or addu <userID> <groupID> \n" 
                + "r or removeu <userID> <groupID> \n"
                + "g or ginfo [groupID] \n" 
                + "m or msg <groupID> <msg> \n" 
                + "c or collect <groupID> \n"
                + "h or history <groupID> \n" 
                + "s or stop"
            );

            String[] input = null;
            List<String> result = null;

            try {
                System.out.print(">>>");
                input = new BufferedReader(new InputStreamReader(System.in)).readLine().split(" ");
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }

            // Check option chosen
            switch (input[0]) {
                case "f":
                case "follow":
                    switch (cs.follow(input[1], client_id)) {
                        case 0:
                            show_sep();
                            System.out.println("Successfully followed " + input[1]);
                            show_sep();
                            break;
                        case 1:
                            show_sep();
                            System.out.println("The user is already followed");
                            show_sep();
                            break;
                        case -1:
                            show_sep();
                            System.out.println("The user does not exist");
                            show_sep();
                            break;
                        default:
                            break;
                    }
                    break;
                case "u":
                case "unfollow":
                    switch (cs.unfollow(input[1], client_id)) {
                        case 0:
                            show_sep();
                            System.out.println(input[1] + " unfollowed successfully");
                            show_sep();
                            break;
                        case 1:
                            show_sep();
                            System.out.println("The user does not exist");
                            show_sep();
                            break;
                        case 2:
                            show_sep();
                            System.out.println("The user is not followed yet. Please call this " +
                                               "Operation over a user you already follow.");
                            show_sep();
                            break;
                        case -1: 
                            show_sep();
                            System.out.println("Error while unfollowing user...Try again later.");
                            show_sep();
                            break;
                        default:
                            break;
                    }
                    break;
                case "v":
                case "viewfollowers":
                    result = cs.viewFollowers(client_id);
                    if (!result.isEmpty()) {
                        // Print all followers.
                        show_sep();
                        System.out.println("Followers:");
                        result.forEach(System.out::println);
                        show_sep();
                    } else {
                        // No followers.
                        show_sep();
                        System.out.println("No followers");
                        show_sep();
                    }
                    break;
                case "p":
                case "post":
                    if(cs.post(input[1])) {
                        show_sep();
                        System.out.println("Photo posted with success.");
                        show_sep();
                    } else {
                        show_sep();
                        System.out.println("Error while posting photo.");
                        show_sep();
                    }
                    break;
                case "w":
                case "wall":
                    if (input.length != 2) {
                        show_sep();
                        System.out.println("Please provide only the number of photos to be shown from the wall");
                        show_sep();
                        break;
                    }
                    List<String> photos = cs.wall(input[1], client_id);
                    if (photos == null) {
                        show_sep();
                        System.out.println("Error on operation.");
                        show_sep();
                    }
                    if (photos.get(0) == "1") {
                        show_sep();
                        System.out.println("No pictures to show yet.");
                        show_sep();
                    }
                    else {
                        show_sep();
                        photos.forEach(System.out::println);
                        show_sep();
                    }
                    break;
                case "l":
                case "like":
                    show_sep();
                    switch (cs.like(input[1])) {
                        case 0:
                            System.out.println("Photo liked");
                            break;
                        case 1:
                            System.out.println("Photo disliked");
                            break;
                        case 2:
                            System.out.println("Photo does not exist");
                            break;
                        case -1:
                            System.err.println("Error while liking photo");
                        default:
                            break;
                    }
                    show_sep();
                    break;
                case "n":
                case "newgroup":
                    show_sep();
                    switch (cs.newGroup(input[1])) {
                        case 0:
                            System.out.println("Group created with success");
                            break;
                        case 1:
                            System.out.println("Group already exists");
                            break;
                        case -1:
                            System.out.println("An error has occurred");
                            break;
                        default:
                            break;
                    }
                    show_sep();
                    break;
                case "s":
                case "stop":
                    show_sep();  
                    System.out.println("Stopping the application");
                    show_sep();
                    stop = true;
                    break;
                case "a":
                case "addu":
                    if (input.length != 3) {
                        show_sep();
                        System.out.println("Operation should be written as:" + 
                                           "\naddu <user id> <group id>");
                        show_sep();
                        break;
                    }
                    
                    switch (cs.addu(input[1], input[2])) {
                        case 0:
                            show_sep();  
                            System.out.println("User added to group");
                            show_sep();
                            break;
                        case 1:
                            show_sep();  
                            System.out.println("Group does not exist");
                            show_sep();
                            break;
                        case 2:
                            show_sep();  
                            System.out.println("You are not the group owner\nYou must be the owner of the group to add a member");
                            show_sep();
                            break;
                        case 3:
                            show_sep();  
                            System.out.println("User does not exist");
                            show_sep();
                            break;
                        case 4:
                            show_sep();  
                            System.out.println("User already is in group");
                            show_sep();
                            break;
                        case -1:
                            show_sep();  
                            System.out.println("Error on operation");
                            show_sep();
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    show_sep();
                    System.out.println("Invalid input");
                    show_sep();
                    break;
            }
        }

        show_sep();
        System.out.println("End of session");
        show_sep();
    }
    
}

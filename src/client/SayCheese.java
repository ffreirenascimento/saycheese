package src.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SayCheese {

    public void main(String[] args) {

        int args_len = args.length;
        String separator = "======================================";

        // Verify arguments:
        // SayCheese <serverAddress> <clientID> [password]
        if(args_len != 3) {
            System.err.println("Error on arguments passed" +
            "\n" + "Please use the following format" +
            "\n" + "SeiTchiz <serverAddress> <clientID> [password]");
            System.exit(-1);
        }

        System.out.println("Client initialized");

        // Create connection with server

        // Login

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

            try {
                System.out.println(">>>");
                input = new BufferedReader(new InputStreamReader(System.in)).readLine().split(" ");
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }

            // Check option chosen

            switch (input[0]) {
                case "f":
                case "follow":
                    break;
            
                case "s":
                case "stop":
                    System.out.println(separator);  
                    System.out.println("Stopping the application");
                    System.out.println(separator);
                    stop = true;
                    break;
                default:
                    System.out.println(separator);
                    System.out.println("Invalid input");
                    System.out.println(separator);
                    break;
            }
        }

        System.out.println(separator);
        System.out.println("End of session");
        System.out.println(separator);
    }
    
}

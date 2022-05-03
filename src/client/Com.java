package src.client;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Com {

    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private Socket socket;

    public Com(Socket client_socket) {
        socket = client_socket;
        open();
    }

    /**
     * Method that initializes the channels of communication
    */
    public void open() {
        try {
            this.in = new ObjectInputStream(socket.getInputStream());
            this.out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Error creating streams - COM");
            System.exit(-1);
        }
    }

    /**
     * Sends objects through the communication channel
     * @param obj object to be sent
     * @throws IOException
     */
    public void send(Object obj) throws IOException {
        try {
            // Send object itself
            this.out.writeObject(obj);
            this.out.flush();
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Receives Objects through the communication channel
     * @return Object received
     */
    public Object receive() {
        Object obj = null;
        try {
            obj = this.in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            System.err.println("Error receiving object");
			System.err.println(e.getMessage());
        }

        return obj;
    }

    /**
     * Sends files through the communication channel
     * @param file_path
     * @throws IOException
     */
    public void sendFile(String file_path) throws IOException {
        File file = new File(file_path);
        int bytes_read = 0;
        int offset = 0;
        byte[] byte_array = new byte[1024];

        String[] extension = file.getName().split(".");
        if (extension.length == 0) 
            this.out.writeObject(".jpg");
        else 
            this.out.writeObject(extension[extension.length - 1]);
        this.out.flush();
        this.out.writeLong(file.length());
        this.out.flush();

        try (FileInputStream fis = new FileInputStream(file);){
            while ((offset + 1024) < (int) file.length()) {
                bytes_read = fis.read(byte_array, 0, 1024);
                this.out.write(byte_array, 0, bytes_read);
                this.out.flush();
                offset += bytes_read;
            }
    
            if ((1024 + offset) != (int) file.length()) {
                bytes_read = fis.read(byte_array, 0, (int) file.length() - offset);
                this.out.write(byte_array, 0, bytes_read);
                this.out.flush();
            }
        }
    }

    /**
     * Receives files which will be designated to a specific user.
     * There is a global photo counter that is taken into account
     * whenever a new photo is posted. The id of the lasted photo
     * will have the latest photo counter in its id.  
     * @param user_name id of the client to which the file is allocated
     * @throws ClassNotFoundException 
     * @throws IOException
    */
    public void receiveFilePost(String user_name) throws ClassNotFoundException, IOException {
        String file_name = (String) this.in.readObject();
        long file_size = this.in.readLong();
        int bytes_read = 0;
        int offset = 0;
        byte[] byte_array = new byte[1024];

        File gcp_file = new File("files/server/globalPhotoCounter.txt");
        int global_counter = 0;
        try(Scanner sc_gpc = new Scanner(gcp_file)) {
            if(sc_gpc.hasNextLine()) 
                global_counter = Integer.parseInt(sc_gpc.nextLine());
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Create photo file.
        File photo_file = new File("files/user/" + user_name + 
                          "/photos/photo-" + global_counter + ".jpg");
        // In case the photo is in png format.
        if (file_name.endsWith(".png")) {
            photo_file = new File("files/user/" + user_name + 
            "/photos/photo-" + global_counter + ".png");
        }

        // Create a file to store the number of likes a photo has.
        File likes_file = new File("files/user/" + user_name +
                            "/photos/photo-" + global_counter + ".txt");
        FileWriter fw_likes = new FileWriter(likes_file, true);
        try(BufferedWriter bw_likes = new BufferedWriter(fw_likes);) {
            bw_likes.write("0");
        }

        // Receive photo and write to photo file.
        try (FileOutputStream fos = new FileOutputStream(photo_file)) {
            while ((offset + 1024) < (int) file_size) {
                bytes_read = this.in.read(byte_array, 0, 1024);
                fos.write(byte_array, 0, bytes_read);
                fos.flush();
                offset += bytes_read;
            }
            // In case there are still bytes missing to be read.
            if(offset != (int) file_size) {
                bytes_read = this.in.read(byte_array, 0, (int) file_size - offset);
                fos.write(byte_array, 0, bytes_read);
                fos.flush();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Receives a photo from the posts wall.
     * @throws ClassNotFoundException
     * @throws IOException
     */
    public void receiveFileWall() throws ClassNotFoundException, IOException {
        String file_name = (String) this.in.readObject();
        long file_size = this.in.readLong();
        int bytes_read = 0;
        int offset = 0;
        byte[] byte_array = new byte[1024];
        File file = new File("wall/" + file_name);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            while ((offset + 1024) < (int) file_size) {
                bytes_read = this.in.read(byte_array, 0, 1024);
                fos.write(byte_array, 0, bytes_read);
                fos.flush();
                offset += bytes_read;
            }

            if (offset != (int) file_size) {
                bytes_read = this.in.read(byte_array, 0, 1024);
                fos.write(byte_array, 0, bytes_read);
                fos.flush();
            }
        }
    }

    /**
     * Closes all the communication channels.
     * @throws IOException
     */
    public void close() {
        try {
            this.out.flush();
            while (this.in.available() > 0)
                System.out.println("Waiting for current operations...");
            this.in.close();
            this.out.close();
            this.socket.close();
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.err.println("Error closing channels");
        }
        
    }
}

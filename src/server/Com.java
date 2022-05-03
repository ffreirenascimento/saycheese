package src.server;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;


/**
 * Deals with all communication matter
 * with client. */  
public class Com {
    
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private Socket socket;

    public Com(Socket socket, ObjectInputStream in, ObjectOutputStream out) {
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    /**
     * Opens communication channels
     */
    public void open() {
        try {
            this.out = new ObjectOutputStream(this.socket.getOutputStream());
            this.in = new ObjectInputStream(this.socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Error initializing streams");
            e.printStackTrace();
        }
    }

    /**
     * Sends objects through communication channel.
     * @param obj object to be sent.
     */
    public void send(Object obj) {
        try {
            this.out.writeObject(obj);
            this.out.flush();
        } catch (IOException e) {
            System.err.println("Error while sending object");
            e.printStackTrace();
        }
    }

    /**
     * Receives an Object through the
     * communication channel.
     * @return Object received.
     */
    public Object receive() {
        Object obj = null;
        try{
            obj = in.readObject();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
        return obj;
    }

    /**
     * Sends out file through communication channel
     * @param file_name
     */
    public void sendFile(String file_name) {
        File file = new File(file_name);
        int bytes_read = 0;
        int offset = 0;
        byte[] byte_array = new byte[1024];
        
        try (FileInputStream fis = new FileInputStream(file)) {

            this.out.writeObject(file.getName());
            this.out.flush();
            this.out.writeLong(file.length());
            this.out.flush();

            while((offset + 1024) < (int) file.length()) {
                bytes_read = fis.read(byte_array, 0, 1024);
                this.out.write(byte_array, 0, bytes_read);
                this.out.flush();
                offset += bytes_read;
            }

            if ((offset + 1024) != file.length()) {
                bytes_read = fis.read(byte_array, 0, (int) file.length() - offset);
                this.out.write(byte_array, 0, bytes_read);
                this.out.flush();
            }
        } catch (IOException e) {
			System.err.println("Error receiving object");
			System.err.println(e.getMessage());
        }
    }

    /**
     * Receives files from user_name.
     * @param user_name
     * @throws IOException
     */
    public void receiveFilePost(String user_name) {
        String file_format = null;
        long size = 0;
        int bytes_read = 0;
        int offset = 0;
        byte[] byte_array = new byte[1024];
        File gpc_file = new File("files/server/globalPhotoCounter.txt");
        int global_counter = 0;

        try (Scanner gcp_sc = new Scanner(gpc_file)) {
            if(gcp_sc.hasNextLine()) {
                global_counter = Integer.parseInt(gcp_sc.nextLine());
            }
            file_format = (String) this.in.readObject();
            size = this.in.readLong();
        } catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}

        if (file_format.isEmpty() || size == 0) {
            System.err.println("File format or size not received");
            System.exit(-1);
        }

        File photo_file = new File("files/user/" + user_name +
                                    "/photos/p-" + global_counter +
                                    file_format);
        File likes_file = new File("files/user/" + user_name + 
                                    "/photos/l-" + global_counter +
                                    ".txt");
        try {
            FileWriter likes_fw = new FileWriter(likes_file);
            BufferedWriter likes_bw = new BufferedWriter(likes_fw);
            likes_bw.write("0");
            likes_bw.close();
        } catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

        try(FileOutputStream fos = new FileOutputStream(photo_file)) {
            while((offset + 1024) <= (int) size) {
                bytes_read = this.in.read(byte_array, 0, 1024);
                //TODO:ERROR check if offset here is creating a problem, normally was 0.
                fos.write(byte_array, 0, bytes_read);
                fos.flush();
                offset += bytes_read;
            }

            if(offset != (int) size) {
                bytes_read = this.in.read(byte_array, 0, (int) size - offset);
                //TODO:ERROR check if offset here is creating a problem, normally was 0.
                fos.write(byte_array, 0, bytes_read);
                fos.flush();
            }
        } catch(IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    }

    /**
     * Receives photo files
     */
    public void receiveFileWall() {
        String file_name = null;
        long size = 0;
        int bytes_read = 0;
        int offset = 0;
        byte[] byte_array = new byte[1024];
        File file = new File("wall/" + file_name);

        try(FileOutputStream fos = new FileOutputStream(file)) {
            file_name = (String) this.in.readObject();
            size = this.in.readLong();
            while((offset + 1024) <= (int) size) {
                bytes_read = this.in.read(byte_array, 0, 1024);
                //TODO:ERROR check if offset here is creating a problem, normally was 0.
                fos.write(byte_array, 0, bytes_read);
                fos.flush();
                offset += bytes_read;
            }
            if(offset != (int) size) {
                bytes_read = this.in.read(byte_array, 0, 1024);
                //TODO:ERROR check if offset here is creating a problem, normally was 0.
                fos.write(byte_array, 0, bytes_read);
                fos.flush();
            } 
        } catch(IOException | ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		}
    }

    /**
     * Close communication channels
     */
    public void close() {
        try {
            this.out.flush();
            while(this.in.available() > 0)
                System.out.println("Closing streams...");
            this.out.close();
            this.in.close();
        } catch (IOException e) {
			System.err.println("Error closing streams");
			System.err.println(e.getMessage());
		}
    }
}

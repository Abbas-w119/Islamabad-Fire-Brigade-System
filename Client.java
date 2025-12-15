import java.net.*;
import java.io.*;

public class Client {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("127.0.0.1", 8080);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println("2"); // Fire at F-10

            BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );

            System.out.println("Server: " + in.readLine());
            socket.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

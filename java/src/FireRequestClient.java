import java.io.*;
import java.net.*;

public class FireRequestClient {

    public static String sendFireNode(int nodeId) {
        try (Socket socket = new Socket("127.0.0.1", 8080)) {

            OutputStream out = socket.getOutputStream();
            out.write(String.valueOf(nodeId).getBytes());
            out.flush();

            BufferedReader in =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));

            return in.readLine();

        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }
}

import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.io.*;

public class OthelloClient {

    public static void main(String[] args) throws IOException {

        Socket soc = new Socket("localhost", 9999);
        Scanner keyboard = new Scanner(System.in);
        DataOutputStream writer = new DataOutputStream(soc.getOutputStream());
        DataInputStream reader = new DataInputStream(soc.getInputStream());
        String s = "";

        s = reader.readUTF();
        System.out.println(s);
        do {
            s = reader.readUTF();
            System.out.println(s);
            s = reader.readUTF();
            System.out.println(s);
            s = reader.readUTF();
            System.out.println(s);
            if (!(s.contains("Game over!"))) {
                String userInputMenu = keyboard.nextLine();
                writer.writeUTF(userInputMenu);
                writer.flush();
            }
        } while (!(s.contains("Game over!")));

        keyboard.close();
        reader.close();
        writer.close();
        soc.close();

    }
}
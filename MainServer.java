
import java.net.*;

public class MainServer extends OthelloServer implements Runnable {
    public void run() {
        execute();
    }

    public static void main(String[] args) throws Exception {

        ServerSocket se = new ServerSocket(9999);

        while (true) {
            Socket so = se.accept();
            MainServer act = new MainServer();
            act.setSocket(so);
            new Thread(act).start();
        }
    }
}

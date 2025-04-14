import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class OthelloServer {

    private final int board_size = 8;
    private final char empty = '.';
    private final char white = 'w';
    private final char black = 'b';
    private final char possible = 'o';

    protected Socket socket;

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public void execute() {

        char[][] board = new char[board_size][board_size];
        int currentPlayer = 1;

        try {
            DataInputStream reader = new DataInputStream(socket.getInputStream());
            DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
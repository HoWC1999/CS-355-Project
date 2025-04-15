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
        int currentPlayer = 1; // 1 - black, 2 - white
        initializeboard(board);
        try {
            DataInputStream reader = new DataInputStream(socket.getInputStream());
            DataOutputStream writer = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initializeboard(char[][] board) {
        for (int k = 0; k < board_size; k++) {
            for (int l = 0; l < board_size; l++) {
                board[k][l] = empty;
            }
        }
        // initial positions
        board[3][3] = white;
        board[3][4] = black;
        board[4][4] = white;
        board[4][3] = black;
    }

    private String boardToSend(char[][] board) {
        String b = "\n  0 1 2 3 4 5 6 7\n";
        for (int r = 0; r < board_size; r++) {
            b = b + Integer.toString(r) + " ";
            for (int c = 0; c < board_size; c++) {
                b = b + board[r][c] + " ";
            }
        }
        return b;
    }
}
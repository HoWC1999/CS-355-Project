import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.ArrayList;

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

    // puts initial pieces on the board
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

    // check if particular move in row/column is valid
    private boolean isValidmove(char[][] board, int player, int row, int col) {
        boolean result = false;
        if (board[row][col] != empty) {
            return result;
        }
        char opponent = white;
        char currentplayer = black;
        if (player == 2) {
            opponent = black;
            currentplayer = white;
        }
        int[] walkr = { -1, -1, -1, 0, 1, 1, 1, 0 };
        int[] walkc = { -1, 0, 1, 1, 1, 0, -1, -1 };

        for (int n = 0; n < board_size; n++) {
            boolean opponentsquare = false;
            int r = row + walkr[n];
            int c = col + walkc[n];
            while (r >= 0 && c >= 0 && r < board_size && c < board_size && board[r][c] == opponent) {
                r = r + walkr[n];
                c = c + walkc[n];
                opponentsquare = true;
            }
            if (r >= 0 && c >= 0 && r < board_size && c < board_size && board[r][c] == currentplayer
                    && opponentsquare == true) {
                result = true;
            }
        }
        return result;
    }

    // returns all valid moves
    // each move is 2 element integer array
    private ArrayList<int[]> getValidmoves(char[][] board, int player) {
        ArrayList<int[]> returnMoves = new ArrayList<>();
        for (int r = 0; r < board_size; r++) {
            for (int c = 0; c < board_size; c++) {
                if (isValidmove(board, player, r, c)) {
                    returnMoves.add(new int[] { r, c });
                }
            }
        }
        return returnMoves;
    }

    // call after checking if move is valid
    private void flipOpponent(char[][] board, int player, int row, int col) {
        char playerch = black;
        char opponentch = white;
        if (player == 2) {
            opponentch = black;
            playerch = white;
        }
        int[] walkr = { -1, -1, -1, 0, 1, 1, 1, 0 };
        int[] walkc = { -1, 0, 1, 1, 1, 0, -1, -1 };
    }
}
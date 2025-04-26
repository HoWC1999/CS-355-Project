package src;

import java.io.*;
import java.net.*;
import java.util.*;

public class OthelloServer implements Runnable {
  private static final int BOARD_SIZE = 8;
  private static final char EMPTY = '.';
  private static final char WHITE = 'w';
  private static final char BLACK = 'b';

  private final Socket socket;
  private DataInputStream in;
  private DataOutputStream out;

  private final char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
  private int currentPlayer = 1;


  public OthelloServer(Socket socket) {
    this.socket = socket;
    initializeBoard();
  }

  private void initializeBoard() {
    for (int r = 0; r < BOARD_SIZE; r++) {
      for (int c = 0; c < BOARD_SIZE; c++) {
        board[r][c] = EMPTY;
      }
    }
    board[3][3] = WHITE;
    board[3][4] = BLACK;
    board[4][3] = BLACK;
    board[4][4] = WHITE;
  }

  @Override
  public void run() {
    try {
      in  = new DataInputStream(socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());

      // 1) initial setup
      initializeBoard();
      sendBoard();

      while (true) {
        String cmd = in.readUTF().trim();

        // 2) control commands
        if (cmd.equalsIgnoreCase("bye")) {
          break;
        }
        if (cmd.equalsIgnoreCase("RESTART")) {
          initializeBoard();
          sendBoard();
          continue;
        }

        // 3) parse black’s move
        String[] parts = cmd.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);

        // 4) apply black’s move if valid
        if (isValidMove(row, col, BLACK)) {
          placeMove(row, col, BLACK);
        } else {
          System.out.println("Invalid black move: " + row + "," + col);
        }

        // 5) immediately auto‐play white
        List<int[]> whiteMoves = getValidMoves(WHITE);
        if (!whiteMoves.isEmpty()) {
          int[] m = whiteMoves.get(new Random().nextInt(whiteMoves.size()));
          placeMove(m[0], m[1], WHITE);
          System.out.println("Auto white move: " + m[0] + "," + m[1]);
        }

        // 6) check for game‐over
        if (isGameOver()) {
          sendBoard();
          out.writeUTF(getWinnerMessage());
          out.flush();
          // loop back to allow a RESTART without dropping the socket
          continue;
        }

        // 7) normal update
        sendBoard();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      try { socket.close(); } catch (IOException ignored) {}
      System.out.println("Client disconnected.");
    }
  }


  private void sendBoard() throws IOException {
    StringBuilder sb = new StringBuilder();
    for (char[] row : board) {
      for (char cell : row) {
        sb.append(cell);
      }
      sb.append("\n");
    }
    sb.append(currentPlayer).append("\n");
    out.writeUTF(sb.toString());
    out.flush();
  }

  private boolean isGameOver() {
    return boardFull()
      || singleColorLeft()
      || (getValidMoves(BLACK).isEmpty() && getValidMoves(WHITE).isEmpty());
  }

  private boolean boardFull() {
    for (int r = 0; r < BOARD_SIZE; r++)
      for (int c = 0; c < BOARD_SIZE; c++)
        if (board[r][c] == EMPTY)
          return false;
    return true;
  }

  private boolean singleColorLeft() {
    boolean seenBlack = false;
    boolean seenWhite = false;
    for (char[] row : board) {
      for (char cell : row) {
        if (cell == BLACK) seenBlack = true;
        if (cell == WHITE) seenWhite = true;
        if (seenBlack && seenWhite) return false;
      }
    }
    // if we never saw one of them, game over
    return true;
  }

  private String getWinnerMessage() {
    int blackCount = 0;
    int whiteCount = 0;
    for (char[] row : board) {
      for (char cell : row) {
        if (cell == BLACK) blackCount++;
        else if (cell == WHITE) whiteCount++;
      }
    }

    String winner;
    if (blackCount > whiteCount) {
      winner = "Black (You) win! 🏆";
    } else if (whiteCount > blackCount) {
      winner = "White (Server) wins! 🏆";
    } else {
      winner = "It's a tie! 🤝";
    }

    return "Game Over! Final Score: Black " + blackCount + " - White " + whiteCount + ". " + winner;
  }

  private void makeServerMove() {
    List<int[]> moves = getValidMoves(WHITE);
    if (!moves.isEmpty()) {
      Random rand = new Random();
      int[] move = moves.get(rand.nextInt(moves.size()));
      placeMove(move[0], move[1], WHITE);
      System.out.println("Server played at: " + move[0] + "," + move[1]);
    }
  }

  private boolean isValidMove(int row, int col, char playerPiece) {
    if (!isInBounds(row, col) || board[row][col] != EMPTY)
      return false;

    char opponentPiece = (playerPiece == BLACK) ? WHITE : BLACK;
    int[] dirR = {-1, -1, -1, 0, 1, 1, 1, 0};
    int[] dirC = {-1, 0, 1, 1, 1, 0, -1, -1};

    for (int d = 0; d < 8; d++) {
      int r = row + dirR[d];
      int c = col + dirC[d];
      boolean foundOpponent = false;

      while (isInBounds(r, c) && board[r][c] == opponentPiece) {
        foundOpponent = true;
        r += dirR[d];
        c += dirC[d];
      }

      if (foundOpponent && isInBounds(r, c) && board[r][c] == playerPiece) {
        return true;
      }
    }
    return false;
  }

  private void placeMove(int row, int col, char playerPiece) {
    board[row][col] = playerPiece;
    flipPieces(row, col, playerPiece);
  }

  private void flipPieces(int row, int col, char playerPiece) {
    char opponentPiece = (playerPiece == BLACK) ? WHITE : BLACK;
    int[] dirR = {-1, -1, -1, 0, 1, 1, 1, 0};
    int[] dirC = {-1, 0, 1, 1, 1, 0, -1, -1};

    for (int d = 0; d < 8; d++) {
      List<int[]> toFlip = new ArrayList<>();
      int r = row + dirR[d];
      int c = col + dirC[d];

      while (isInBounds(r, c) && board[r][c] == opponentPiece) {
        toFlip.add(new int[]{r, c});
        r += dirR[d];
        c += dirC[d];
      }

      if (isInBounds(r, c) && board[r][c] == playerPiece) {
        for (int[] flip : toFlip) {
          board[flip[0]][flip[1]] = playerPiece;
        }
      }
    }
  }

  private List<int[]> getValidMoves(char playerPiece) {
    List<int[]> moves = new ArrayList<>();
    for (int r = 0; r < BOARD_SIZE; r++) {
      for (int c = 0; c < BOARD_SIZE; c++) {
        if (isValidMove(r, c, playerPiece)) {
          moves.add(new int[]{r, c});
        }
      }
    }
    return moves;
  }

  private boolean isInBounds(int r, int c) {
    return r >= 0 && r < BOARD_SIZE && c >= 0 && c < BOARD_SIZE;
  }
}

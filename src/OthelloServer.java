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
    boolean gameEnded = false;
    try {
      in  = new DataInputStream (socket.getInputStream());
      out = new DataOutputStream(socket.getOutputStream());

      sendBoard();                    // send initial position

      while (true) {
        // 1) Read the next client message…
        String cmd = in.readUTF();
        System.out.println("Received: " + cmd);

        // 2) If it’s our special restart token…
        if (cmd.equalsIgnoreCase("RESTART")) {            // ← ADDED
          initializeBoard();                              // ← ADDED
          currentPlayer = 1;                              // ← ADDED
          gameEnded     = false;                          // ← ADDED
          sendBoard();                                    // ← ADDED
          continue;                                       // ← ADDED
        }

        // 3) If it’s the normal “bye” exit:
        if (cmd.equalsIgnoreCase("bye")) {
          break;
        }

        // 4) Otherwise parse as a move and play through:
        String[] parts = cmd.split(",");
        int row = Integer.parseInt(parts[0]);
        int col = Integer.parseInt(parts[1]);

        if (currentPlayer == 1) { // Client’s turn
          if (isValidMove(row, col, BLACK)) {
            placeMove(row, col, BLACK);
            currentPlayer = 2;
          } else {
            System.out.println("Invalid move: " + row + "," + col);
          }
        } else {                   // Server’s turn
          Thread.sleep(500);
          makeServerMove();
          currentPlayer = 1;
        }

        // 5) Check for game‐over:
        if (isGameOver()) {
          gameEnded = true;
          sendBoard();
          out.writeUTF(getWinnerMessage());
          out.flush();
          // NOTE: do *not* break here if you want to let the client
          //       send “RESTART” after game‐over.  Instead, loop!
          continue;                  // ← ADJUSTED: allow RESTART after the game end
        }

        // 6) Otherwise, push the updated board and continue
        sendBoard();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    finally {
      try { socket.close(); }
      catch (IOException ignored) {}
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

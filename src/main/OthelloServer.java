package src.main;

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

  public OthelloServer(Socket socket) {
    this.socket = socket;
    initializeBoard();
  }

  private void initializeBoard() {
    for (int r = 0; r < BOARD_SIZE; r++)
      for (int c = 0; c < BOARD_SIZE; c++)
        board[r][c] = EMPTY;

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

      // initial position
      initializeBoard();
      sendBoard();

      outer:
      while (true) {
        // 1) end‐of‐game: neither can move
        List<int[]> blackMoves = getValidMoves(BLACK);
        List<int[]> whiteMoves = getValidMoves(WHITE);
        if (blackMoves.isEmpty() && whiteMoves.isEmpty()) {
          sendBoard();
          out.writeUTF(getWinnerMessage());
          out.flush();

          // wait only for RESTART or bye
          while (true) {
            String cmd = in.readUTF().trim();
            if (cmd.equalsIgnoreCase("RESTART")) {
              initializeBoard();
              sendBoard();
              continue outer;
            }
            if (cmd.equalsIgnoreCase("bye")) {
              break outer;
            }
            // otherwise ignore
          }
        }

        // 2) black has no move, white auto‐plays
        if (blackMoves.isEmpty()) {
          int[] m = whiteMoves.get(new Random().nextInt(whiteMoves.size()));
          placeMove(m[0], m[1], WHITE);
          sendBoard();
          continue;
        }

        // 3) read black move or control
        String cmd = in.readUTF().trim();
        if (cmd.equalsIgnoreCase("bye")) break;
        if (cmd.equalsIgnoreCase("RESTART")) {
          initializeBoard();
          sendBoard();
          continue;
        }

        // 4) apply black move
        try {
          String[] parts = cmd.split(",");
          int row = Integer.parseInt(parts[0]);
          int col = Integer.parseInt(parts[1]);
          if (isValidMove(row, col, BLACK)) {
            placeMove(row, col, BLACK);
          }
        } catch (Exception ignored) {}

        // 5) white auto‐plays if possible
        whiteMoves = getValidMoves(WHITE);
        if (!whiteMoves.isEmpty()) {
          int[] m = whiteMoves.get(new Random().nextInt(whiteMoves.size()));
          placeMove(m[0], m[1], WHITE);
        }

        // ---- **NO** second isGameOver() check here! ----

        // 6) normal board update
        sendBoard();
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try { socket.close(); } catch (IOException ignored) {}
      System.out.println("Client disconnected.");
    }
  }

  private void sendBoard() throws IOException {
    StringBuilder sb = new StringBuilder();
    for (char[] row : board) {
      for (char cell : row) sb.append(cell);
      sb.append('\n');
    }
    // client always draws Black’s turn
    sb.append('1').append('\n');
    out.writeUTF(sb.toString());
    out.flush();
  }

  private boolean isGameOver() {
    return boardFull()
      || singleColorLeft()
      || (getValidMoves(BLACK).isEmpty() && getValidMoves(WHITE).isEmpty());
  }

  private boolean boardFull() {
    for (char[] row : board)
      for (char c : row)
        if (c == EMPTY) return false;
    return true;
  }

  private boolean singleColorLeft() {
    boolean seenB = false, seenW = false;
    for (char[] row : board)
      for (char c : row) {
        if (c == BLACK) seenB = true;
        if (c == WHITE) seenW = true;
        if (seenB && seenW) return false;
      }
    return true;
  }

  private String getWinnerMessage() {
    int b=0, w=0;
    for (char[] row : board)
      for (char c : row) {
        if (c == BLACK) b++;
        else if (c == WHITE) w++;
      }
    String winner = b> w ? "Black (You) win"
      : w> b ? "White (Server) wins"
      : "It's a tie";
    return "Game Over! Final Score: Black " + b + " - White " + w + ". " + winner;
  }

  private boolean isValidMove(int r, int c, char p) {
    if (!isInBounds(r,c) || board[r][c] != EMPTY) return false;
    char o = p==BLACK ? WHITE : BLACK;
    int[] dr={-1,-1,-1,0,1,1,1,0}, dc={-1,0,1,1,1,0,-1,-1};
    for (int d=0; d<8; d++) {
      int rr=r+dr[d], cc=c+dc[d];
      boolean seen=false;
      while (isInBounds(rr,cc)&&board[rr][cc]==o) {
        seen=true; rr+=dr[d]; cc+=dc[d];
      }
      if (seen && isInBounds(rr,cc)&& board[rr][cc]==p) return true;
    }
    return false;
  }

  private void placeMove(int r,int c,char p) {
    board[r][c]=p; flipPieces(r,c,p);
  }

  private void flipPieces(int r,int c,char p) {
    char o = p==BLACK ? WHITE : BLACK;
    int[] dr={-1,-1,-1,0,1,1,1,0}, dc={-1,0,1,1,1,0,-1,-1};
    for (int d=0; d<8; d++) {
      List<int[]> chain=new ArrayList<>();
      int rr=r+dr[d], cc=c+dc[d];
      while (isInBounds(rr,cc)&&board[rr][cc]==o) {
        chain.add(new int[]{rr,cc});
        rr+=dr[d]; cc+=dc[d];
      }
      if (isInBounds(rr,cc)&&board[rr][cc]==p) {
        for (int[] xy:chain) board[xy[0]][xy[1]]=p;
      }
    }
  }

  private List<int[]> getValidMoves(char p) {
    List<int[]> m=new ArrayList<>();
    for (int r=0;r<BOARD_SIZE;r++)
      for (int c=0;c<BOARD_SIZE;c++)
        if (isValidMove(r,c,p)) m.add(new int[]{r,c});
    return m;
  }

  private boolean isInBounds(int r,int c) {
    return r>=0&&r<BOARD_SIZE&&c>=0&&c<BOARD_SIZE;
  }
}

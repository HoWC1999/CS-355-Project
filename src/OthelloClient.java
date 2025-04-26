package src;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class OthelloClient extends JFrame {
  private int winCount    = 0;
  private int lossCount   = 0;
  private static final int BOARD_SIZE = 8;
  private static final int CELL_SIZE  = 80;
  private static final Color BOARD_COLOR = new Color(0, 128, 0);

  private static Socket socket;
  private static DataInputStream in;
  private static DataOutputStream out;

  private final char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
  private int currentPlayer = 1; // 1 = black, 2 = white
  private final BoardPanel boardPanel = new BoardPanel();
  private volatile boolean gameOver = false;
  private final List<Point> validMoves = new ArrayList<>();

  public OthelloClient() throws IOException {
    setupNetworking();
    setupGUI();
    new Thread(this::listenToServer).start();
  }

  private static void setupNetworking() throws IOException {
    socket = new Socket("localhost", 9994);
    in     = new DataInputStream(socket.getInputStream());
    out    = new DataOutputStream(socket.getOutputStream());
  }

  private JMenuBar createMenuBar() {
    JMenu menu = new JMenu("Game");
    JMenuItem restart = new JMenuItem("Restart");
    restart.addActionListener(e -> resetGame());
    menu.add(restart);
    JMenuBar bar = new JMenuBar();
    bar.add(menu);
    return bar;
  }

  private void setupGUI() {
    setTitle("Othello Client");
    setJMenuBar(createMenuBar());
    setSize(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE + 40);

    // ↓— do NOT auto-exit on close; we handle shutdown manually
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        // 1) signal the listen thread to stop
        gameOver = true;
        // 2) tell server goodbye (optional but polite)
        try { out.writeUTF("bye"); out.flush(); } catch (IOException exception) {
          exception.getMessage();
        }
        // 3) close our socket
        try { socket.close(); } catch (IOException ignored) {}
        // 4) dispose UI and exit JVM
        dispose();
        System.exit(0);
      }
    });

    setLocationRelativeTo(null);
    boardPanel.setPreferredSize(new Dimension(BOARD_SIZE * CELL_SIZE, BOARD_SIZE * CELL_SIZE));
    add(boardPanel, BorderLayout.CENTER);
    setVisible(true);
  }

  private void resetGame() {
    // 1) show splash and wait
    SplashDialog.showSplash();
    // 2) ask server to restart
    try { out.writeUTF("RESTART"); out.flush(); }
    catch (IOException e) { e.printStackTrace(); }
    // 3) clear flag so the listen loop stays alive
    gameOver = false;
  }

  private void listenToServer() {
    try {
      while (!gameOver) {
        String boardData = in.readUTF();

        // —————————————————————————
        // handle game-over first
        if (boardData.contains("Game Over")) {
          if (boardData.contains("Black (You) win"))    winCount++;
          else if (boardData.contains("White (Server) wins")) lossCount++;

          String stats = "\n\nSession stats: Wins = " + winCount + ", Losses = " + lossCount;

          SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(
              this,
              boardData + stats + "\nPlay again?",
              "Game Over",
              JOptionPane.YES_NO_OPTION
            );
            if (choice == JOptionPane.YES_OPTION) {
              resetGame();
            } else {
              gameOver = true;
            }
          });

          // skip the normal board-update parsing
          continue;
        }

        // —————————————————————————
        // normal board update
        String[] lines = boardData.split("\n");
        for (int r = 0; r < BOARD_SIZE; r++) {
          for (int c = 0; c < BOARD_SIZE; c++) {
            board[r][c] = lines[r].charAt(c);
          }
        }
        currentPlayer = Integer.parseInt(lines[BOARD_SIZE]);

        calculateValidMoves();
        boardPanel.repaint();
      }
    } catch (IOException e) {
      // if gameOver==true because of window close, we expect an IOException from socket.close()
      if (!gameOver) {
        System.out.println("Connection lost unexpectedly.");
      }
    }
    // no need to close socket here — windowClosing handler already did
  }

  private void calculateValidMoves() {
    validMoves.clear();
    char me       = (currentPlayer == 1) ? 'b' : 'w';
    char opponent = (currentPlayer == 1) ? 'w' : 'b';
    int[] dr = {-1,-1,-1,0,1,1,1,0}, dc = {-1,0,1,1,1,0,-1,-1};

    for (int r = 0; r < BOARD_SIZE; r++) {
      for (int c = 0; c < BOARD_SIZE; c++) {
        if (board[r][c] != '.') continue;
        outer: for (int d = 0; d < 8; d++) {
          int nr = r + dr[d], nc = c + dc[d];
          boolean foundOpponent = false;
          while (nr>=0 && nr<BOARD_SIZE && nc>=0 && nc<BOARD_SIZE
            && board[nr][nc] == opponent) {
            nr += dr[d]; nc += dc[d]; foundOpponent = true;
          }
          if (foundOpponent
            && nr>=0 && nr<BOARD_SIZE && nc>=0 && nc<BOARD_SIZE
            && board[nr][nc] == me) {
            validMoves.add(new Point(c, r));
            break outer;
          }
        }
      }
    }
  }

  private class BoardPanel extends JPanel {
    BoardPanel() {
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          if (gameOver) return;
          int col = e.getX() / CELL_SIZE;
          int row = e.getY() / CELL_SIZE;
          sendMove(row, col);
        }
      });
    }

    private void sendMove(int row, int col) {
      try {
        out.writeUTF(row + "," + col);
        out.flush();
      } catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      // draw board background
      g.setColor(BOARD_COLOR);
      g.fillRect(0, 0, getWidth(), getHeight());
      // grid
      g.setColor(Color.BLACK);
      for (int i = 0; i <= BOARD_SIZE; i++) {
        g.drawLine(i*CELL_SIZE, 0, i*CELL_SIZE, BOARD_SIZE*CELL_SIZE);
        g.drawLine(0, i*CELL_SIZE, BOARD_SIZE*CELL_SIZE, i*CELL_SIZE);
      }
      // pieces
      for (int r = 0; r < BOARD_SIZE; r++) {
        for (int c = 0; c < BOARD_SIZE; c++) {
          if (board[r][c] == 'b') drawPiece(g, c, r, Color.BLACK);
          else if (board[r][c] == 'w') drawPiece(g, c, r, Color.WHITE);
        }
      }
      // hints
      g.setColor(new Color(255,255,255,100));
      for (Point p : validMoves) {
        g.fillOval(p.x*CELL_SIZE + 30, p.y*CELL_SIZE + 30, CELL_SIZE-60, CELL_SIZE-60);
      }
      // current player
      g.setColor(Color.BLACK);
      g.setFont(new Font("Arial", Font.BOLD, 20));
      String turn = (currentPlayer == 1) ? "Black's Turn" : "White's Turn";
      g.drawString(turn, 10, BOARD_SIZE*CELL_SIZE + 30);
    }

    private void drawPiece(Graphics g, int col, int row, Color color) {
      g.setColor(color);
      g.fillOval(col*CELL_SIZE + 10, row*CELL_SIZE + 10, CELL_SIZE-20, CELL_SIZE-20);
    }
  }

  public static void main(String[] args) throws IOException {
    // 1) splash → 2) client
    SplashDialog.showSplash();
    new OthelloClient();
  }
}

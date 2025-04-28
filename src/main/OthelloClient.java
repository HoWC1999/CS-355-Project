package src.main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class OthelloClient extends JFrame {
  private int winCount  = 0;
  private int lossCount = 0;

  private static final int BOARD_SIZE = 8;
  private static final int CELL_SIZE  = 80;
  private static final Color BOARD_COLOR = new Color(0,128,0);

  private Socket socket;
  private DataInputStream in;
  private DataOutputStream out;

  private final char[][] board = new char[BOARD_SIZE][BOARD_SIZE];
  private volatile boolean gameOver = false;
  private final List<Point> validMoves = new ArrayList<>();
  private final BoardPanel boardPanel = new BoardPanel();

  public OthelloClient() throws IOException {
    setupNetworking();
    setupGUI();
    new Thread(this::listenToServer).start();
  }

  private void setupNetworking() throws IOException {
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
    setSize(BOARD_SIZE*CELL_SIZE + 20, BOARD_SIZE*CELL_SIZE + 120);
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        gameOver = true;
        try { out.writeUTF("bye"); out.flush(); } catch (IOException ignored) {}
        try { socket.close(); } catch (IOException ignored) {}
        dispose();
        System.exit(0);
      }
    });
    add(boardPanel, BorderLayout.CENTER);
    boardPanel.setPreferredSize(new Dimension(BOARD_SIZE*CELL_SIZE, BOARD_SIZE*CELL_SIZE));
    setLocationRelativeTo(null);
    setVisible(true);
  }

  private void resetGame() {
    SplashDialog.showSplash();
    try { out.writeUTF("RESTART"); out.flush(); } catch (IOException ignored) {}
    gameOver = false;
  }

  private void listenToServer() {
    try {
      while (!gameOver) {
        String msg = in.readUTF();

        // Only react to the single server‐sent "Game Over" message:
        if (msg.startsWith("Game Over")) {
          // update stats
          if (msg.contains("You) win"))      winCount++;
          else if (msg.contains("Server) wins")) lossCount++;

          String stats = "\n\nSession stats: Wins = " + winCount + ", Losses = " + lossCount;
          SwingUtilities.invokeLater(() -> {
            int choice = JOptionPane.showConfirmDialog(
              this,
              msg + stats + "\nPlay again?",
              "Game Over",
              JOptionPane.YES_NO_OPTION
            );
            if (choice == JOptionPane.YES_OPTION) {
              resetGame();
            } else {
              // dispatch windowClosing to clean up and exit
              dispatchEvent(new WindowEvent(OthelloClient.this, WindowEvent.WINDOW_CLOSING));
            }
          });
          continue;
        }

        // Otherwise it's a board update
        String[] lines = msg.split("\n");
        for (int r = 0; r < BOARD_SIZE; r++) {
          for (int c = 0; c < BOARD_SIZE; c++) {
            board[r][c] = lines[r].charAt(c);
          }
        }

        computeValidMoves();
        boardPanel.repaint();
      }
    } catch (IOException e) {
      if (!gameOver) System.out.println("Connection lost unexpectedly.");
    }
  }

  private void computeValidMoves() {
    validMoves.clear();
    char me = 'b', opp = 'w';  // client is always black
    int[] dr = {-1,-1,-1,0,1,1,1,0}, dc = {-1,0,1,1,1,0,-1,-1};

    for (int r = 0; r < BOARD_SIZE; r++) {
      for (int c = 0; c < BOARD_SIZE; c++) {
        if (board[r][c] != '.') continue;
        outer: for (int d = 0; d < 8; d++) {
          int rr = r + dr[d], cc = c + dc[d];
          boolean seen = false;
          while (rr >= 0 && rr < BOARD_SIZE && cc >= 0 && cc < BOARD_SIZE
            && board[rr][cc] == opp) {
            rr += dr[d]; cc += dc[d]; seen = true;
          }
          if (seen && rr >= 0 && rr < BOARD_SIZE && cc >= 0 && cc < BOARD_SIZE
            && board[rr][cc] == me) {
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
          Point p = new Point(e.getX()/CELL_SIZE, e.getY()/CELL_SIZE);
          if (!validMoves.contains(p)) {
            Toolkit.getDefaultToolkit().beep();
            return;
          }
          try { out.writeUTF(p.y + "," + p.x); out.flush(); }
          catch (IOException ex) { ex.printStackTrace(); }
        }
      });
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.setColor(BOARD_COLOR);
      g.fillRect(0,0,getWidth(),getHeight());
      g.setColor(Color.BLACK);
      for (int i=0; i<=BOARD_SIZE; i++) {
        g.drawLine(i*CELL_SIZE,0,i*CELL_SIZE,BOARD_SIZE*CELL_SIZE);
        g.drawLine(0,i*CELL_SIZE,BOARD_SIZE*CELL_SIZE,i*CELL_SIZE);
      }
      for (int r=0; r<BOARD_SIZE; r++) {
        for (int c=0; c<BOARD_SIZE; c++) {
          if (board[r][c]=='b') drawPiece(g, c, r, Color.BLACK);
          else if (board[r][c]=='w') drawPiece(g, c, r, Color.WHITE);
        }
      }
      g.setColor(new Color(255,255,255,100));
      for (Point m : validMoves) {
        g.fillOval(m.x*CELL_SIZE+30, m.y*CELL_SIZE+30, CELL_SIZE-60, CELL_SIZE-60);
      }
    }

    private void drawPiece(Graphics g, int col, int row, Color color) {
      g.setColor(color);
      g.fillOval(col*CELL_SIZE+10, row*CELL_SIZE+10, CELL_SIZE-20, CELL_SIZE-20);
    }
  }

  public static void main(String[] args) throws IOException {
    SplashDialog.showSplash();
    new OthelloClient();
  }
}


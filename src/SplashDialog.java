package src;

import javax.swing.*;
import java.awt.*;

public class SplashDialog extends JDialog {
  public SplashDialog(JFrame owner) {
    super(owner, "Othello Rules", true);
    JTextArea rules = new JTextArea(
      "Othello Rules:\n" +
        "1) Black always moves first.\n" +
        "2) You must bracket opponent pieces to flip.\n" +
        "3) If you have no valid move, you pass.\n" +
        "4) Game ends when:\n" +
        "     • board is full,\n" +
        "     • only one color remains,\n" +
        "     • or neither can move.\n" +
        "5) Highest piece count wins."
    );
    rules.setEditable(false);
    rules.setLineWrap(true);
    rules.setWrapStyleWord(true);
    rules.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

    JButton startBtn = new JButton("Start Game");
    startBtn.addActionListener(e -> dispose());

    setLayout(new BorderLayout());
    add(new JScrollPane(rules), BorderLayout.CENTER);
    add(startBtn, BorderLayout.SOUTH);
    pack();

    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    double height = screen.getHeight();
    double width = screen.getWidth();
    setSize((int) (width/2.0), (int) (height/2.0));
    setLocationRelativeTo(owner);

  }

  public static void showSplash() {
    SplashDialog dlg = new SplashDialog(null);
    dlg.setVisible(true);
  }
}

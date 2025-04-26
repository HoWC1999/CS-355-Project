package src.main;

import org.junit.jupiter.api.*;
import java.lang.reflect.*;

import static org.junit.jupiter.api.Assertions.*;

class OthelloServerTest {
  private OthelloServer server;
  private char[][] board;

  @BeforeEach
  void setUp() throws Exception {
    // Construct a server with a null socket (we won't invoke networking)
    server = new OthelloServer((java.net.Socket) null);
    // Initialize board via private method
    Method init = OthelloServer.class.getDeclaredMethod("initializeBoard");
    init.setAccessible(true);
    init.invoke(server);

    // Access the private board field
    Field boardField = OthelloServer.class.getDeclaredField("board");
    boardField.setAccessible(true);
    board = (char[][]) boardField.get(server);
  }

  @Test
  void testInitializeBoard() {
    assertEquals('w', board[3][3], "Center should be WHITE at (3,3)");
    assertEquals('b', board[3][4], "Center should be BLACK at (3,4)");
    assertEquals('b', board[4][3], "Center should be BLACK at (4,3)");
    assertEquals('w', board[4][4], "Center should be WHITE at (4,4)");
    assertEquals('.', board[0][0], "Corner should be EMPTY");
  }

  @Test
  void testBoardFull() throws Exception {
    // fill entire board
    for (int r = 0; r < 8; r++)
      for (int c = 0; c < 8; c++)
        board[r][c] = 'b';
    Method full = OthelloServer.class.getDeclaredMethod("boardFull");
    full.setAccessible(true);
    assertTrue((boolean) full.invoke(server), "boardFull should return true when no EMPTY cells");
  }

  @Test
  void testSingleColorLeft() throws Exception {
    // only black pieces
    for (int r = 0; r < 8; r++)
      for (int c = 0; c < 8; c++)
        board[r][c] = 'b';
    Method single = OthelloServer.class.getDeclaredMethod("singleColorLeft");
    single.setAccessible(true);
    assertTrue((boolean) single.invoke(server), "singleColorLeft should be true when only one color present");

    // mix in a white piece
    board[0][0] = 'w';
    assertFalse((boolean) single.invoke(server), "singleColorLeft should be false when both colors present");
  }

  @Test
  void testIsValidMoveInitial() throws Exception {
    Method valid = OthelloServer.class.getDeclaredMethod("isValidMove", int.class, int.class, char.class);
    valid.setAccessible(true);
    // initial valid black moves: (2,3),(3,2),(4,5),(5,4)
    assertTrue((boolean) valid.invoke(server, 2, 3, 'b'));
    assertTrue((boolean) valid.invoke(server, 3, 2, 'b'));
    assertTrue((boolean) valid.invoke(server, 4, 5, 'b'));
    assertTrue((boolean) valid.invoke(server, 5, 4, 'b'));
    assertFalse((boolean) valid.invoke(server, 0, 0, 'b'), "(0,0) should not be valid");
  }

  @Test
  void testPlaceMoveFlipsPieces() throws Exception {
    Method placeMove = OthelloServer.class.getDeclaredMethod("placeMove", int.class, int.class, char.class);
    placeMove.setAccessible(true);

    // place at (2,3) should flip (3,3)
    placeMove.invoke(server, 2, 3, 'b');
    assertEquals('b', board[3][3], "After placing at (2,3), (3,3) should become BLACK");
  }

  @Test
  void testGetWinnerMessageTie() throws Exception {
    // 32 black, 32 white => tie
    int half = 4;
    for (int r = 0; r < 8; r++)
      for (int c = 0; c < 8; c++)
        board[r][c] = ((r+c) % 2 == 0) ? 'b' : 'w';

    Method winner = OthelloServer.class.getDeclaredMethod("getWinnerMessage");
    winner.setAccessible(true);
    String msg = (String) winner.invoke(server);
    assertTrue(msg.contains("tie"), "Should report a tie when counts equal");
  }
}

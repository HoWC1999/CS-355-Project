package src.main;


import java.net.*;
import java.io.*;
import java.util.concurrent.atomic.AtomicLong;

public class MainServer {
  private static final int PORT = 9994;
  private static final long INACTIVITY_TIMEOUT = 15 * 60_000L; // 15 minutes

  // Shared timestamp of the last client action
  private static final AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());

  public static void main(String[] args) {
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket();
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new InetSocketAddress(PORT));
      System.out.println("Server started on port " + PORT);

      // Watchdog thread to close server if no client activity
      Thread watchdog = getThread(serverSocket);
      watchdog.start();

      // Main accept loop
      while (true) {
        Socket clientSocket = serverSocket.accept();
        System.out.println("Client connected from " + clientSocket.getRemoteSocketAddress());
        // Reset inactivity timer on a new connection
        lastActivity.set(System.currentTimeMillis());

        // Pass the lastActivity reference to each game thread
        OthelloServer game = new OthelloServer(clientSocket);
        new Thread(game).start();
      }
    } catch (IOException e) {
      if (serverSocket != null && serverSocket.isClosed()) {
        System.out.println("Server socket closed due to inactivity.");
      } else {
        System.err.println("Server error: " + e.getMessage());
        e.printStackTrace();
      }
    } finally {
      if (serverSocket != null && !serverSocket.isClosed()) {
        try { serverSocket.close(); } catch (IOException ignored) {}
      }
      System.out.println("Server has been shut down.");
    }
  }

  private static Thread getThread(ServerSocket serverSocket) {
    Thread watchdog = new Thread(() -> {
      try {
        while (!serverSocket.isClosed()) {
          long idle = System.currentTimeMillis() - lastActivity.get();
          if (idle >= INACTIVITY_TIMEOUT) {
            System.out.println("No client activity for " + (INACTIVITY_TIMEOUT / 60000) + " minutes. Shutting down server.");
            serverSocket.close();
            break;
          }
          Thread.sleep(60_000); // check every minute
        }
      } catch (Exception ignored) {}
    }, "Inactivity-Watchdog");
    watchdog.setDaemon(true);
    return watchdog;
  }
}

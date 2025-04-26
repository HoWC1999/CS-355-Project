package src;

import java.net.*;
import java.io.*;

public class MainServer {
  private static final int PORT = 9994;

  public static void main(String[] args) {
    // Use try-with-resources so serverSocket only closes when main() exits
    try (ServerSocket serverSocket = new ServerSocket()) {
      serverSocket.setReuseAddress(true);
      serverSocket.bind(new InetSocketAddress(PORT));
      System.out.println("Server started on port " + PORT);

      // Loop forever, accepting new clients
      while (true) {
        try {
          Socket clientSocket = serverSocket.accept();
          System.out.println("Client connected from " + clientSocket.getRemoteSocketAddress());
          // Hand off to a per-client thread; each thread closes its own socket
          OthelloServer game = new OthelloServer(clientSocket);
          new Thread(game).start();
        }
        catch (IOException ioe) {
          // Log the accept error, but keep the server socket open
          System.err.println("Error accepting connection: " + ioe.getMessage());
        }
      }
    }
    catch (IOException e) {
      // Fatal: failed to bind or serverSocket creation
      System.err.println("Could not start server on port " + PORT);
      e.printStackTrace();
    }
    // serverSocket is closed automatically here if ever we exit main()
  }
}

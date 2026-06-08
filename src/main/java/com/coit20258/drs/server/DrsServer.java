package com.coit20258.drs.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DrsServer implements Runnable {

    private static final Logger LOGGER          = Logger.getLogger(DrsServer.class.getName());
    public  static final int    PORT            = 9090;
    private static final int    THREAD_POOL_SIZE = 10;

    @Override
    public void run() {
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            LOGGER.info(String.format(
                    "DRS Server started on port %d with %d worker threads.",
                    PORT, THREAD_POOL_SIZE));

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket client = serverSocket.accept();
                    pool.execute(new ClientHandler(client));
                } catch (IOException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        LOGGER.log(Level.WARNING, "Accept error", e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Server failed to start on port " + PORT, e);
        } finally {
            pool.shutdown();
        }
    }

    public static void start() {
        Thread t = new Thread(new DrsServer(), "drs-server");
        t.setDaemon(true);  // dies when the JavaFX app exits
        t.start();
        LOGGER.info("DRS Server thread launched.");
    }
}

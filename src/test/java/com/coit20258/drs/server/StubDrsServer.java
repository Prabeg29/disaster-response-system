package com.coit20258.drs.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Minimal TCP stub that replaces the real DrsServer in unit tests.
 *
 * Binds to an OS-assigned free port (port 0), reads one DrsRequest per
 * connection, passes it to the supplied handler, and writes back the
 * returned DrsResponse.  Start it, redirect AppService.useEndpoint() to
 * the assigned port, run the test, then stop().
 */
public class StubDrsServer {

    private final Function<DrsRequest, DrsResponse> handler;
    private ServerSocket serverSocket;
    private ExecutorService executor;

    public StubDrsServer(Function<DrsRequest, DrsResponse> handler) {
        this.handler = handler;
    }

    /** Starts the stub and returns the port it bound to. */
    public int start() throws IOException {
        serverSocket = new ServerSocket(0);
        executor     = Executors.newCachedThreadPool();
        executor.submit(this::acceptLoop);
        return serverSocket.getLocalPort();
    }

    public void stop() {
        try { serverSocket.close(); } catch (IOException ignored) {}
        executor.shutdownNow();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket client = serverSocket.accept();
                executor.submit(() -> handle(client));
            } catch (IOException e) {
                break;
            }
        }
    }

    private void handle(Socket client) {
        // ObjectInputStream must be created BEFORE ObjectOutputStream on the
        // server side to avoid a deadlock: the client writes its OOS header
        // first, then blocks waiting for the server OOS header; so the server
        // must consume the client header (OIS ctor) before sending its own
        // (OOS ctor).
        try (client;
             ObjectInputStream  in  = new ObjectInputStream(client.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(client.getOutputStream())) {
            DrsRequest  req  = (DrsRequest) in.readObject();
            DrsResponse resp = handler.apply(req);
            out.writeObject(resp);
            out.flush();
        } catch (Exception ignored) {}
    }
}

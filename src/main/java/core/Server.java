package core;

import model.Auction;
import model.Bid;
import utils.CountingSemaphore;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private final ConcurrentHashMap<UUID, CountingSemaphore> auctionSemaphores;
    private final ConcurrentHashMap<UUID, Auction> activeAuctions;
    public ServerSocket serverSocket;
    public final int port;
    public final Database database;
    public static Boolean running;


    public Server(int port){
        this.port = port;
        this.database = new Database();
        this.auctionSemaphores = new ConcurrentHashMap<>();
        this.activeAuctions = new ConcurrentHashMap<>();
        database.loadActiveAuctions();
        running = true;
    }


    public void start() {
        try{
            serverSocket =new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (running){
                Socket socket = serverSocket.accept();
                new AuctionHandler(socket, database, auctionSemaphores);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }
    public void shutdown() {
        running = false;
        try {
            if (serverSocket!=null){
                serverSocket.close();
            }

            database.shutdown();
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }


    public static void main(String[] args) throws IOException {
        try{
            Server server = new Server(8080);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutting down server...");
                server.shutdown();
            }));

            server.start();
        }catch (Exception e){
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }

}

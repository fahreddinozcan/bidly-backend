package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import core.message.AuctionMessage;
import core.message.BidMessage;
import core.message.MessageType;
import model.Auction;
import model.Bid;
import utils.CountingSemaphore;

import java.io.*;
import java.math.BigDecimal;
import java.net.Socket;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionHandler extends Thread {
    private final Socket socket;
    private final BufferedReader input;
    private static PrintWriter output = null;
    private static Database database = null;
    private final ConcurrentHashMap<UUID, CountingSemaphore> auctionSemaphores;
    private static Gson gson = null;

    private static final ConcurrentHashMap<UUID, PrintWriter> connectedClients = new ConcurrentHashMap<>();
    private final UUID clientId;

    public AuctionHandler(Socket socket, Database database, ConcurrentHashMap<UUID, CountingSemaphore> auctionSemaphores) throws IOException {
        this.socket = socket;
        this.input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);
        this.auctionSemaphores = auctionSemaphores;
        AuctionHandler.database = database;
        gson = new GsonBuilder()
                .create();

        this.clientId = UUID.randomUUID();
        System.out.println("NEW CLIENT: " + clientId + "| CLIENT COUNT: " + connectedClients.size());
        connectedClients.put(clientId, output);
        this.start();
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed()) {
                String jsonMessage = input.readLine();
                if (jsonMessage == null) {
                    System.out.println("Connection closed by client");
                    break;
                }

                try {
                    AuctionMessage message = gson.fromJson(jsonMessage, AuctionMessage.class);
                    System.out.println("Received message: " + message);
                    handleMessage(message);
                } catch (Exception e) {
                    System.err.println("Error handling message: " + e.getMessage());
                    writeResponse(new AuctionMessage(MessageType.ERROR, "Error processing message: " + e.getMessage()));
                }
            }
        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
        } finally {
            connectedClients.remove(clientId);
            closeConnection();
        }
    }


    private void handleMessage(AuctionMessage message) throws IOException {
        System.out.println("Handling message: " + message.getType());
        switch (message.getType()) {
            case LIST_AUCTIONS -> {
                handleListAuctions(message);
            }
            case PLACE_BID -> {
                handlePlaceBid(message);
            }
            case CREATE_AUCTION -> {
                handleCreateAuction(message);
            }
            default -> {
                throw new IllegalArgumentException("Invalid message type");
            }
        }
    }

    private void handleListAuctions(AuctionMessage message) {
        try {
            List<Auction> auctions = database.loadActiveAuctions();
            writeResponse(new AuctionMessage(MessageType.LIST_AUCTIONS, auctions));
        } catch (Exception e) {
            writeResponse(new AuctionMessage(MessageType.ERROR_LIST_AUCTIONS, "Error listing auctions: " + e.getMessage()));
        }
    }

    private void handlePlaceBid(AuctionMessage message) {
        JsonObject data = gson.toJsonTree(message.getData()).getAsJsonObject();
        BidMessage bidMessage = gson.fromJson(data, BidMessage.class);

        if (bidMessage == null) {
            writeResponse(new AuctionMessage(MessageType.BID_REJECTED, "Bid data cannot be null"));
            return;
        }

        UUID auctionId = bidMessage.getProductId();
        CountingSemaphore cs = auctionSemaphores.computeIfAbsent(auctionId, k -> new CountingSemaphore(1));

        cs.P();
        try {
            Auction auction = database.getAuction(auctionId);
            if (auction == null) {
                writeResponse(new AuctionMessage(MessageType.BID_REJECTED, "Auction not found"));
                return;
            }

            if (auction.canBid(bidMessage.getPrice())) {
                Bid bid = new Bid(
                        UUID.randomUUID(),
                        bidMessage.getBidder(),
                        bidMessage.getProductId(),
                        bidMessage.getPrice()
                );
                System.out.println("Bid accepted: " + bid.getPrice());
                database.saveBid(bid);
                writeResponse(new AuctionMessage(MessageType.BID_ACCEPTED, "Bid accepted"));
                broadcastAuctionUpdate();
            } else {
                BigDecimal minimumAllowedBid = auction.getMinimumBidIncrement().add(auction.getCurrentPrice());
                String rejectionMessage = String.format(
                        "Bid rejected. Minimum allowed bid is %s (current price %s + minimum increment %s)",
                        minimumAllowedBid,
                        auction.getCurrentPrice().toString(),
                        auction.getMinimumBidIncrement().toString()
                );
                writeResponse(new AuctionMessage(MessageType.BID_REJECTED, rejectionMessage));
            }
        } finally {
            cs.V();
        }
    }

    private void handleCreateAuction(AuctionMessage message) {
        JsonObject data = gson.toJsonTree(message.getData()).getAsJsonObject();

        String auctionName = data.get("name").getAsString().toLowerCase();

        synchronized (auctionName.intern()){
            if (database.isAuctionNameTaken(auctionName)) {
                writeResponse(new AuctionMessage(MessageType.AUCTION_CREATION_REJECTED, "Auction name already taken"));
                return;
            }
            Auction auction = new Auction(
                    data.get("name").getAsString(),
                    data.get("startingPrice").getAsBigDecimal(),
                    data.get("endTime").getAsLong(),
                    data.get("seller").getAsString(),
                    data.get("minimumBidIncrement").getAsBigDecimal()
            );

            auction.setId(UUID.randomUUID());

            try {
                database.createAuction(auction);
                writeResponse(new AuctionMessage(MessageType.AUCTION_CREATION_ACCEPTED, auction.getId()));
                broadcastAuctionUpdate();
            } catch (Exception e) {
                writeResponse(new AuctionMessage(MessageType.AUCTION_CREATION_REJECTED, "Error creating auction: " + e.getMessage()));
            }
        }
    }

    private static void broadcastAuctionUpdate(){
        try {
            List<Auction> auctions = database.loadActiveAuctions();
            AuctionMessage message = new AuctionMessage(MessageType.LIST_AUCTIONS, auctions);
            System.out.println("Broadcasting auction update to "+ connectedClients.size());
            for (PrintWriter client : connectedClients.values()) {
                client.println(gson.toJson(message));
            }
        } catch (Exception e) {
            System.err.println("Error broadcasting auction update: " + e.getMessage());
        }
    }
    private void closeConnection() {
        try {
            input.close();
            output.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeResponse(AuctionMessage message) {
        System.out.println("Sending message TYPE: " + message.getType());
        System.out.println(gson.toJson(message));
        output.println(gson.toJson(message));
    }
}




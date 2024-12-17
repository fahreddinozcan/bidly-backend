package core;

import model.Auction;
import model.Bid;
import java.util.*;

public class Database {
    private final Map<UUID, Auction> auctions;
    private final Map<UUID, Set<Bid>> auctionBids;

    public Database(Map<UUID, Auction> auctions, Map<UUID, Set<Bid>> auctionBids) {
        this.auctions = auctions;
        this.auctionBids = auctionBids;
    }

    public List<Auction> loadActiveAuctions() {
        List<Auction> activeAuctions = new ArrayList<>(auctions.values());
        activeAuctions.forEach(this::loadBidsForAuction);
        return activeAuctions;
    }

    private void loadBidsForAuction(Auction auction) {
        Set<Bid> bids = auctionBids.getOrDefault(auction.getId(), new HashSet<>());
        bids.forEach(auction::addBid);
    }

    public Auction getAuction(UUID id) {
        Auction auction = auctions.get(id);
        if (auction != null) {
            loadBidsForAuction(auction);
        }
        return auction;
    }

    public void createAuction(Auction auction) {
        auctions.put(auction.getId(), auction);
        auctionBids.putIfAbsent(auction.getId(), new HashSet<>());
    }

    public void saveBid(Bid bid) {
        Auction auction = auctions.get(bid.getAuctionId());
        if (auction != null) {
            Set<Bid> bids = auctionBids.computeIfAbsent(bid.getAuctionId(), k -> new HashSet<>());
            bids.add(bid);
            auction.addBid(bid);
        }
    }

    public boolean isAuctionNameTaken(String name){
        String normalizedName = name.toLowerCase();

        return auctions.values().stream().anyMatch(auction -> auction.getName().toLowerCase().equals(normalizedName));
    }

    public void shutdown() {
        auctions.clear();
        auctionBids.clear();
    }
}
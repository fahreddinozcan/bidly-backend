package core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Auction;
import model.Bid;
import redis.clients.jedis.Jedis;


import java.util.*;

public class Database {
    private final Jedis jedis;
    private final Gson gson;
    private static final String AUCTION_KEY_PREFIX = "auction:";
    private static final String BID_KEY_PREFIX = "bid:";
    private static final String ACTIVE_AUCTIONS_SET = "active_auctions";



    public List<Auction> loadActiveAuctions() {
            List<Auction> auctions = new ArrayList<>();
            System.out.println("Loading active auctions");
            Set<String> activeAuctionIds = jedis.smembers(ACTIVE_AUCTIONS_SET);

            for (String id : activeAuctionIds) {
                String auctionJson = jedis.get(AUCTION_KEY_PREFIX + id);
                if (auctionJson != null) {
                    Auction auction = gson.fromJson(auctionJson, Auction.class);
                    loadBidsForAuction(auction);
                    auctions.add(auction);
                }
            }
            return auctions;
    }

    private void loadBidsForAuction(Auction auction) {
            String bidSetKey = BID_KEY_PREFIX + auction.getId() + ":bids";
            Set<String> bidIds = jedis.smembers(bidSetKey);
            for (String bidId : bidIds) {
                String bidJson = jedis.get(BID_KEY_PREFIX + bidId);
                if (bidJson != null) {
                    Bid bid = gson.fromJson(bidJson, Bid.class);
                    auction.addBid(bid);
                }
            }

    }

    public Auction getAuction(UUID id) {
            String auctionJson = jedis.get(AUCTION_KEY_PREFIX + id);
            if (auctionJson == null) {
                return null;
            }
            Auction auction = gson.fromJson(auctionJson, Auction.class);
            loadBidsForAuction(auction);
            return auction;

    }

    public void createAuction(Auction auction) {
        System.out.println(auction);
            String auctionJson = gson.toJson(auction);

            jedis.set(AUCTION_KEY_PREFIX + auction.getId(), auctionJson);
            jedis.sadd(ACTIVE_AUCTIONS_SET, auction.getId().toString());
    }

    public void saveBid(Bid bid) {
            String bidJson = gson.toJson(bid);
            jedis.set(BID_KEY_PREFIX + bid.getId(), bidJson);

            String bidSetKey = BID_KEY_PREFIX + bid.getAuctionId() + ":bids";
            jedis.sadd(bidSetKey, bid.getId().toString());

            String auctionKey = AUCTION_KEY_PREFIX + bid.getAuctionId();
            String auctionJson = jedis.get(auctionKey);
            if (auctionJson != null) {
                Auction auction = gson.fromJson(auctionJson, Auction.class);
                auction.setCurrentPrice(bid.getPrice());

                jedis.set(auctionKey, gson.toJson(auction));
            }

    }


    public void shutdown() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
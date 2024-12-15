package model;

import java.math.BigDecimal;
import java.util.UUID;

public class Bid {
    private final UUID id;

    private final UUID auctionId;
    private String userName;
    private final BigDecimal price;
    private long timestamp;

    public Bid(UUID id, String userName, UUID auctionId, BigDecimal price){
        this.id = id;
        this.userName = userName;
        this.price = price;
        this.auctionId = auctionId;
        this.timestamp = System.currentTimeMillis() /1000;
    }

    public UUID getId() {
        return id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public BigDecimal getPrice() {
        return price;
    }

}

package model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Auction {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private long startTime;
    private long endTime;
    private final String seller;
    private final long createdAt = System.currentTimeMillis() / 1000;
    private List<Bid> biddingHistory = new ArrayList<>();
    private AuctionStatus status = AuctionStatus.PENDING;
    private final BigDecimal minimumBidIncrement;

    public enum AuctionStatus {
        PENDING, ACTIVE, ENDED, CANCELLED, SOLD
    }

    public Auction(String name, BigDecimal startingPrice, long endTime, String seller, BigDecimal minimumBidIncrement) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Product name is required");
        if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Starting price must be greater than zero");
        if (endTime <= System.currentTimeMillis() / 1000)
            throw new IllegalArgumentException("End time must be in the future");
        if (minimumBidIncrement == null || minimumBidIncrement.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Minimum bid increment must be greater than zero");

        this.name = name;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
        System.out.println("Current price: " + currentPrice);
        this.endTime = endTime;
        this.seller = seller;
        this.minimumBidIncrement = minimumBidIncrement;
        this.startTime = System.currentTimeMillis() / 1000;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty())
            throw new IllegalArgumentException("Product name is required");
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public BigDecimal getMinimumBidIncrement() {
        return minimumBidIncrement;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        if (endTime <= System.currentTimeMillis() / 1000)
            throw new IllegalArgumentException("End time must be in the future");
        this.endTime = endTime;
    }

    public String getSeller() {
        return seller;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return status == AuctionStatus.ACTIVE &&
                System.currentTimeMillis() / 1000 < endTime &&
                System.currentTimeMillis() / 1000 > startTime;
    }

    public boolean canBid(BigDecimal bidAmount) {
        if (System.currentTimeMillis() / 1000 >= endTime) {
            return false;
        }

        BigDecimal minimumBid = currentPrice.add(minimumBidIncrement);
        return bidAmount.compareTo(minimumBid) >= 0;
    }

    public List<Bid> getBiddingHistory() {
        synchronized(this) {
            List<Bid> sortedHistory = new ArrayList<>(biddingHistory);
            sortedHistory.sort((b1, b2) -> Long.compare(b2.getTimestamp(), b1.getTimestamp()));
            return sortedHistory;
        }
    }

    public void setBiddingHistory(List<Bid> biddingHistory) {
        this.biddingHistory = biddingHistory;
    }

    public void addBid(Bid bid) {
        if (bid == null)
            throw new IllegalArgumentException("Bid cannot be null");

        boolean bidExists = biddingHistory.stream()
                .anyMatch(existingBid -> existingBid.getId().equals(bid.getId()));

        if (!bidExists) {
            int insertIndex = 0;
            for (int i = 0; i < biddingHistory.size(); i++) {
                if (bid.getTimestamp() < biddingHistory.get(i).getTimestamp()) {
                    insertIndex = i + 1;
                } else {
                    break;
                }
            }
            biddingHistory.add(insertIndex, bid);
            setCurrentPrice(bid.getPrice());
        }
    }

    @Override
    public String toString() {
        return String.format(
                "Auction{id=%s, name='%s'"+
                        "currentPrice=%s, startTime=%d, endTime=%d, seller=%s, ",
                id,
                name,
                currentPrice,
                startTime,
                endTime,
                seller

        );
    }
}
package core.message;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

public class BidMessage implements Serializable {
    private final UUID productId;
    private final String bidder;
    private final BigDecimal price;

    public BidMessage(UUID productId, String bidder, BigDecimal price){
        this.productId = productId;
        this.bidder = bidder;
        this.price = price;
    }

    public UUID getProductId() {
        return productId;
    }

    public String getBidder() {
        return bidder;
    }

    public BigDecimal getPrice() {
        return price;
    }

}

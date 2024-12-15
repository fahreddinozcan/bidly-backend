package core.message;

import java.io.Serializable;

public class AuctionMessage implements Serializable {
    private final MessageType type;
    private final Object data;
    
    public AuctionMessage(MessageType type, Object data){
        this.type = type;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public MessageType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "AuctionMessage{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}

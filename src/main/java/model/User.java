package model;

import java.util.ArrayList;

public class User {
    private Long id;
    private String name;
    private String password;
    private String email;
    private ArrayList<Bid> bids;

    public User(Long id, String name, String email, ArrayList<Bid> bids){
        this.id = id;
        this.name = name;
        this.email = email;
        this.bids = bids;
    }

    public Long getId(){
        return id;
    }

    public void setId(Long id){
        this.id = id;
    }

    public String getName(){
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getPassword(){
        return password;
    }

    public void setPassword(String password){
        this.password = password;
    }

    public String getEmail(){
        return email;
    }

    public void setEmail(String email){
        this.email = email;
    }

    public ArrayList<Bid> getBids(){
        return bids;
    }

    public void setBids(ArrayList<Bid> bids){
        this.bids = bids;
    }

    public void addBid(Bid bid){
        this.bids.add(bid);
    }

    public void removeBid(Bid bid){
        this.bids.remove(bid);
    }
}

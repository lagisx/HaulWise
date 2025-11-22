package com.example.postgresql.UserF;

public class Cargo {
    private int id;
    private String typeTC;
    private double weight;
    private double volume;
    private String product;
    private String from;
    private String to;
    private String loadType;
    private String loadDetails;
    private String date;
    private double priceCard;
    private double priceNDC;
    private String torg;
    private String contact;
    private String owner;

    public Cargo(int id, String typeTC, double weight, double volume, String product, String from, String to, String loadType, String loadDetails, String date, double priceCard, double priceNDC, String torg, String contact, String owner) {
        this.id = id;
        this.typeTC = typeTC;
        this.weight = weight;
        this.volume = volume;
        this.product = product;
        this.from = from;
        this.to = to;
        this.loadType = loadType;
        this.loadDetails = loadDetails;
        this.date = date;
        this.priceCard = priceCard;
        this.priceNDC = priceNDC;
        this.torg = torg;
        this.contact = contact;
        this.owner = owner;
    }

    public int getId() {
        return id;
    }
    public String getTypeTC() {
        return typeTC;
    }
    public double getWeight() {
        return weight;
    }
    public double getVolume() {
        return volume;
    }
    public String getProduct() {
        return product;
    }
    public String getFrom() {
        return from;
    }
    public String getTo() {
        return to;
    }
    public String getLoadType() {
        return loadType;
    }
    public String getLoadDetails() {
        return loadDetails;
    }
    public String getDate() {
        return date;
    }
    public double getPriceCard() {
        return priceCard;
    }
    public double getPriceNDC() {
        return priceNDC;
    }
    public String getTorg() {
        return torg;
    }
    public String getContact() {
        return contact;
    }
    public String getOwner() {
        return owner;
    }
}



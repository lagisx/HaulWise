package com.example.postgresql.UserF;

public class Cargo {
    private int    id;
    private String vehicleType;   
    private double weight;
    private double volume;
    private String product;
    private String fromCity;
    private String toCity;
    private String loadType;
    private String loadDetails;
    private String date;
    private double priceCard;
    private double priceNds;
    private String bargain;
    private String contactPhone;
    private int    ownerId;

    public Cargo(int id, String vehicleType, double weight, double volume,
                 String product, String fromCity, String toCity,
                 String loadType, String loadDetails, String date,
                 double priceCard, double priceNds, String bargain,
                 String contactPhone, int ownerId) {
        this.id           = id;
        this.vehicleType  = vehicleType;
        this.weight       = weight;
        this.volume       = volume;
        this.product      = product;
        this.fromCity     = fromCity;
        this.toCity       = toCity;
        this.loadType     = loadType;
        this.loadDetails  = loadDetails;
        this.date         = date;
        this.priceCard    = priceCard;
        this.priceNds     = priceNds;
        this.bargain      = bargain;
        this.contactPhone = contactPhone;
        this.ownerId      = ownerId;
    }

    public int    getId()           { return id; }
    public String getVehicleType()  { return vehicleType; }
    public double getWeight()       { return weight; }
    public double getVolume()       { return volume; }
    public String getProduct()      { return product; }
    public String getFromCity()     { return fromCity; }
    public String getToCity()       { return toCity; }
    public String getLoadType()     { return loadType; }
    public String getLoadDetails()  { return loadDetails; }
    public String getDate()         { return date; }
    public double getPriceCard()    { return priceCard; }
    public double getPriceNds()     { return priceNds; }
    public String getBargain()      { return bargain; }
    public String getContactPhone() { return contactPhone; }
    public int    getOwnerId()      { return ownerId; }
}

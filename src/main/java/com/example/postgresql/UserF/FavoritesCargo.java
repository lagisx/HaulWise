package com.example.postgresql.UserF;

public class FavoritesCargo {
    private final int id;
    private final int userId;
    private final int cargoId;
    private final String createdAt;

    public FavoritesCargo(int id, int userId, int cargoId, String createdAt) {
        this.id = id;
        this.userId = userId;
        this.cargoId = cargoId;
        this.createdAt = createdAt;
    }

    public int getId()          { return id; }
    public int getUserId()      { return userId; }
    public int getCargoId()     { return cargoId; }
    public String getCreatedAt(){ return createdAt; }
}

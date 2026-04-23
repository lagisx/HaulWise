package com.example.postgresql.controllers.CardControllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class UserCargoCardController {


    @FXML public Label     typeLabel;
    @FXML public Label     routeLabel;
    @FXML public Label     vesObemLabel;
    @FXML public Label     tovarLabel;
    @FXML public Label     dateValue;
    @FXML public Label     cargoTypeLabel;
    @FXML public Label     priceKartaLabel;
    @FXML public Label     priceNDSLabel;
    @FXML public Label     contactLabel;
    @FXML public Label     tradeLabel;
    @FXML public ImageView randomImageOnCard;


    @FXML public Button chatButton;
    @FXML public Button favButton;
    @FXML public Button removeFromFavButton;
    @FXML public Label  deleteLabel;
}
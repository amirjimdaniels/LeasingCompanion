package com.leasingcompanion;

import com.leasingcompanion.pms.MockPmsClient;
import com.leasingcompanion.pms.PmsClient;
import com.leasingcompanion.renewal.expiration.RenewalPlanner;
import com.leasingcompanion.ui.ExpirationView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.time.LocalDate;

public final class App extends Application {

    @Override
    public void start(Stage stage) {
        LocalDate today = LocalDate.now();
        PmsClient pms = new MockPmsClient(today);

        stage.setTitle("Leasing Companion: Renewal & Close Assist");
        stage.setScene(new Scene(new ExpirationView(new RenewalPlanner(pms), today), 1280, 820));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

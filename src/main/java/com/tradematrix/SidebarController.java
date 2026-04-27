package com.tradematrix;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import java.util.Arrays;

public abstract class SidebarController { 

    @FXML protected Button btnProfile;
    @FXML protected Button btnDashboard;
    @FXML protected Button btnPortfolio;
    @FXML protected Button btnHistory;
    @FXML protected Button btnSettings;

    /**
     * Updates the sidebar UI so only the target button is highlighted.
     * This version uses a more robust clearing mechanism.
     */
    protected void updateNavSelection(Button target) {
        // Create the array of buttons
        Button[] navButtons = {btnProfile, btnDashboard, btnPortfolio, btnHistory, btnSettings};

        for (Button b : navButtons) {
            if (b != null) {
                // Remove the active class if it exists
                b.getStyleClass().remove("active-nav");
                
                // Force a CSS pseudo-class reset to ensure no "hover" or "armed" 
                // states are stuck in blue from the global CSS
                b.applyCss(); 
            }
        }

        if (target != null) {
            if (!target.getStyleClass().contains("active-nav")) {
                target.getStyleClass().add("active-nav");
            }
            // Ensure the button is rendered with the new style immediately
            target.applyCss();
        }
    }
}
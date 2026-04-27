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

    /**
     * Navigates to a new FXML view and applies the current theme.
     */
    protected void navigate(javafx.scene.Node anyNodeInScene, String fxmlPath) {
        try {
            javafx.scene.Scene scene = anyNodeInScene.getScene();
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent root = loader.load();
            
            // Apply theme to both scene and the new root to ensure it overrides FXML styles
            applyThemeToNode(root);
            applyThemeToScene(scene);
            
            scene.setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Applies the light theme if enabled in UserSession, otherwise removes it.
     */
    public static void applyTheme(javafx.scene.Scene scene) {
        applyThemeToScene(scene);
    }

    public static void applyThemeToScene(javafx.scene.Scene scene) {
        if (scene == null) return;
        String lightCss = SidebarController.class.getResource("/css/light-theme.css").toExternalForm();
        if (UserSession.getInstance().isLightMode()) {
            if (!scene.getStylesheets().contains(lightCss)) {
                scene.getStylesheets().add(lightCss);
            }
        } else {
            // Remove all occurrences of light-theme to be safe
            while (scene.getStylesheets().contains(lightCss)) {
                scene.getStylesheets().remove(lightCss);
            }
        }
    }

    public static void applyThemeToNode(javafx.scene.Parent node) {
        if (node == null) return;
        String lightCss = SidebarController.class.getResource("/css/light-theme.css").toExternalForm();
        if (UserSession.getInstance().isLightMode()) {
            if (!node.getStylesheets().contains(lightCss)) {
                node.getStylesheets().add(lightCss);
            }
        } else {
            // Remove all occurrences of light-theme to be safe
            while (node.getStylesheets().contains(lightCss)) {
                node.getStylesheets().remove(lightCss);
            }
        }
    }
}
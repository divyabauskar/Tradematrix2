package com.tradematrix;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.concurrent.Task;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DashboardController extends SidebarController { 


    @FXML private Label totalInvestedLabel;
    @FXML private Label currentValueLabel;
    @FXML private Label pnlLabel;
    @FXML private PieChart sectorPieChart;
    @FXML private javafx.scene.chart.LineChart<String, Number> benchmarkChart;
    
    @FXML private VBox profileDetailsCard;
    @FXML private VBox dashboardContent;
    @FXML private Label profileUsernameLabel;
    @FXML private Label profileEmailLabel;
    @FXML private Label profileNameLabel;
    @FXML private Label profileMobileLabel;
    @FXML private Label profileCurrencyLabel;
    
    @FXML private TableView<Holding> holdingsTable;
    @FXML private TableColumn<Holding, String> tickerCol;
    @FXML private TableColumn<Holding, Double> qtyCol;
    @FXML private TableColumn<Holding, Double> avgCostCol;
    @FXML private TableColumn<Holding, Double> ltpCol;
    @FXML private TableColumn<Holding, Double> investedCol;
    @FXML private TableColumn<Holding, Double> currentValCol;
    @FXML private TableColumn<Holding, Double> pnlCol;

    private ObservableList<Holding> holdingsList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Initialize profile and dashboard visibility
        profileDetailsCard.setVisible(false);
        profileDetailsCard.setManaged(false);
        dashboardContent.setVisible(true);
        dashboardContent.setManaged(true);

        UserSession session = UserSession.getInstance();
        profileUsernameLabel.setText("Username: " + (session.getUsername() != null ? session.getUsername() : "N/A"));
        profileEmailLabel.setText("Email: " + (session.getEmail() != null ? session.getEmail() : "N/A"));
        profileNameLabel.setText("Name: " + (session.getFullName() != null ? session.getFullName() : "N/A"));
        profileMobileLabel.setText("Mobile: " + (session.getMobileNumber() != null ? session.getMobileNumber() : "N/A"));
        profileCurrencyLabel.setText("Base Currency: " + (session.getBaseCurrency() != null ? session.getBaseCurrency() : "INR"));
        
        tickerCol.setCellValueFactory(new PropertyValueFactory<>("ticker"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        avgCostCol.setCellValueFactory(new PropertyValueFactory<>("avgCost"));
        ltpCol.setCellValueFactory(new PropertyValueFactory<>("ltp"));
        investedCol.setCellValueFactory(new PropertyValueFactory<>("invested"));
        currentValCol.setCellValueFactory(new PropertyValueFactory<>("currentValue"));
        pnlCol.setCellValueFactory(new PropertyValueFactory<>("pnl"));
        
        pnlCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override protected void updateItem(Double pnl, boolean empty) {
                super.updateItem(pnl, empty);
                if (empty || pnl == null) { setText(null); setStyle(""); }
                else {
                    setText(UserSession.getInstance().formatCurrency(pnl));
                    setStyle(pnl >= 0 ? "-fx-text-fill: #22c55e; -fx-font-weight: bold;" : "-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                }
            }
        });
        holdingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        benchmarkChart.setAnimated(false);
        benchmarkChart.setCreateSymbols(false);
        benchmarkChart.setLegendVisible(true);
        benchmarkChart.setLegendSide(javafx.geometry.Side.BOTTOM);
        benchmarkChart.setStyle("-fx-padding: 10;");
        holdingsTable.setPlaceholder(new Label("No holdings found. Go to Portfolio to add stocks."));
        holdingsTable.setItems(holdingsList);

        updateNavSelection(btnDashboard);

        totalInvestedLabel.setText(UserSession.getInstance().formatCurrency(0));
        currentValueLabel.setText(UserSession.getInstance().formatCurrency(0));
        pnlLabel.setText("Loading...");
        pnlLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 14px;");

        // Load disk cache first
        UserSession.getInstance().loadCacheFromDisk();

        // If cache exists, show it and stay quiet. 
        // Only load fresh from DB if no cache exists at all.
        if (UserSession.getInstance().getCachedHoldings() != null) {
            displayCachedData();
            // We still trigger live price update if they want, but the user specifically said 
            // "should not reload/refresh itself until and unless i hit the refresh button"
            // So we skip loadHoldingsAsync() and updateLivePrices() here.
        } else {
            loadHoldingsAsync();
        }
    }

    private void displayCachedData() {
        UserSession session = UserSession.getInstance();
        if (session.getCachedHoldings() != null) {
            holdingsList.setAll(session.getCachedHoldings());
            holdingsTable.setItems(holdingsList);
            holdingsTable.refresh();
            updateDashboardSummary();
            if (session.getCachedBenchmarkData() != null) {
                updateChart(session.getCachedBenchmarkData());
            }
        }
    }
    
    private void updateDashboardSummary() {
        UserSession session = UserSession.getInstance();
        double totalInvested = 0;
        double currentValue = 0;
        System.out.println("Updating dashboard summary. Holdings count: " + holdingsList.size());
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Holding h : holdingsList) {
            totalInvested += h.getInvested();
            currentValue += h.getCurrentValue();
            System.out.println("Holding: " + h.getTicker() + ", Qty: " + h.getQuantity() + ", Invested: " + h.getInvested());
            pieData.add(new PieChart.Data(h.getTicker(), h.getCurrentValue() > 0 ? h.getCurrentValue() : h.getInvested()));
        }

        sectorPieChart.setData(pieData);
        
        // Add tooltips to each slice for hover information (wait for nodes to be created)
        javafx.application.Platform.runLater(() -> {
            for (PieChart.Data data : sectorPieChart.getData()) {
                javafx.scene.Node node = data.getNode();
                if (node != null) {
                    javafx.scene.control.Tooltip tooltip = new javafx.scene.control.Tooltip(
                        String.format("%s: %s", data.getName(), session.formatCurrency(data.getPieValue()))
                    );
                    tooltip.setStyle("-fx-font-size: 14px; -fx-background-color: #1e293b; -fx-text-fill: white; -fx-border-color: #0ea5e9; -fx-border-width: 1;");
                    javafx.scene.control.Tooltip.install(node, tooltip);
                    
                    // Add slight hover effect (scaling)
                    node.setOnMouseEntered(e -> {
                        node.setScaleX(1.05);
                        node.setScaleY(1.05);
                    });
                    node.setOnMouseExited(e -> {
                        node.setScaleX(1.0);
                        node.setScaleY(1.0);
                    });
                }
            }
        });

        totalInvestedLabel.setText(session.formatCurrency(totalInvested));
        currentValueLabel.setText(session.formatCurrency(currentValue));

        double pnl = currentValue - totalInvested;
        double pnlPct = totalInvested > 0 ? (pnl / totalInvested) * 100 : 0;
        pnlLabel.setText(String.format("%s (%.2f%%)", session.formatCurrency(pnl), pnlPct));
        if (pnl >= 0) {
            pnlLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 14px;");
        } else {
            pnlLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
        }
    }

    private void showBenchmarkPlaceholder() {
        javafx.application.Platform.runLater(() -> {
            benchmarkChart.getData().clear();
            
            // Create placeholder series for both Nifty 50 and Portfolio
            javafx.scene.chart.XYChart.Series<String, Number> nSeries = new javafx.scene.chart.XYChart.Series<>();
            nSeries.setName("Nifty 50 (Benchmark)");
            
            javafx.scene.chart.XYChart.Series<String, Number> pSeries = new javafx.scene.chart.XYChart.Series<>();
            pSeries.setName("My Portfolio");
            
            // Add a placeholder data point so series are visible in legend
            nSeries.getData().add(new javafx.scene.chart.XYChart.Data<>("Loading...", 100));
            pSeries.getData().add(new javafx.scene.chart.XYChart.Data<>("Loading...", 100));
            
            benchmarkChart.getData().addAll(nSeries, pSeries);
            System.out.println("Placeholder chart initialized with both series");
        });
    }

    private String createPortfolioJson() {
        try {
            Map<String, Double> portfolioMap = new HashMap<>();
            double currentTotalValue = 0.0;
            
            for (Holding h : holdingsList) {
                portfolioMap.put(h.getTicker(), h.getQuantity());
                currentTotalValue += h.getCurrentValue();
            }
            
            // Create a JSON with both portfolio map and current total value for reactive graphing
            Map<String, Object> portfolioData = new HashMap<>();
            portfolioData.put("portfolio", portfolioMap);
            portfolioData.put("current_total_value", currentTotalValue > 0 ? currentTotalValue : 100.0);
            
            return new Gson().toJson(portfolioData);
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"portfolio\": {}, \"current_total_value\": 100.0}";
        }
    }

    @FXML
    private void handleRefresh() {
        loadHoldingsAsync();
    }

    @FXML
    private void handleUpdateDetails() {
        showProfileUpdateDialog();
    }

    private void showProfileUpdateDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Profile");
        dialog.setHeaderText("Update your profile details");

        ButtonType saveButtonType = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField usernameField = new TextField();
        usernameField.setPromptText("New Username");

        TextField emailField = new TextField();
        emailField.setPromptText("New Email");

        TextField mobileField = new TextField();
        mobileField.setPromptText("New Mobile No");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("New Password");

        VBox dialogContent = new VBox(12, usernameField, emailField, mobileField, passwordField);
        dialogContent.setStyle("-fx-padding: 20; -fx-background-color: #0f172a;");
        dialog.getDialogPane().setContent(dialogContent);

        dialog.setResultConverter(dialogButton -> dialogButton);
        dialog.showAndWait().ifPresent(result -> {
            if (result == saveButtonType) {
                updateUserProfile(usernameField, emailField, mobileField, passwordField);
            }
        });
    }

    private void updateUserProfile(TextField usernameField, TextField emailField, TextField mobileField, PasswordField passwordField) {
        String newUsername = usernameField.getText().trim();
        String newEmail = emailField.getText().trim();
        String newMobile = mobileField.getText().trim();
        String newPassword = passwordField.getText();

        StringBuilder sql = new StringBuilder("UPDATE user_profile SET ");
        List<Object> params = new ArrayList<>();

        if (!newUsername.isEmpty()) {
            sql.append("username = ?, ");
            params.add(newUsername);
        }
        if (!newEmail.isEmpty()) {
            sql.append("email = ?, ");
            params.add(newEmail);
        }
        if (!newMobile.isEmpty()) {
            sql.append("mobile_number = ?, ");
            params.add(newMobile);
        }
        if (!newPassword.isEmpty()) {
            sql.append("password = ?, ");
            params.add(newPassword);
        }

        if (params.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Changes");
            alert.setHeaderText(null);
            alert.setContentText("No updates were entered. Please fill at least one field.");
            alert.showAndWait();
            return;
        }

        sql.setLength(sql.length() - 2); // Remove trailing comma and space
        sql.append(" WHERE id = ?");
        params.add(UserSession.getInstance().getUserId());

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            int updated = stmt.executeUpdate();
            if (updated > 0) {
                UserSession session = UserSession.getInstance();
                if (!newUsername.isEmpty()) session.setUsername(newUsername);
                if (!newEmail.isEmpty()) session.setEmail(newEmail);
                if (!newMobile.isEmpty()) session.setMobileNumber(newMobile);
                if (!newPassword.isEmpty()) session.setPassword(newPassword);
                refreshProfileDetails();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Profile Updated");
                alert.setHeaderText(null);
                alert.setContentText("Your profile details were updated successfully.");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Update Failed");
                alert.setHeaderText(null);
                alert.setContentText("Unable to update profile. Please try again.");
                alert.showAndWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.setContentText("Error updating profile: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void refreshProfileDetails() {
        UserSession session = UserSession.getInstance();
        profileUsernameLabel.setText("Username: " + (session.getUsername() != null ? session.getUsername() : "N/A"));
        profileEmailLabel.setText("Email: " + (session.getEmail() != null ? session.getEmail() : "N/A"));
        profileNameLabel.setText("Name: " + (session.getFullName() != null ? session.getFullName() : "N/A"));
        profileMobileLabel.setText("Mobile: " + (session.getMobileNumber() != null ? session.getMobileNumber() : "N/A"));
        profileCurrencyLabel.setText("Base Currency: " + (session.getBaseCurrency() != null ? session.getBaseCurrency() : "INR"));
    }

    private void loadHoldingsAsync() {
        new Thread(() -> {
            ObservableList<Holding> loadedHoldings = FXCollections.observableArrayList();
            try {
                loadedHoldings.addAll(DatabaseManager.getHoldings(UserSession.getInstance().getUserId()));
            } catch (Exception e) {
                e.printStackTrace();
            }

            System.out.println("Rows fetched from DB: " + loadedHoldings.size());

            javafx.application.Platform.runLater(() -> {
                holdingsList.setAll(loadedHoldings);
                holdingsTable.setItems(holdingsList);
                updateDashboardSummary();
                holdingsTable.refresh();
                updateLivePrices();
                updateBenchmarkChart(); // Also start here if loading from scratch
            });
        }).start();
    }
    
    private void updateLivePrices() {
        if (holdingsList.isEmpty()) return;
        
        StringBuilder tickers = new StringBuilder();
        for (Holding h : holdingsList) {
            tickers.append(h.getTicker()).append(",");
        }
        
        String pythonExe = Paths.get(System.getProperty("user.dir"), ".venv", "Scripts", "python.exe").toString();
        String scriptPath = Paths.get(System.getProperty("user.dir"), "scripts", "python", "fetch_price.py").toString();

        Task<Void> priceTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath, tickers.toString());
                    pb.redirectErrorStream(true);
                    Process p = pb.start();
                    
                    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String jsonOutputStr = "";
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.trim().startsWith("{")) jsonOutputStr = line.trim();
                    }
                    boolean finished = p.waitFor(8, java.util.concurrent.TimeUnit.SECONDS);
                    if (!finished) {
                        p.destroyForcibly();
                        System.err.println("Live price script timed out. Falling back to default data.");
                    }
                    final String jsonOutput = jsonOutputStr;
                    System.out.println("Live price raw output: " + jsonOutput);
                    javafx.application.Platform.runLater(() -> {
                        boolean updated = false;
                        if (jsonOutput != null && jsonOutput.startsWith("{")) {
                            try {
                                Gson gson = new Gson();
                                Map<String, Double> prices = gson.fromJson(jsonOutput, new TypeToken<Map<String, Double>>(){}.getType());
                                
                                if (prices != null && !prices.isEmpty()) {
                                    double totalInvested = 0;
                                    double currentTotal = 0;
                                    
                                    for (Holding h : holdingsList) {
                                        Double price = prices.get(h.getTicker());
                                        if (price != null && price > 0) {
                                            h.setLtp(price);
                                            h.setCurrentValue(price * h.getQuantity());
                                        } else {
                                            h.setLtp(h.getAvgCost());
                                            h.setCurrentValue(h.getInvested());
                                        }
                                        h.setPnl(h.getCurrentValue() - h.getInvested());
                                        
                                        totalInvested += h.getInvested();
                                        currentTotal += h.getCurrentValue();
                                    }
                                    
                                    holdingsTable.refresh();
                                    
                                    totalInvestedLabel.setText(UserSession.getInstance().formatCurrency(totalInvested));
                                    currentValueLabel.setText(UserSession.getInstance().formatCurrency(currentTotal));
                                    
                                    double pnl = currentTotal - totalInvested;
                                    double pnlPct = (totalInvested > 0) ? (pnl / totalInvested) * 100 : 0;
                                    pnlLabel.setText(String.format("%s (%.2f%%)", UserSession.getInstance().formatCurrency(pnl), pnlPct));
                                    if (pnl >= 0) pnlLabel.setStyle("-fx-text-fill: #22c55e; -fx-font-weight: bold; -fx-font-size: 14px;");
                                    else pnlLabel.setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 14px;");
                                    
                                    ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                                    for (Holding h : holdingsList) {
                                        pieData.add(new PieChart.Data(h.getTicker(), h.getCurrentValue()));
                                    }
                                    sectorPieChart.setData(pieData);
                                    updated = true;
                                    
                                    UserSession.getInstance().setCachedHoldings(new java.util.ArrayList<>(holdingsList));
                                    UserSession.getInstance().setLastCacheTime(System.currentTimeMillis());
                                    UserSession.getInstance().saveCacheToDisk();
                                }
                            } catch (Exception e) {
                                System.err.println("JSON Parse Error: " + e.getMessage());
                            }
                        }
                        if (!updated) {
                            double totalInvested = 0;
                            for (Holding h : holdingsList) {
                                h.setLtp(h.getAvgCost());
                                h.setCurrentValue(h.getInvested());
                                h.setPnl(0);
                                totalInvested += h.getInvested();
                            }
                            holdingsTable.refresh();
                            totalInvestedLabel.setText(UserSession.getInstance().formatCurrency(totalInvested));
                            currentValueLabel.setText(UserSession.getInstance().formatCurrency(totalInvested));
                            pnlLabel.setText(String.format("%s (%.2f%%)", UserSession.getInstance().formatCurrency(0), 0.0));
                            pnlLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 14px;");
                            ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
                            for (Holding h : holdingsList) {
                                pieData.add(new PieChart.Data(h.getTicker(), h.getCurrentValue()));
                            }
                            sectorPieChart.setData(pieData);
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Live price error: " + e.getMessage());
                }
                return null;
            }
        };

        new Thread(priceTask).start();
    }
    
    private void scheduleBenchmarkChartUpdate() {
        new Thread(() -> {
            try {
                Thread.sleep(250);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            updateBenchmarkChart();
        }).start();
    }
    
    private void updateBenchmarkChart() {
        if (holdingsList.isEmpty()) {
            System.err.println("No holdings to display on benchmark chart");
            return;
        }
        
        // First, try to show cached data immediately if available
        String cachedData = UserSession.getInstance().getCachedBenchmarkData();
        if (cachedData != null && !cachedData.isEmpty()) {
            System.out.println("Showing cached benchmark data immediately");
            parseAndDisplayBenchmark(cachedData);
        } else {
            System.out.println("No cached data, showing placeholder");
            showBenchmarkPlaceholder();
        }
        
        // Then fetch fresh data in background
        fetchBenchmarkDataAsync();
    }
    
    private void parseAndDisplayBenchmark(String cachedData) {
        if (cachedData == null || cachedData.trim().isEmpty()) {
            showBenchmarkPlaceholder();
            return;
        }
        updateChart(cachedData);
    }

    private void styleBenchmarkSeries(javafx.scene.chart.XYChart.Series<String, Number> portfolioSeries,
                                      javafx.scene.chart.XYChart.Series<String, Number> niftySeries) {
        if (portfolioSeries.getNode() != null) {
            portfolioSeries.getNode().setStyle("-fx-stroke: #0ea5e9; -fx-stroke-width: 3px;");
        }
        if (niftySeries.getNode() != null) {
            niftySeries.getNode().setStyle("-fx-stroke: #cccccc; -fx-stroke-dash-array: 12 6; -fx-stroke-width: 2px;");
        }
    }

    private void fetchBenchmarkDataAsync() {
        if (holdingsList.isEmpty()) return;
        
        String portfolioJson = createPortfolioJson();
        System.out.println("Portfolio JSON being sent: " + portfolioJson);
        
        String pythonExe = Paths.get(System.getProperty("user.dir"), ".venv", "Scripts", "python.exe").toString();
        String scriptPath = Paths.get(System.getProperty("user.dir"), "scripts", "python", "engine.py").toString();
        
        System.out.println("Fetching fresh benchmark data with JavaFX Task in background");

        Task<String> benchmarkTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                // On Windows, wrap the JSON in double quotes and escape internal quotes for the command line
                String escapedPortfolioJson = "\"" + portfolioJson.replace("\"", "\\\"") + "\"";
                ProcessBuilder pb = new ProcessBuilder(pythonExe, scriptPath, escapedPortfolioJson);
                pb.redirectErrorStream(false);
                Process p = pb.start();
                
                java.io.BufferedReader stdoutReader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()));
                java.io.BufferedReader stderrReader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getErrorStream()));
                
                StringBuilder jsonOutputBuilder = new StringBuilder();
                String line;
                
                // Collect stderr asynchronously
                Thread stderrThread = new Thread(() -> {
                    try {
                        String errLine;
                        while ((errLine = stderrReader.readLine()) != null) {
                            if (!errLine.isEmpty()) {
                                System.err.println("[Benchmark] " + errLine);
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("Error reading stderr: " + e.getMessage());
                    }
                });
                stderrThread.setDaemon(true);
                stderrThread.start();
                
                while ((line = stdoutReader.readLine()) != null) {
                    if (line.trim().startsWith("{")) {
                        jsonOutputBuilder.append(line.trim());
                    }
                }
                
                boolean finished = p.waitFor(20, java.util.concurrent.TimeUnit.SECONDS);
                if (!finished) {
                    p.destroyForcibly();
                    System.err.println("Benchmark script timed out, killing process");
                }
                stderrThread.join(2000);
                
                return jsonOutputBuilder.toString();
            }
        };

        benchmarkTask.setOnSucceeded(event -> {
            String jsonOutput = benchmarkTask.getValue();
            System.out.println("Python script completed. Received output: " + (jsonOutput == null || jsonOutput.isEmpty() ? "(empty)" : jsonOutput.length() + " chars"));
            
            if (jsonOutput != null && !jsonOutput.isEmpty()) {
                try {
                    Gson gson = new Gson();
                    BenchmarkData benchmarkData = gson.fromJson(jsonOutput, BenchmarkData.class);
                    if (benchmarkData != null && benchmarkData.dates != null && benchmarkData.nifty != null && benchmarkData.portfolio != null
                        && !benchmarkData.dates.isEmpty() && !benchmarkData.nifty.isEmpty() && !benchmarkData.portfolio.isEmpty()) {
                        System.out.println("Valid benchmark data received: " + benchmarkData.dates.size() + " dates");
                        UserSession.getInstance().setCachedBenchmarkData(jsonOutput);
                        UserSession.getInstance().saveCacheToDisk();
                        updateChart(jsonOutput);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing benchmark JSON: " + e.getMessage());
                }
            }
            System.out.println("No valid benchmark data received, keeping existing chart");
        });

        benchmarkTask.setOnFailed(event -> {
            Throwable throwable = benchmarkTask.getException();
            System.err.println("Benchmark task failed: " + (throwable != null ? throwable.getMessage() : "unknown error"));
            if (throwable != null) {
                throwable.printStackTrace();
            }
        });

        new Thread(benchmarkTask).start();
    }
    
    private void updateChart(String jsonOutput) {
        if (jsonOutput == null || jsonOutput.trim().isEmpty() || jsonOutput.trim().equals("{}")) return;
        
        try {
            Gson gson = new Gson();
            BenchmarkData data = gson.fromJson(jsonOutput, BenchmarkData.class);
            
            if (data == null || data.dates == null || data.portfolio == null || data.nifty == null) return;
            
            javafx.application.Platform.runLater(() -> {
                try {
                    benchmarkChart.getData().clear();
                    
                    javafx.scene.chart.XYChart.Series<String, Number> portfolioSeries = new javafx.scene.chart.XYChart.Series<>();
                    portfolioSeries.setName("My Portfolio");
                    
                    javafx.scene.chart.XYChart.Series<String, Number> niftySeries = new javafx.scene.chart.XYChart.Series<>();
                    niftySeries.setName("Nifty 50 (Benchmark)");
                    
                    int len = Math.min(data.dates.size(), Math.min(data.portfolio.size(), data.nifty.size()));
                    for (int i = 0; i < len; i++) {
                        String date = data.dates.get(i);
                        portfolioSeries.getData().add(new javafx.scene.chart.XYChart.Data<>(date, data.portfolio.get(i)));
                        niftySeries.getData().add(new javafx.scene.chart.XYChart.Data<>(date, data.nifty.get(i)));
                    }
                    
                    benchmarkChart.getData().addAll(portfolioSeries, niftySeries);
                    styleBenchmarkSeries(portfolioSeries, niftySeries);
                } catch (Exception e) {
                    System.err.println("Error updating chart UI: " + e.getMessage());
                }
            });
        } catch (Exception e) {
            System.err.println("Chart update error: " + e.getMessage());
        }
    }

    private static class BenchmarkData {
        java.util.List<String> dates;
        java.util.List<Double> nifty;
        java.util.List<Double> portfolio;
    }
    
    @FXML private void handleLogout() {
        UserSession session = UserSession.getInstance();
        session.logout();
        
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource("/fxml/LoginView.fxml"));
            totalInvestedLabel.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @FXML private void handleGoToProfile() { updateNavSelection(btnProfile); showProfile(); }
    @FXML private void handleGoToDashboard() { updateNavSelection(btnDashboard); showDashboard(); }
    @FXML private void handleGoToPortfolio() { updateNavSelection(btnPortfolio); navigate("/fxml/Onboarding.fxml"); }
    @FXML private void handleGoToHistory() { updateNavSelection(btnHistory); navigate("/fxml/History.fxml"); }
    @FXML private void handleGoToSettings() { updateNavSelection(btnSettings); navigate("/fxml/Settings.fxml"); }

    private void showProfile() {
        if (profileDetailsCard != null && dashboardContent != null) {
            profileDetailsCard.setVisible(true);
            profileDetailsCard.setManaged(true);
            dashboardContent.setVisible(false);
            dashboardContent.setManaged(false);
        }
    }

    private void showDashboard() {
        if (profileDetailsCard != null && dashboardContent != null) {
            profileDetailsCard.setVisible(false);
            profileDetailsCard.setManaged(false);
            dashboardContent.setVisible(true);
            dashboardContent.setManaged(true);
        }
    }

    private void navigate(String path) {
        try {
            javafx.scene.Parent root = javafx.fxml.FXMLLoader.load(getClass().getResource(path));
            totalInvestedLabel.getScene().setRoot(root);
        } catch (Exception e) { e.printStackTrace(); }
    }
}


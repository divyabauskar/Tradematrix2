package com.tradematrix;

public class UserSession {
    private static UserSession instance;
    private int userId;
    private String username;
    private String email;
    private String fullName;
    private String mobileNumber;
    private String password;
    private String baseCurrency = "INR";
    private boolean lightMode = false;
    
    // Cache fields
    private java.util.List<Holding> cachedHoldings;
    private String cachedBenchmarkData;
    private long lastCacheTime = 0;
    private static final String CACHE_FILE_PREFIX = "cache_";
    private static final String CACHE_FILE_SUFFIX = ".json";

    private String getCacheFileName() {
        if (userId <= 0) {
            return null;
        }
        return CACHE_FILE_PREFIX + userId + CACHE_FILE_SUFFIX;
    }

    public void saveCacheToDisk() {
        String cacheFileName = getCacheFileName();
        if (cacheFileName == null) {
            return;
        }
        try (java.io.FileWriter writer = new java.io.FileWriter(cacheFileName)) {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            CacheData data = new CacheData(cachedHoldings, cachedBenchmarkData, lastCacheTime);
            gson.toJson(data, writer);
        } catch (Exception e) {
            System.err.println("Failed to save cache: " + e.getMessage());
        }
    }

    public void loadCacheFromDisk() {
        String cacheFileName = getCacheFileName();
        if (cacheFileName == null) {
            return;
        }
        java.io.File file = new java.io.File(cacheFileName);
        if (!file.exists()) return;
        try (java.io.FileReader reader = new java.io.FileReader(file)) {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            CacheData data = gson.fromJson(reader, CacheData.class);
            if (data != null) {
                this.cachedHoldings = data.holdings;
                this.cachedBenchmarkData = data.benchmarkData;
                this.lastCacheTime = data.lastCacheTime;
            }
        } catch (Exception e) {
            System.err.println("Failed to load cache: " + e.getMessage());
        }
    }

    private static class CacheData {
        java.util.List<Holding> holdings;
        String benchmarkData;
        long lastCacheTime;
        CacheData(java.util.List<Holding> h, String b, long t) {
            this.holdings = h;
            this.benchmarkData = b;
            this.lastCacheTime = t;
        }
    }
    
    private UserSession() {}
    
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getBaseCurrency() { return baseCurrency; }
    public void setBaseCurrency(String baseCurrency) { this.baseCurrency = baseCurrency; }
    
    public boolean isLightMode() { return lightMode; }
    public void setLightMode(boolean lightMode) { this.lightMode = lightMode; }
    
    public String formatCurrency(double amount) {
        if ("USD".equalsIgnoreCase(baseCurrency)) {
            return String.format("$%,.2f", amount);
        } else {
            return String.format("₹%,.2f", amount);
        }
    }
    
    public java.util.List<Holding> getCachedHoldings() { return cachedHoldings; }
    public void setCachedHoldings(java.util.List<Holding> cachedHoldings) { this.cachedHoldings = cachedHoldings; }
    
    public String getCachedBenchmarkData() { return cachedBenchmarkData; }
    public void setCachedBenchmarkData(String cachedBenchmarkData) { this.cachedBenchmarkData = cachedBenchmarkData; }
    
    public long getLastCacheTime() { return lastCacheTime; }
    public void setLastCacheTime(long lastCacheTime) { this.lastCacheTime = lastCacheTime; }

    public void logout() {
        userId = 0;
        username = null;
        email = null;
        fullName = null;
        mobileNumber = null;
        password = null;
        baseCurrency = "INR";
        lightMode = false;
        cachedHoldings = null;
        cachedBenchmarkData = null;
        lastCacheTime = 0;
    }
}

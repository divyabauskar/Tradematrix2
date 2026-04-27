# TradeMatrix 📈

TradeMatrix is a professional-grade stock portfolio tracker built with **JavaFX**. It provides real-time analytics, portfolio performance tracking against benchmarks (Nifty 50), and interactive visualizations.

## ✨ Features
- **Live Dashboard**: Real-time tracking of invested value, current value, and P&L.
- **Interactive Charts**:
  - Portfolio Performance vs Nifty 50 (Line Chart).
  - Asset Diversification with hover tooltips (Pie Chart).
- **Theme Support**: Seamless switching between **Dark Mode** and **Light Mode**.
- **Portfolio Management**: Add/Remove stocks with automatic live price fetching.
- **History Log**: Full audit trail of all buy/sell transactions.
- **Secure Authentication**: User registration and login system with persistent sessions.

## 🛠️ Tech Stack
- **Frontend**: JavaFX (FXML + CSS)
- **Backend**: Java 21
- **Database**: MySQL (local)
- **Analytics**: Python (Yahoo Finance Integration)

## 🚀 Getting Started

### Prerequisites
1. **Java 21+**
2. **MySQL Server**
3. **Python 3.x** with `yfinance` installed:
   ```bash
   pip install yfinance
   ```

### Installation
1. **Clone the repository**:
   ```bash
   git clone https://github.com/divyabauskar/Tradematrix2.git
   cd Tradematrix2
   ```

2. **Database Setup**:
   - Create a database named `tradematrix`.
   - Run the script in `database/schema.sql` to create the tables.
   - Update `DatabaseManager.java` with your MySQL credentials.

3. **Run the Application**:
   ```bash
   mvn clean javafx:run
   ```


Developed by [divyabauskar](https://github.com/divyabauskar)

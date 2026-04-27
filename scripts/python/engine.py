import sys
import os
import json
import datetime
import requests
from dotenv import load_dotenv

# Optional dependency: yfinance
try:
    import yfinance as yf
except ImportError:
    yf = None

try:
    import requests_cache
except ImportError:
    requests_cache = None

session = None
if yf:
    try:
        session = yf.utils.get_cache_session()
        session.headers.update({'User-Agent': 'Mozilla/5.0'})
        if requests_cache:
            requests_cache.install_cache('yf_cache', expire_after=1800)
    except Exception as e:
        print(f"WARNING: Could not create yfinance session: {e}", file=sys.stderr)
        session = None

def parse_json_input(input_str):
    try:
        # Replace single quotes with double quotes if needed (though Java should send double)
        return json.loads(input_str)
    except json.JSONDecodeError as e:
        print(f"ERROR: Invalid JSON input: {str(e)}", file=sys.stderr)
        return None

def fetch_performance(portfolio_data):
    # Generate reactive demo data based on current date and actual portfolio value
    import datetime
    import random

    # Parse portfolio data - can be dict or have current_total_value key
    portfolio_map = portfolio_data if isinstance(portfolio_data, dict) and "AAPL" in str(portfolio_data) else portfolio_data.get("portfolio", {})
    current_total_value = portfolio_data.get("current_total_value", None) if isinstance(portfolio_data, dict) and "current_total_value" in portfolio_data else None

    # If no explicit current_total_value provided, calculate from portfolio_map
    if current_total_value is None:
        total_quantity = sum(portfolio_map.values()) if isinstance(portfolio_map, dict) and portfolio_map else 1.0
        current_total_value = total_quantity * 100.0  # Demo price per unit
    
    # Ensure current_total_value is at least 100 (minimum base)
    if current_total_value < 100:
        current_total_value = 100.0

    # Generate dates for the last 30 days
    dates = []
    today = datetime.date.today()
    for i in range(30):
        date = today - datetime.timedelta(days=29 - i)
        dates.append(date.strftime('%Y-%m-%d'))

    # Portfolio: Work backwards from current_total_value using daily percentage changes
    # Start from current value and apply random daily percentage changes to generate history
    portfolio_raw = []
    current_sim_value = current_total_value
    
    for i in range(29, -1, -1):  # Work backwards from day 29 to day 0
        portfolio_raw.insert(0, current_sim_value)
        # Random daily percentage change (realistic market movement: -2% to +2%)
        if i > 0:  # Not on the last iteration
            daily_change_pct = random.uniform(-0.02, 0.02)
            current_sim_value = current_sim_value * (1 + daily_change_pct)
            current_sim_value = max(current_sim_value, 0.1)  # Ensure positive

    # Normalize portfolio to start at 100
    first_portfolio = portfolio_raw[0]
    portfolio = [(x / first_portfolio) * 100 for x in portfolio_raw]

    # Nifty 50: Start at 100, grow with some volatility and noise
    nifty_raw = []
    nifty_initial = 100.0
    nifty_growth = 1.15  # 15% growth over 30 days
    for i in range(30):
        progress = i / 29.0
        base_value = nifty_initial * (1 + (nifty_growth - 1) * progress)
        noise = random.uniform(-0.03, 0.03) * base_value  # 3% noise for more volatility
        value = base_value + noise
        nifty_raw.append(max(value, 0.1))

    # Normalize Nifty to start at 100
    first_nifty = nifty_raw[0]
    nifty = [(x / first_nifty) * 100 for x in nifty_raw]

    return {
        "dates": dates,
        "portfolio": portfolio,
        "nifty": nifty
    }

def send_sms(mobile, otp):
    env_path = os.path.join(os.path.dirname(__file__), '.env')
    load_dotenv(env_path)
    api_key = os.getenv("FAST2SMS_API_KEY")
    
    if mobile.startswith("+91"):
        mobile = mobile[3:]
        
    if not api_key or api_key == "YOUR_FAST2SMS_KEY_HERE":
        print("ERROR: FAST2SMS API key missing in scripts/python/.env file!")
        print(f"MOCK SMS FALLBACK. OTP for {mobile} is {otp}")
        sys.exit(0)

    url = "https://www.fast2sms.com/dev/bulkV2"
    payload = f"route=q&message=Your TradeMatrix OTP is {otp}&language=english&flash=0&numbers={mobile}"
    headers = {
        'authorization': api_key,
        'Content-Type': "application/x-www-form-urlencoded",
        'Cache-Control': "no-cache",
    }
    
    try:
        response = requests.post(url, data=payload, headers=headers)
        if response.status_code == 200:
            print("OTP Sent successfully via Fast2SMS API!")
        else:
            print(f"Failed to send SMS (API Error): {response.text}")
    except Exception as e:
        print(f"Failed to send SMS (Network Exception): {str(e)}")

if __name__ == "__main__":
    if len(sys.argv) == 2:
        # Check if argument is JSON (portfolio) or mobile (SMS requires 2 args)
        arg = sys.argv[1]
        if arg.startswith("{"):
            portfolio = parse_json_input(arg)
            if portfolio is not None:
                result = fetch_performance(portfolio)
                print(json.dumps(result))
            sys.exit(0)
        else:
            print("Usage: python engine.py <portfolio_json> OR python engine.py <mobile> <otp>")
            sys.exit(1)

    if len(sys.argv) == 3:
        mobile = sys.argv[1]
        otp = sys.argv[2]
        send_sms(mobile, otp)
    else:
        print("Usage: python engine.py <portfolio_json> OR python engine.py <mobile> <otp>")
        sys.exit(1)

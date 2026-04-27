import sys
import json
import requests
import datetime
import threading
from dateutil.relativedelta import relativedelta
from concurrent.futures import ThreadPoolExecutor, as_completed

try:
    import requests_cache
except ImportError:
    requests_cache = None

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36'
}

session = requests.Session()
session.headers.update(HEADERS)
if requests_cache:
    try:
        requests_cache.install_cache('yf_cache', expire_after=1800)
    except Exception:
        pass


def fetch_chart(symbol, interval='1d', timeout=10):
    """Fetch chart data for a single symbol with timeout"""
    try:
        url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?range=1mo&interval={interval}"
        resp = session.get(url, timeout=timeout)
        if resp.status_code != 200:
            print(f"ERROR: Failed to fetch {symbol}: HTTP {resp.status_code}", file=sys.stderr)
            return symbol, None
        
        data = resp.json()
        result = data.get('chart', {}).get('result')
        if not result:
            print(f"ERROR: No chart result for {symbol}", file=sys.stderr)
            return symbol, None
        
        result = result[0]
        timestamps = result.get('timestamp', [])
        indicators = result.get('indicators', {}).get('quote', [])
        
        if not timestamps or not indicators:
            print(f"ERROR: No timestamps/indicators for {symbol}", file=sys.stderr)
            return symbol, None
        
        closes = indicators[0].get('close', [])
        if len(timestamps) != len(closes):
            print(f"ERROR: Timestamp/close mismatch for {symbol}", file=sys.stderr)
            return symbol, None
        
        cleaned = []
        for close in closes:
            cleaned.append(float(close) if close is not None else None)
        
        print(f"Successfully fetched {symbol}: {len(cleaned)} data points", file=sys.stderr)
        return symbol, (timestamps, cleaned)
    except requests.Timeout:
        print(f"ERROR: Timeout fetching {symbol}", file=sys.stderr)
        return symbol, None
    except Exception as e:
        print(f"ERROR: Exception fetching {symbol}: {str(e)}", file=sys.stderr)
        return symbol, None


def normalized_series(values):
    """Normalize values to percentage change from first value"""
    cleaned = []
    last_valid = None
    for v in values:
        if v is not None:
            last_valid = v
            cleaned.append(v)
        else:
            cleaned.append(last_valid)
    first_valid = next((v for v in cleaned if v is not None), None)
    if first_valid is None:
        return [0 for _ in cleaned]
    return [first_valid if v is None else v for v in cleaned]


def fetch_benchmark(tickers_json_str, max_workers=4):
    """Fetch benchmark data with parallel requests"""
    try:
        # Validate input
        print(f"Input received: {tickers_json_str[:100] if tickers_json_str else 'None'}", file=sys.stderr)
        
        if not tickers_json_str or tickers_json_str.strip() == '':
            print("ERROR: Empty input string", file=sys.stderr)
            print(json.dumps({"dates": [], "nifty": [], "portfolio": []}))
            return
        
        # Parse portfolio JSON
        def parse_portfolio_input(input_str):
            try:
                return json.loads(input_str)
            except json.JSONDecodeError as e:
                print(f"ERROR: Invalid JSON input: {str(e)}", file=sys.stderr)
                return {}

        portfolio_map = parse_portfolio_input(tickers_json_str)
        if not portfolio_map or not isinstance(portfolio_map, dict):
            print(f"ERROR: Portfolio map is empty or not a dict", file=sys.stderr)
            print(json.dumps({"dates": [], "nifty": [], "portfolio": []}))
            return

        symbols = list(portfolio_map.keys()) + ['^NSEI']
        print(f"Fetching data for {len(symbols)} symbols: {symbols}", file=sys.stderr)
        
        chart_data = {}
        common_dates = None
        
        # Fetch all symbols in parallel
        with ThreadPoolExecutor(max_workers=max_workers) as executor:
            futures = {executor.submit(fetch_chart, symbol): symbol for symbol in symbols}
            
            for future in as_completed(futures, timeout=12):
                try:
                    symbol, result = future.result()
                    if result:
                        timestamps, closes = result
                        if common_dates is None:
                            common_dates = [datetime.datetime.utcfromtimestamp(ts).strftime('%Y-%m-%d') for ts in timestamps]
                        chart_data[symbol] = normalized_series(closes)
                        print(f"Added data for {symbol}", file=sys.stderr)
                    else:
                        print(f"Failed to fetch {symbol}, skipping", file=sys.stderr)
                except Exception as e:
                    print(f"ERROR in future result: {str(e)}", file=sys.stderr)
        
        # Validate we have critical data
        if not chart_data:
            print("ERROR: No chart data collected", file=sys.stderr)
            print(json.dumps({"dates": [], "nifty": [], "portfolio": []}))
            return
            
        if '^NSEI' not in chart_data:
            print("ERROR: Nifty 50 data not available", file=sys.stderr)
            print(json.dumps({"dates": [], "nifty": [], "portfolio": []}))
            return
            
        if not common_dates:
            print("ERROR: No common dates found", file=sys.stderr)
            print(json.dumps({"dates": [], "nifty": [], "portfolio": []}))
            return

        dates = common_dates
        nifty_raw = chart_data.get('^NSEI', [0] * len(dates))
        portfolio_raw = []

        print(f"Computing portfolio values from {len(portfolio_map)} holdings", file=sys.stderr)
        
        for i in range(len(dates)):
            p_val = 0
            for t, q in portfolio_map.items():
                symbol_values = chart_data.get(t)
                if symbol_values and i < len(symbol_values) and symbol_values[i] is not None:
                    p_val += float(symbol_values[i]) * float(q)
            portfolio_raw.append(p_val)

        n_start = next((x for x in nifty_raw if x and x > 0), 1)
        p_start = next((x for x in portfolio_raw if x and x > 0), 1)
        
        print(f"Nifty start: {n_start}, Portfolio start: {p_start}", file=sys.stderr)

        nifty = [(x / n_start) * 100 if x and n_start else 0 for x in nifty_raw]
        portfolio = [(x / p_start) * 100 if x and p_start else 0 for x in portfolio_raw]
        
        result = {
            "dates": dates,
            "nifty": nifty,
            "portfolio": portfolio
        }
        
        print(f"Final result: {len(dates)} dates, {len(nifty)} nifty points, {len(portfolio)} portfolio points", file=sys.stderr)
        print(json.dumps(result))
        
    except Exception as e:
        print(f"ERROR: Unexpected error in fetch_benchmark: {str(e)}", file=sys.stderr)
        import traceback
        traceback.print_exc(file=sys.stderr)
        print(json.dumps({"dates": [], "nifty": [], "portfolio": []}))


if __name__ == "__main__":
    if len(sys.argv) > 1:
        fetch_benchmark(sys.argv[1])
    else:
        print(json.dumps({}))

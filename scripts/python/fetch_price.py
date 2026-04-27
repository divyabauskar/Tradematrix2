import sys
import json
import requests

HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36'
}


def query_yahoo_price(symbol):
    url = f"https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?range=5d&interval=1d"
    resp = requests.get(url, headers=HEADERS, timeout=15)
    if resp.status_code != 200:
        return None
    data = resp.json()
    result = data.get('chart', {}).get('result')
    if not result:
        return None
    result = result[0]
    meta = result.get('meta', {})
    price = meta.get('regularMarketPrice')
    if price is None:
        quote = result.get('indicators', {}).get('quote', [{}])[0]
        closes = quote.get('close', [])
        for value in reversed(closes):
            if value is not None:
                price = float(value)
                break
    return price


def fetch_prices(tickers_str):
    ticker_list = [t.strip() for t in tickers_str.split(',') if t.strip()]
    if not ticker_list:
        print("{}")
        return

    result = {}
    for ticker in ticker_list:
        try:
            price = query_yahoo_price(ticker)
            if price is not None:
                result[ticker] = float(price)
        except Exception:
            pass

    print(json.dumps(result))


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("{}")
        sys.exit(1)
    fetch_prices(sys.argv[1])

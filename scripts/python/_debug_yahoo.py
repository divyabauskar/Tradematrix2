import requests

headers = {'User-Agent': 'Mozilla/5.0'}

for url in [
    'https://query1.finance.yahoo.com/v7/finance/quote?symbols=AAPL',
    'https://query1.finance.yahoo.com/v8/finance/chart/AAPL?range=1mo&interval=1d'
]:
    try:
        r = requests.get(url, headers=headers, timeout=15)
        print('URL:', url)
        print('STATUS:', r.status_code)
        print('BODY:', r.text[:1000])
    except Exception as e:
        print('URL:', url, 'ERROR:', repr(e))

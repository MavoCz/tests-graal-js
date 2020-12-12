(function fetch() {
    const rates = client.get('https://api.coincap.io/v2/rates');
    console.log(rates.status);
    const assets = client.get('https://api.coincap.io/v2/assets');
    console.log(assets.status);

    return {
        rates: rates.json(),
        assets: assets.json()
    }
})



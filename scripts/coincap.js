
function fetchRates() {
    var response = client.get('/v2/rates');
    console.log(response.status);
    var ratesJson = JSON.parse(response.data) ;
    var btcRates = ratesJson["data"].filter(record => record["symbol"] === "BTC");
    store.save("rates", btcRates);

    var assets = client.get('/v2/assets');
    store.save("assets", assets.json()); // third parameter is root path
}

({
    "rates": fetchRates
})



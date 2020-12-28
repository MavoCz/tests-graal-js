(async function fetch() {
    const company = await client.get(config.get("baseurl") + '/company/info/doesnotexist');
    console.log(company.status);
    const ceoList = await client.get(config.get("baseurl") + '/company/ceo');
    console.log(ceoList.status);

    return {
        company: company.json(),
        ceos: ceoList.json()
    }
})

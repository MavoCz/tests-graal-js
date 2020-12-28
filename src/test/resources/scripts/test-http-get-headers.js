(async function fetch() {
    const company = await client.get(config.get("baseurl") + '/company/info', {
        headers: {a: 'valuea', b: 'valueb'}
    });

    return {
        data: company.json(),
        status: company.status,
        headers: company.headers,
        mediaType: company.mediaType
    }
})
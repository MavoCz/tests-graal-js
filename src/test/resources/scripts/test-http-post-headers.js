(async function fetch() {
    const company = await client.post(config.get("baseurl") + '/company/add', { data: 'valuedata' }, {
        headers: {a: 'valuea', b: 'valueb'}
    });

    return {
        data: company.json(),
        status: company.status,
        headers: company.headers,
        mediaType: company.mediaType
    }
})
function promiseTimeout(time) {
    return new Promise(function (resolve, reject) {
        setTimeout(function () {
            resolve(time);
        }, time);
    });
}

(async function test() {
    return await timeout.ms(100, [
        {
            "id": 1,
            "name": "Steve Jobs"
        },
        {
            "id": 2,
            "name": "Bob Balmer"
        }
    ]);
})


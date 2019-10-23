function getDate(date) {
    let millis = Date.parse(date);
    return new Date(millis).toUTCString()
}
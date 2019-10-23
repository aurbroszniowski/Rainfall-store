function formatDates() {
    $('.time_stamp').each(function() {
        let val =  $(this).html();
        let millis = Date.parse(val);
        let date = getDate(millis);
        $(this).html(date);
        $(this).removeClass('time_stamp').addClass('date');
    });
}

function getDate(millis) {
    return new Date(millis * 1000).toUTCString()
}
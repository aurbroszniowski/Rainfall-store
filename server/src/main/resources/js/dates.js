function formatDates() {
    $('.time_stamp').each(function() {
        var millis = $(this).html();
        var date = getDate(millis);
        $(this).html(date);
        $(this).removeClass('time_stamp').addClass('date');
    });
}

function getDate(millis) {
    return new Date(millis * 1000).toUTCString()
}
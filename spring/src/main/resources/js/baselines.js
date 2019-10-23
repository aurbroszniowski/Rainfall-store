function toggleBaseline(evt) {
    let source = event.target || event.srcElement;
    let checked = source.checked;
    let ok = confirm("Do you want to permanently change the baseline status to " + checked + "?");
    if (ok) {
        let ID = source.getAttribute('data-id');
        let url = getBaselineStatusURL(ID);
        $.ajax({
            'type': 'POST',
            'url': url,
            'contentType': 'application/json',
            'data': JSON.stringify(checked),
            'dataType': 'json',
        });
    } else {
        evt.preventDefault();
    }
}
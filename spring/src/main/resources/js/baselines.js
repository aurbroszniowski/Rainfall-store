function toggleBaseline(o) {
    let checked = o.checked;
    let ok = confirm("Do you want to permanently change the baseline status to " + checked + "?");
    if (ok) {
        let ID = o.getAttribute('data-id');
        let url = getBaselineStatusURL(ID);
        $.post(url, JSON.stringify(checked));
    } else {
        evt.preventDefault();
    }
}
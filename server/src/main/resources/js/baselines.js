function setBaselineChecks() {
    $('.baseline-check').each(setBaselineCheck);
    $('.baseline-check').click(toggleBaseline);
}

function setBaselineCheck() {
    let baseline = 'true' == $(this).attr("data-baseline");
    $(this).prop("checked", baseline);
}

function toggleBaseline(evt) {
    let checked = $(this).prop('checked');
    let ok = confirm("Do you want to permanently change the baseline status?");
    if (ok) {
        let ID = $(this).attr("data-id");
        let url = getBaselineStatusURL(ID);
        $.post(url, checked.toString());
    } else {
        evt.preventDefault();
    }
}
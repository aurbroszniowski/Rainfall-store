var base = '/performance'

function setHrefs() {
    $('.homelink').each(setBase);
    $('.idlink').each(setHref);
    $('.parentlink').each(setHref);
}

function setBase() {
    $(this).attr("href", base);
}

function setHref() {
    var id = $(this).attr("data-id");
    var funcname = $(this).attr("data-fun");
    var url = eval(funcname + "('" + id + "')");
    $(this).attr("href", url);
}

function getRunsFormURL(ID) {
    return base + "/cases/" + ID + "/runs #run_table"
}

function getRunsListURL(ID) {
    return base + "/cases/" + ID + "/runs/json"
}

function getRunURL(ID) {
    return base + "/runs/" + ID;
}

function getBaselineStatusURL(ID) {
    return base + "/runs/" + ID + "/baseline";
}

function getJobURL(ID) {
    return base + "/jobs/" + ID;
}

function getJobsTableURL(ID) {
    return base + "/runs/" + ID + "/jobs #jobs_table"
}

function getStatsURL(ID) {
    return base + "/stats/" + ID;
}

function getStatsTableURL(ID) {
    return base + "/runs/" + ID + "/stats #stats_table"
}

function getJobStatsTableURL(ID, host) {
    return base + "/runs/" + ID + "/stats/" + host + " #stats_table"
}

function getOutputsURL(ID) {
    return base + "/jobs/" + ID + "/outputs";
}

function getOutputData(ID, callback) {
    let url = getOutputUrl(ID);
    return getData(url, callback);
}

function getHdrData(ID, callback) {
    let url = getOutputUrl(ID) + "/hdr";
    return $.getJSON(url, callback);
}

function getAggregateHdrData(ID, op, callback) {
    let url = getRunURL(ID) + "/aggregate/" + op;
    return $.getJSON(url, callback);
}

function getComparativeHdrData(IDs, op, callback) {
    let url = base + "/compare/" + IDs.join("-") + "/" + op;
    return $.getJSON(url, callback);
}

function getOutputUrl(ID) {
    return base + "/outputs/" + ID;
}

function getOperationsForRunURL(ID) {
    return base + "/runs/" + ID + "/operations";
}

function getCommonOperationsForRunsURL(IDs) {
    return base + "/runs/" + IDs.join("-") + "/common-operations";
}

function getComparisonURL(IDs) {
    return base + "/compare/" + IDs.join("-");
}

function getData(url, callback) {
    return $.ajax({
        mimeType: 'text/plain',
        dataType: "text",
        cache: true,
        url: url,
        success: callback
    });
}

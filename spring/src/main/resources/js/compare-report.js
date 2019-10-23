function reportComparison(runRecs) {
    let IDs = runRecs.map(rec => rec.ID);
    loadCommonOps(IDs, commonOps => reportCommonOps(runRecs, commonOps))
}

function loadCommonOps(IDs, fun) {
    $(".btn").prop('disabled', true);
    startSpinner("#op-spin");
    let url = getCommonOperationsForRunsURL(IDs);
    $.getJSON(url, fun);
}

function reportCommonOps(runRecs, commonOps) {
    if (commonOps.length > 0) {
        let runIDs = runRecs.map(runRec => runRec.ID);
        $.each(commonOps, (i, op) => {
            addTabs(op);
            getComparativeHdrData(runIDs, op, comparison => {
                let hdrMap = new Map(Object.entries(comparison.runs));
                for (rec of runRecs) {
                    let runID = rec.ID;
                    let hdrData = hdrMap.get(runID.toString());
                    let date = getDate(rec.date);
                    reportPlots(hdrData, op, date, runID);
                }
                let pvalues = new Map(Object.entries(comparison.pvalues));
                reportPvalues(op, pvalues, runRecs);
            });
        });
    } else {
        warn("No common operations: nothing to compare!");
    }
}

function addTabs(op) {
    addTab(op, "tps");
    addTab(op, "response-time");
    ['MEDIAN', '_99', '_99_99', 'MAX']
        .forEach(p => addTab(op, p));
    addPercentilesTab(op);
    startSpinner(`.${op}-spin`);
}

function reportPlots(hdrData, op, date, runID) {
    graphTps(hdrData, op, false, date);
    graphResponseTime(hdrData, op, false, date);
    graphTimedPercentiles(hdrData, op, false, date);
    graphPercentiles(hdrData, op, date, runID);
}

function graphTimedPercentiles(hdrData, op, realTimes, name) {
    ['MEDIAN', '_99', '_99_99', 'MAX']
        .forEach(p => graphTimedPercentile(hdrData, op, p, name));
}

function graphTimedPercentile(hdrData, op, percentile, name) {
    let divId = addTab(op, percentile);

    var layout = {
        title: op,
        xaxis: xaxis(false),
        yaxis: {title: 'Response time ' + percentileLabel(percentile) + ' (ms)'}
    };

    var traces = {
        x: xtimes(hdrData.startTimes, false),
        y: hdrData.timedPercentiles[percentile],
        name: name,
        type: 'scatter',
        mode: 'lines',
        line: {width: '1'}
    };

    var data = [traces];

    Plotly.plot(divId, data, layout, {showLink: false});
    stopSpinner(`#${op}-${percentile}-spin`);
}

function percentileLabel(percentile) {
    switch(percentile) {
        case 'MEDIAN': return 'medians';
        case '_99': return '99th percentiles';
        case '_99_99': return '99.99th percentiles';
        case 'MAX': return 'maximums';
    }
}

function graphPercentiles(hdrData, op, name, runID) {
    let divId = addPercentilesTab(op);

    var traces = {
        x: hdrData.percentilePoints,
        y: hdrData.percentileValues,
        name: name,
        type: 'scatter',
        line: {width: '1'}
    };

    addRoundedPercentiles(divId, hdrData, name, runID);

    var data = [traces];

    var layout = {
        title: op,
        xaxis: xaxis(false),
        yaxis: {title: 'Response Time percentiles distribution (ms)'}
    };

    Plotly.plot(divId + "_graph", data, layout, {showLink: false});
    stopSpinner(`#${op}-percentiles-spin`);
}

function addPercentilesTab(op) {
    type = 'percentiles';
    let divId = op + type;
    if (!exists(divId)) {
        $('#percentiles-ul').append(`<li><a href="#${divId}">${op}  ${tabSpinner(op, type)}</a></li>`);

        let tabs = $("#percentiles-tabs");
        tabs.append(`<div id='${divId}'></div>`);

        let div = $(`#${divId}`);

        let graphId = divId + "_graph";
        div.append(`<div id='${graphId}' style='height: 550px;width: 1200px;'></div>`);

        let footerId = divId + "_footer";
        div.append(`<div id='${footerId}'></div>`);

        $(`#${footerId}`).append('<p><h3>Percentiles per run:<h3></p>');

        let tableId = divId + "_table";
        let table = `<table id='${tableId}' border="1" cellpadding="5"><tr><th>Run date</th>`;
        table += ['MEDIAN', '_99', '_99_99', 'MAX']
            .map(percentileName)
            .map(p => `<th>${p}</th>`)
            .reduce((acc, cur) => acc + cur);
        table += '</tr></table>';
        $(`#${footerId}`).append(table);

        tabs.tabs("refresh");
        tabs.tabs( "option", "active", 0 );
    }
    return divId;
}

function addRoundedPercentiles(divId, hdrData, name, runID) {
    let row = `<tr><td>${name}</td>`;
    row += ['MEDIAN', '_99', '_99_99', 'MAX']
            .map(percentile => hdrData.roundedPercentiles[percentile] / 1000)
            .map(val => `<td>${val}</td>`)
            .reduce((acc, cur) => acc + cur);
    row += '</tr>';

    let tableId = "#" + divId + "_table";
    $(tableId).append(row);
}

function reportPvalues(op, pvalues, runRecs) {
    let divId = addPercentilesTab(op);
    let div = $("#" + divId + "_footer");

    div.append('<br>');
    div.append('<p><h3>Kolmogorov-Smirnov test p-values for percentile distributions in pairs of runs:<h3></p>');

    let table = '<table border="1" cellpadding="5">';
    table += '<tr> <th>Run 1</th> <th>Run 2</th> <th>P-value</th> <tr>';
    pvalues.forEach((pvalue, spair) => {
        let pair = $.parseJSON(spair);
        let date1 = getDateByID(runRecs, pair[0]);
        let date2 = getDateByID(runRecs, pair[1]);
        table += `<tr> <td>${date1}</td> <td>${date2}</td> <td>${pvalue}</td> </tr>`;
    });
    table += "</table>";

    div.append(table);
}

function getDateByID(runRecs, ID) {
    let rec = runRecs.filter(rec => rec.ID === ID)[0];
    return getDate(rec.date)
}


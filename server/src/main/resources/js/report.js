function reportJob(jobID) {
    $.getJSON(getOutputsURL(jobID), function(list) {
        $.each(list, function( i ) {
            var rec = list[i];
            var op = rec.value.operation;
            addTabs(op);
            getHdrData(rec.ID, function(hdrData) {
                reportAll(hdrData, op);
            });
        });
    });
}

function reportAggregate(runID) {
    $.getJSON(getOperationsForRunURL(runID), function(ops) {
        $.each( ops, function( i, op ) {
            addTabs(op);
            getAggregateHdrData(runID, op, function(hdrData) {
                reportAll(hdrData, op);
            });
        });
    });
}

function warn(msg) {
    console.log(msg);
    alert(msg);
}

function addTabs(op) {
    addTab(op, "tps");
    addTab(op, "response-time");
    addTab(op, "timed-percentiles");
    addTab(op, "percentiles");
    startSpinner(`.${op}-spin`);
}

function reportAll(hdrData, op) {
    graphTps(hdrData, op, true, op);
    graphResponseTime(hdrData, op, true, op);
    graphTimedPercentiles(hdrData, op, true, op);
    graphPercentiles(hdrData, op, op);
}

function graphTps(hdrData, op, realTimes, name) {
    let divId = addTab(op, "tps");

    var traces = {
        x: xtimes(hdrData.startTimes, realTimes),
        y: hdrData.tps,
        name: name,
        type: 'scatter',
        mode: 'lines',
        line: {width: '1'}
    };

    var data = [traces];

    var layout = {
        title: op,
        xaxis: xaxis(realTimes),
        yaxis: {title: 'TPS'}
    };

    Plotly.plot(divId, data, layout, {showLink: false});
    stopSpinner(`#${op}-tps-spin`);
}

function graphResponseTime(hdrData, op, realTimes, name) {
    let divId = addTab(op, "response-time");

    var traces = {
        x: xtimes(hdrData.startTimes, realTimes),
        y: hdrData.means,
        name: name,
        error_y: {
             type: 'data',
             array: hdrData.errors,
             visible: true
           },
        type: 'scatter',
        mode: 'lines',
        line: {width: '1'}
    };

    var data = [traces];

    var layout = {
        title: op,
        xaxis: xaxis(realTimes),
        yaxis: {title: 'Response time means and st. devs (ms)'}
    };

    Plotly.plot(divId, data, layout, {showLink: false});
    stopSpinner(`#${op}-response-time-spin`);
}

function graphTimedPercentiles(hdrData, op, realTimes, name) {
    let divId = addTab(op, "timed-percentiles");

    var layout = {
        title: op,
        xaxis: xaxis(realTimes),
        yaxis: {title: 'Response time percentiles (ms)'}
    };

    for (percentile of ['MAX', '_99_99', '_99', 'MEDIAN' ]) {

        var traces = {
            x: xtimes(hdrData.startTimes, realTimes),
            y: hdrData.timedPercentiles[percentile],
            name: percentileName(percentile),
            type: 'scatter',
            mode: 'lines',
            line: {width: '1'}
        };

        var data = [traces];

        Plotly.plot(divId, data, layout, {showLink: false});
        stopSpinner(`#${op}-timed-percentiles-spin`);
    }
}

function percentileName(percentile) {
    switch(percentile) {
        case 'MEDIAN': return 'Median';
        case '_99': return '99%';
        case '_99_99': return '99.99%';
        case 'MAX': return 'Max';
    }
}

function graphPercentiles(hdrData, op, name) {
    let divId = addTab(op, "percentiles");

    var traces = {
        x: hdrData.percentilePoints,
        y: hdrData.percentileValues,
        name: name,
        type: 'scatter',
        fill: 'tozeroy',
        line: {width: '1'}
    };

    addRoundedPercentileLabels($("#" + divId), hdrData, name);

    var data = [traces];

    var layout = {
        title: op,
        xaxis: xaxis(false),
        yaxis: {title: 'Response Time percentiles distribution (ms)'}
    };

    Plotly.plot(divId, data, layout, {showLink: false});
    stopSpinner(`#${op}-percentiles-spin`);
}

function addRoundedPercentileLabels(div, hdrData) {
    ['MEDIAN', '_99', '_99_99', 'MAX']
        .map(p => labelRoundedPercentile(p, hdrData))
        .forEach(label => div.append(label));
}

function labelRoundedPercentile(percentile, hdrData) {
    let name = percentileName(percentile);
    let value = hdrData.roundedPercentiles[percentile] / 1000;
    return `<span style='margin-left: 30px;'>${name}: ${value} &micro;s</span>`;
}

function addTab(op, type) {
    let divId = op + '_' + type;
    if (!exists(divId)) {
        let ul_ref = `#${type}-ul`;
        let li = `<li><a href="#${divId}">${op}  ${tabSpinner(op, type)}</a></li>`;
        $(ul_ref).append(li);

        let tabs_ref = `#${type}-tabs`;
        let tab = `<div id='${divId}' style='height: 550px;width: 1200px;'></div>`;
        $(tabs_ref).append(tab);

        $(tabs_ref).tabs("refresh");
        $(tabs_ref).tabs( "option", "active", 0 );
    }
    return divId;
}

function tabSpinner(op, type) {
    return `<span class='${op}-spin' id='${op}-${type}-spin'></span>`;
}

function exists(ID) {
    return $('#' + ID).length;
}

function xtimes(times, real) {
    return real ? times : null;
}

function xaxis(real) {
    return real
        ? {
            title: 'Time',
            type: 'date'
        }
        : {
            type: 'linear',
            autorange: true,
            title: 'Time',
            tickmode: 'auto',
            nticks: 20
        }
}

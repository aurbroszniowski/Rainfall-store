function reportTps(filename, title) {
    $.ajax({
        mimeType: 'text/plain; charset=x-user-defined',
        dataType: "text",
        cache: false,
        url : filename + ".hlog",
        success : function(result) {
            graphTps(result, filename, title);
        }
    });
}

function graphTps(histograms, filename, title) {
    var divId = filename + 'tps';
    $("#tps-box").append("<div id='" + divId + "' style='height: 550px;width: 1200px;'><div class='title'/><div class='graph'/></div>");
    var reader = new hdr.HistogramLogReader(histograms);
    var histogram;

    var traces = {
        x: [],
        y: [],
        name: title,
        type: 'scatter',
        mode: 'lines+markers',
        line: {width: '1'}
    };

    while ((histogram = reader.nextIntervalHistogram()) != null) {
        var durationInMs = histogram.endTimeStampMsec - histogram.startTimeStampMsec;
        var tps = 1000 * histogram.getTotalCount() / durationInMs;

        traces['x'].push(new Date(histogram.startTimeStampMsec).toISOString());
        traces['y'].push(tps);
    }

    var data = [traces];

    var layout = {
        title: title,
        xaxis: {title: 'Time', type: 'date'},
        yaxis: {title: 'TPS'}
    };

    Plotly.newPlot(divId, data, layout, {showLink: false});
}

function reportResponseTime(filename, title) {
    $.ajax({
        mimeType: 'text/plain; charset=x-user-defined',
        dataType: "text",
        cache: false,
        url : filename + ".hlog",
        success : function(result) {
            graphResponseTime(result, filename, title);
        }
    });
}

function graphResponseTime(histograms, filename, title) {
    var divId = filename + 'rt';
    $("#response-time-box").append("<div id='" + divId + "' style='height: 550px;width: 1200px;'><div class='title'/><div class='graph'/></div>");
    var reader = new hdr.HistogramLogReader(histograms);
    var histogram;

    var traces = {
        x: [],
        y: [],
        name: title,
        error_y: {
             type: 'data',
             array: [],
             visible: true
           },
        type: 'scatter',
        mode: 'lines+markers',
        line: {width: '1'}
    };

    while ((histogram = reader.nextIntervalHistogram()) != null) {
        var durationInMs = histogram.endTimeStampMsec - histogram.startTimeStampMsec;
        var mean = histogram.getMean() / 1000000;

        traces['x'].push(new Date(histogram.startTimeStampMsec).toISOString());
        traces['y'].push(mean);

        traces['error_y']['array'].push((histogram.getStdDeviation()/1000000));
    }

    var data = [traces];

    var layout = {
        title: title,
        xaxis: {title: 'Time', type: 'date'},
        yaxis: {title: 'Response time (ms)'}
    };

    Plotly.newPlot(divId, data, layout, {showLink: false});
}

function reportPercentiles(filename, title) {
//        $("#" + filename + 'pct' + " .title").after("Max Response Time = " + (Math.round((max / 1000000) * 1000) / 1000) + " ms<br/>");
//        $("#" + filename + 'pct' + " .title").after("Mean Response Time = " + (Math.round((mean / 1000000) * 1000) / 1000) + " ms<br/>");
    $.ajax({
        mimeType: 'text/plain; charset=x-user-defined',
        dataType: "text",
        cache: false,
        url : filename + ".hlog",
        success : function(result) {
            graphPercentiles(result, filename, title);
        }
    });
}

function graphPercentiles(histograms, filename, title) {
    var divId = filename + 'pct';
    $("#percentiles-box").append("<div id='" + divId + "' style='height: 550px;width: 1200px;'><div class='title'/><div class='graph'/></div>");
    var reader = new hdr.HistogramLogReader(histograms);
    var histogram;

    var traces = {
        x: [],
        y: [],
        name: title,
        type: 'scatter',
        fill: 'tozeroy',
        line: {width: '1'}
    };

    var accumulatedHistogram = hdr.build();

    while ((histogram = reader.nextIntervalHistogram()) != null) {
        accumulatedHistogram.add(histogram);
    }

    $("#percentiles-box").append("<span style='margin-left: 30px;'>Median : " + accumulatedHistogram.getValueAtPercentile(50) / 1000 + " &micro;s</span>");
    $("#percentiles-box").append("<span style='margin-left: 30px;'>99th : " + accumulatedHistogram.getValueAtPercentile(99) / 1000 + " &micro;s</span>");
    $("#percentiles-box").append("<span style='margin-left: 30px;'>99.9th : " + accumulatedHistogram.getValueAtPercentile(99.99) / 1000 + " &micro;s</span>");
    $("#percentiles-box").append("<span style='margin-left: 30px;'>Max : " + accumulatedHistogram.maxValue / 1000 + " &micro;s</span>");

    var histoOutput = accumulatedHistogram.outputPercentileDistribution();
    var lines = histoOutput.split("\n");

    for (var i = 0; i < lines.length; i++) {
         var line = lines[i].trim();
         var values = line.trim().split(/[ ]+/);
         if (line[0] != '#' && values.length == 4) {
             var y = parseFloat(values[0]) / 1000000;
             var x = parseFloat(values[1]);
             console.log(x + ',' + y);

             if (!isNaN(x) && !isNaN(y)) {
                traces['x'].push(x);
                traces['y'].push(y);
             }
         }
    }

    var data = [traces];

    var layout = {
        title: title,
        xaxis: {
            type: 'linear',
            autorange: true,
            title: 'Time',
            tickmode: 'auto',
            nticks: 20
        },
        yaxis: {title: 'Response Time percentiles distribution (ms)'}
    };

    Plotly.newPlot(divId, data, layout, {showLink: false});
}


$(document).ready(function () {
//!report!
    reportTps('ADD', 'TPS for ADD');
    reportResponseTime('ADD', 'Periodic Response Time for ADD');
    reportPercentiles('ADD', 'Response Time percentiles for ADD');
    reportTps('ALREADY_PRESENT', 'TPS for ALREADY_PRESENT');
    reportResponseTime('ALREADY_PRESENT', 'Periodic Response Time for ALREADY_PRESENT');
    reportPercentiles('ALREADY_PRESENT', 'Response Time percentiles for ALREADY_PRESENT');
});
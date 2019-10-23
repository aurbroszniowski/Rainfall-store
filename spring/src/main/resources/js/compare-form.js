function selectComparison() {
    let testsBox = $("#tests");
    testsBox.on("change", event => {
        let testName = event.currentTarget.value;
        if (testName) {
            loadRuns(testName);
        }
    });
    $("#runs").on("change", setAddBtn);
    $("#included-runs").on("change", setRemoveBtn);
    $("#del-btn").click(removeSelectedRows);
    $("#clear-btn").click(clearSelectedRows);
    setCommonOps();
}

function loadRuns(testName) {
    let url = getRunsListURL(testName);
    $.ajax({
        mimeType: 'application/json',
        cache: false,
        url: url,
        success: runs => showRuns(runs)
    });
}

function showRuns(runs) {
    let runsBox = $("#runs");
    runsBox.empty();
    if (runs.length > 0) {
        let runsByDateReverse = runs.sort(
            (a, b) => b.created - a.created
        );
        $.each(runsByDateReverse, i => {
            let rec = runs[i];
            let ID = rec.id;
            let date = getDate(rec.created);
            let row = runRow(ID, date, rec.value.status, rec.value.baseline);
            runsBox.append(row);
        });
        setAddBtn();
    } else {
        let row = "<option disabled value='xxx'> -- No test runs yet -- </option>";
        runsBox.append(row);
        setAddBtn();
    }
}

function runRow(ID, date, status, baseline) {
    let color = colorOf(status);
    let fw = baseline ? 'bold' : 'normal';
    let txt_dec = baseline ? 'text-decoration: underline;' : '';
    let html = `<option
                    value= "${ID}"
                    data-status=${status}
                    style="color:${color}; font-weight: ${fw}; ${txt_dec}"
                    title="Status: ${status.toLowerCase()}"
                >
                    ${date}
                </option>`;
    return $.parseHTML(html);
}

function colorOf(status) {
    switch(status) {
        case "COMPLETE": return "blue";
        case "FAILED": return "red";
        case "UNKNOWN": return "gray";
        default: return "green";
    }
}

function setAddBtn() {
    let btn = $("#add-btn");
    btn.off("click");
    let selectedRows = $.map(
        $('#runs option:selected'),
        e => runRow(e.value, e.text, e.getAttribute("data-status"))
    )
    if (selectedRows.length > 0) {
        btn.prop('disabled', false);
        btn.click(event => {
            event.preventDefault();
            addRuns(selectedRows);
        });
    } else {
        btn.prop('disabled', true);
    }
}

function addRuns(rows) {
    let includedRuns = $("#included-runs");
    let includedIds = new Set(inlcudedRunIds());
    for (row of rows) {
        if (!includedIds.has(row.value)) {
            includedRuns.append(row);
        }
    }
    clearSelection($("#runs"));
    setAddBtn();
    setClearBtn();
    setCommonOps();
}

function inlcudedRunIds() {
    return $.map(
        $('#included-runs option'),
        e => e.value
    );
}

function clearSelection(list) {
    list.find('option').attr("selected", false) ;
}

function setRemoveBtn() {
    let includedRuns = $("#included-runs option:selected");
    $("#del-btn").prop('disabled', includedRuns.length == 0);
}

function setClearBtn() {
    let includedRuns = $("#included-runs option");
    $("#clear-btn").prop('disabled', includedRuns.length == 0);
}

function removeSelectedRows(event) {
    event.preventDefault();
    $("#included-runs option:selected").remove();
    setRemoveBtn();
    setCommonOps();
}

function clearSelectedRows(event) {
    event.preventDefault();
    $("#included-runs").empty();
    setRemoveBtn();
    setClearBtn();
    setCommonOps();
}

function setCommonOps() {
    opsField = $("#common-ops");
    opsField.val("");

    let includedIds = inlcudedRunIds();
    if (includedIds.length > 0) {
        loadCommonOps(includedIds, commonOps => {
            let joined = commonOps.join(", ");
            opsField.val(joined);
            stopSpinner("#op-spin");
            setRunBtn(includedIds, commonOps);
        });
    }
}

function setRunBtn(IDs, ops) {
    let btn = $("#compare-btn");
    btn.off("click");
    if (IDs.length > 1 && ops.length > 0) {
        btn.prop('disabled', false);
        btn.click(event => {
            event.preventDefault();
            compare(IDs);
        });
    } else {
        btn.prop('disabled', true);
    }
}

function compare(IDs) {
    var url = getComparisonURL(IDs);
    window.open(url);
}

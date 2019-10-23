let spinnerClass = "fa fa-spinner fa-spin";

function startSpinner(selector) {
    $(selector).addClass(spinnerClass);
}

function stopSpinner(selector) {
    $(selector).removeClass(spinnerClass);
}


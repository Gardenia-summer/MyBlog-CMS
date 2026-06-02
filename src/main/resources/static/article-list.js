(function () {
    function isInteractiveElement(target) {
        return Boolean(target.closest("a, button, input, textarea, select, summary, details, form"));
    }

    document.addEventListener("click", function (event) {
        var article = event.target.closest(".clickable-article[data-href]");
        if (!article || isInteractiveElement(event.target)) {
            return;
        }
        window.location.href = article.dataset.href;
    });

    document.addEventListener("keydown", function (event) {
        if (event.key !== "Enter") {
            return;
        }
        var article = event.target.closest(".clickable-article[data-href]");
        if (!article || event.target !== article) {
            return;
        }
        window.location.href = article.dataset.href;
    });
})();

(function () {
    function isInteractiveElement(target) {
        // 文章卡片整行可点，但作者、标签、按钮、表单等内部控件保留自己的行为。
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

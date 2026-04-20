@import gg.jte.Content
@param String pageTitle
@param Content content
@param String flash = null
@param String flashType = null

<!doctype html>
<html lang="ru">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${pageTitle}</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<nav class="navbar navbar-expand-lg navbar-light bg-white border-bottom mb-5">
    <div class="container">
        <a class="navbar-brand" href="/">Анализатор страниц</a>
        <div class="navbar-nav">
            <a class="nav-link" href="/">Главная</a>
            <a class="nav-link" href="/urls">Сайты</a>
        </div>
    </div>
</nav>
<main class="container">
    @if (flash != null)
        <div class="row mb-3">
            <div class="col-12">
                <div class="alert alert-${flashType}" role="alert">
                    ${flash}
                </div>
            </div>
        </div>
    @endif
    ${content}
</main>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
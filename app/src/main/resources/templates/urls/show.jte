@import hexlet.code.model.Url
@import hexlet.code.model.UrlCheck
@import hexlet.code.util.StringUtils
@param Url url
@param java.util.List<UrlCheck> checks
@param String flash = null
@param String flashType = null

@template.layout.main(
    pageTitle = "Сайт",
    flash = flash,
    flashType = flashType,
    content = @`
        <h1 class="mb-5">Сайт: ${url.getName()}</h1>

        <table class="table table-bordered table-hover mb-5" data-test="url">
            <tbody>
                <tr>
                    <td>ID</td>
                    <td>${url.getId()}</td>
                </tr>
                <tr>
                    <td>Имя</td>
                    <td>${url.getName()}</td>
                </tr>
                <tr>
                    <td>Дата создания</td>
                    <td>${url.getCreatedAt().toLocalDate().toString()}</td>
                </tr>
            </tbody>
        </table>

        <div class="mb-5">
            <form method="post" action="/urls/${url.getId()}/checks">
                <input type="submit" class="btn btn-primary" value="Запустить проверку">
            </form>
        </div>

        <h2 class="mb-3">Проверки</h2>

        <table class="table table-bordered table-hover" data-test="checks">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Код ответа</th>
                    <th>h1</th>
                    <th>title</th>
                    <th>description</th>
                    <th>Дата создания</th>
                </tr>
            </thead>
            <tbody>
                @for (UrlCheck check : checks)
                    <tr>
                        <td>${check.getId()}</td>
                        <td>${check.getStatusCode()}</td>
                        <td>${StringUtils.truncate(check.getH1())}</td>
                        <td>${StringUtils.truncate(check.getTitle())}</td>
                        <td>${StringUtils.truncate(check.getDescription())}</td>
                        <td>${check.getCreatedAt().toLocalDate().toString()}</td>
                    </tr>
                @endfor
            </tbody>
        </table>
    `
)
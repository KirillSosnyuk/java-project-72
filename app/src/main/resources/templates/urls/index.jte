@import hexlet.code.model.Url
@import hexlet.code.model.UrlCheck
@param java.util.List<Url> urls
@param java.util.Map<Long, UrlCheck> latestChecks
@param String flash = null
@param String flashType = null

@template.layout.main(
    pageTitle = "Сайты",
    flash = flash,
    flashType = flashType,
    content = @`
        <h1 class="mb-5">Сайты</h1>
        <table class="table table-bordered table-hover" data-test="urls">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Имя</th>
                    <th>Дата последней проверки</th>
                    <th>Код ответа</th>
                </tr>
            </thead>
            <tbody>
                @for (Url url : urls)
                    <tr>
                        <td>${url.getId()}</td>
                        <td><a href="/urls/${url.getId()}">${url.getName()}</a></td>
                        <td>
                            @if (latestChecks.get(url.getId()) != null)
                                ${latestChecks.get(url.getId()).getCreatedAt().toLocalDate().toString()}
                            @endif
                        </td>
                        <td>
                            @if (latestChecks.get(url.getId()) != null)
                                ${latestChecks.get(url.getId()).getStatusCode()}
                            @endif
                        </td>
                    </tr>
                @endfor
            </tbody>
        </table>
    `
)
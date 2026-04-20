package hexlet.code.controller;

import hexlet.code.model.Url;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.NamedRoutes;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;
import kong.unirest.core.Unirest;
import org.jsoup.Jsoup;

import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class UrlController {
    private static final String FLASH_KEY = "flash";

    private static final String FLASH_TYPE_KEY = "flashType";

    public static void index(Context ctx) {
        var model = baseModel(ctx);
        model.put("url", "");
        ctx.render("index.jte", model);
    }

    public static void create(Context ctx) throws Exception {
        var inputUrl = ctx.formParam("url");

        String normalizedUrl;
        try {
            normalizedUrl = normalizeUrl(inputUrl);
        } catch (Exception e) {
            ctx.status(422);
            var model = baseModel(ctx);
            model.put("url", inputUrl == null ? "" : inputUrl);
            model.put("flash", "Некорректный URL");
            model.put("flashType", "danger");
            ctx.render("index.jte", model);
            return;
        }

        var existingUrl = UrlRepository.findByName(normalizedUrl);

        if (existingUrl.isPresent()) {
            var savedUrl = existingUrl.orElseThrow();
            setFlash(ctx, "Страница уже существует", "info");
            ctx.redirect(NamedRoutes.urlPath(savedUrl.getId()));
            return;
        }

        var url = new Url(normalizedUrl);
        UrlRepository.save(url);
        setFlash(ctx, "Страница успешно добавлена", "success");
        ctx.redirect(NamedRoutes.urlPath(url.getId()));
    }

    public static void list(Context ctx) throws Exception {
        var urls = UrlRepository.getEntities();
        var latestChecks = UrlCheckRepository.findLatestChecks();

        var model = baseModel(ctx);
        model.put("urls", urls);
        model.put("latestChecks", latestChecks);
        ctx.render("urls/index.jte", model);
    }

    public static void show(Context ctx) throws Exception {
        var id = ctx.pathParamAsClass("id", Long.class).get();
        var url = UrlRepository.find(id).orElseThrow(() -> new NotFoundResponse("Page not found"));

        var model = baseModel(ctx);
        model.put("url", url);
        model.put("checks", UrlCheckRepository.findByUrlId(id));
        ctx.render("urls/show.jte", model);
    }

    public static void check(Context ctx) throws Exception {
        var id = ctx.pathParamAsClass("id", Long.class).get();
        var url = UrlRepository.find(id).orElseThrow(() -> new NotFoundResponse("Page not found"));

        try {
            var response = Unirest.get(url.getName()).asString();

            if (response.getStatus() >= 400) {
                setFlash(ctx, "Произошла ошибка при проверке", "danger");
                ctx.redirect(NamedRoutes.urlPath(id));
                return;
            }

            var document = Jsoup.parse(response.getBody());

            var h1Element = document.selectFirst("h1");
            var h1 = h1Element == null ? "" : h1Element.text();

            var title = document.title();

            var descriptionElement = document.selectFirst("meta[name=description]");
            var description = descriptionElement == null ? "" : descriptionElement.attr("content");

            var urlCheck = new hexlet.code.model.UrlCheck(id, response.getStatus(), h1, title, description);
            UrlCheckRepository.save(urlCheck);

            setFlash(ctx, "Страница успешно проверена", "success");
        } catch (Exception e) {
            setFlash(ctx, "Произошла ошибка при проверке", "danger");
        }

        ctx.redirect(NamedRoutes.urlPath(id));
    }

    private static String normalizeUrl(String rawUrl) throws Exception {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("URL is blank");
        }

        URL url = new URI(rawUrl.trim()).toURL();
        var protocol = url.getProtocol();
        var host = url.getHost();
        var port = url.getPort();

        if (protocol == null || protocol.isBlank() || host == null || host.isBlank()) {
            throw new IllegalArgumentException("Invalid URL");
        }

        return port == -1
                ? String.format("%s://%s", protocol, host)
                : String.format("%s://%s:%d", protocol, host, port);
    }

    private static void setFlash(Context ctx, String message, String type) {
        ctx.sessionAttribute(FLASH_KEY, message);
        ctx.sessionAttribute(FLASH_TYPE_KEY, type);
    }

    private static Map<String, Object> baseModel(Context ctx) {
        var model = new HashMap<String, Object>();
        model.put("flash", popSessionAttribute(ctx, FLASH_KEY));
        model.put("flashType", popSessionAttribute(ctx, FLASH_TYPE_KEY));
        return model;
    }

    private static String popSessionAttribute(Context ctx, String key) {
        String value = ctx.sessionAttribute(key);

        if (value != null) {
            ctx.req().getSession().removeAttribute(key);
        }

        return value;
    }
}

package hexlet.code;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import hexlet.code.controller.UrlController;
import hexlet.code.repository.BaseRepository;
import hexlet.code.util.DataSourceFactory;
import hexlet.code.util.DatabaseInitializer;
import hexlet.code.util.NamedRoutes;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinJte;

public class App {
    private static final int DEFAULT_PORT = 7070;

    private static final String DEFAULT_HOST = "0.0.0.0";

    public static Javalin getApp() throws Exception {
        var dataSource = DataSourceFactory.getDataSource();
        DatabaseInitializer.init(dataSource);
        BaseRepository.setDataSource(dataSource);

        return Javalin.create(config -> {
            config.bundledPlugins.enableDevLogging();
            config.fileRenderer(new JavalinJte(createTemplateEngine()));

            config.routes.get(NamedRoutes.rootPath(), UrlController::index);
            config.routes.post(NamedRoutes.urlsPath(), UrlController::create);
            config.routes.get(NamedRoutes.urlsPath(), UrlController::list);
            config.routes.get(NamedRoutes.urlPath("{id}"), UrlController::show);
            config.routes.post(NamedRoutes.urlChecksPath("{id}"), UrlController::check);
        });
    }

    public static void main(String[] args) throws Exception {
        var app = getApp();
        app.start(DEFAULT_HOST, getPort());
    }

    private static TemplateEngine createTemplateEngine() {
        var classLoader = App.class.getClassLoader();
        var codeResolver = new ResourceCodeResolver("templates", classLoader);
        return TemplateEngine.create(codeResolver, ContentType.Html);
    }

    private static int getPort() {
        var port = System.getenv("PORT");
        return port == null ? DEFAULT_PORT : Integer.parseInt(port);
    }
}

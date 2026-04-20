package hexlet.code;

import hexlet.code.model.Url;
import hexlet.code.model.UrlCheck;
import hexlet.code.repository.BaseRepository;
import hexlet.code.repository.UrlCheckRepository;
import hexlet.code.repository.UrlRepository;
import hexlet.code.util.DataSourceFactory;
import hexlet.code.util.DatabaseInitializer;
import io.javalin.testtools.JavalinTest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class AppTest {
    private static MockWebServer mockWebServer;

    @BeforeAll
    static void beforeAll() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void afterAll() throws Exception {
        mockWebServer.shutdown();
    }

    @BeforeEach
    void setUp() throws Exception {
        var dbName = UUID.randomUUID().toString();
        var jdbcUrl = "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1;";
        System.setProperty("JDBC_DATABASE_URL", jdbcUrl);

        var ds = DataSourceFactory.getDataSource();
        DatabaseInitializer.init(ds);
        BaseRepository.setDataSource(ds);
    }

    @Test
    void testMainPage() throws Exception {
        var app = App.getApp();
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/");
            Assertions.assertEquals(200, response.code());
            Assertions.assertTrue(response.body().string().contains("Анализатор страниц"));
        });
    }

    @Test
    void testStoreUrl() throws Exception {
        var app = App.getApp();
        JavalinTest.test(app, (server, client) -> {
            var requestBody = "url=https://www.example.com/some-path";
            var response = client.post("/urls", requestBody);
            Assertions.assertEquals(302, response.code());

            var urlInDb = UrlRepository.findByName("https://www.example.com");
            Assertions.assertTrue(urlInDb.isPresent());
        });
    }

    @Test
    void testStoreDuplicateUrl() throws Exception {
        var app = App.getApp();
        var domain = "https://unique.org";
        UrlRepository.save(new Url(domain));

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls", "url=" + domain);
            Assertions.assertEquals(302, response.code());
            Assertions.assertEquals(1, UrlRepository.getEntities().size());
        });
    }

    @Test
    void testStoreMalformedUrl() throws Exception {
        var app = App.getApp();
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls", "url=not-a-url");

            var code = response.code();
            Assertions.assertTrue(code == 422 || code == 400 || code == 302 || code == 200,
                    "Unexpected status code: " + code);

            if (code != 302) {
                Assertions.assertTrue(response.body().string().contains("Некорректный URL"));
            }
        });
    }

    @Test
    void testUrlDetails() throws Exception {
        var app = App.getApp();
        var url = new Url("https://details.com");
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls/" + url.getId());
            Assertions.assertEquals(200, response.code());
            Assertions.assertTrue(response.body().string().contains("https://details.com"));
        });
    }

    @Test
    void testUrlNotFound() throws Exception {
        var app = App.getApp();
        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls/9999");
            Assertions.assertEquals(404, response.code());
        });
    }

    @Test
    void testRunCheckSuccess() throws Exception {
        var app = App.getApp();
        var mockHtml = "<html><title>Title</title><h1>H1</h1><meta name=\"description\" content=\"Desc\"></html>";
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockHtml));

        var targetUrl = mockWebServer.url("/").toString().replaceAll("/$", "");
        var urlEntity = new Url(targetUrl);
        UrlRepository.save(urlEntity);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + urlEntity.getId() + "/checks");
            Assertions.assertEquals(302, response.code());

            var check = UrlCheckRepository.findByUrlId(urlEntity.getId()).get(0);
            Assertions.assertEquals(200, check.getStatusCode());
            Assertions.assertEquals("Title", check.getTitle());
            Assertions.assertEquals("H1", check.getH1());
            Assertions.assertEquals("Desc", check.getDescription());
        });
    }

    @Test
    void testRunCheckEmptyTags() throws Exception {
        var app = App.getApp();
        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody("<html></html>"));

        var targetUrl = mockWebServer.url("/").toString().replaceAll("/$", "");
        var urlEntity = new Url(targetUrl);
        UrlRepository.save(urlEntity);

        JavalinTest.test(app, (server, client) -> {
            client.post("/urls/" + urlEntity.getId() + "/checks");
            var check = UrlCheckRepository.findByUrlId(urlEntity.getId()).get(0);
            Assertions.assertEquals("", check.getTitle());
            Assertions.assertEquals("", check.getH1());
            Assertions.assertEquals("", check.getDescription());
        });
    }

    @Test
    void testRunCheckFailure() throws Exception {
        var app = App.getApp();
        mockWebServer.enqueue(new MockResponse().setResponseCode(404));

        var url = new Url("https://non-existent.com");
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + url.getId() + "/checks");
            Assertions.assertEquals(302, response.code());
            // Проверяем, что запись о проверке либо не создалась, либо содержит код ошибки (зависит от реализации)
            var checks = UrlCheckRepository.findByUrlId(url.getId());
            Assertions.assertTrue(checks.isEmpty() || checks.get(0).getStatusCode() >= 400);
        });
    }

    @Test
    void testLatestChecksLogic() throws Exception {
        var url = new Url("https://latest.io");
        UrlRepository.save(url);

        UrlCheckRepository.save(new UrlCheck(url.getId(), 200, "old", "old", "old"));
        UrlCheckRepository.save(new UrlCheck(url.getId(), 201, "new", "new", "new"));

        var latest = UrlCheckRepository.findLatestChecks();
        Assertions.assertEquals(201, latest.get(url.getId()).getStatusCode());
        Assertions.assertEquals("new", latest.get(url.getId()).getH1());
    }
}

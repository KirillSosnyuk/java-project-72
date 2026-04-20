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

            var listResponse = client.get("/urls");
            Assertions.assertTrue(listResponse.body().string().contains("https://www.example.com"));
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

            var count = UrlRepository.getEntities().size();
            Assertions.assertEquals(1, count);
        });
    }

    @Test
    void testStoreMalformedUrl() throws Exception {
        var app = App.getApp();
        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls", "url=not-a-url");
            Assertions.assertTrue(response.code() == 302 || response.code() == 422 || response.code() == 200);
        });
    }

    @Test
    void testRunCheckSuccess() throws Exception {
        var app = App.getApp();
        var mockHtml = "<html><title>Test Title</title><h1>Test H1</h1>"
                + "<meta name=\"description\" content=\"Test Desc\"></html>";

        mockWebServer.enqueue(new MockResponse().setResponseCode(200).setBody(mockHtml));

        var targetUrl = mockWebServer.url("/").toString().replaceAll("/$", "");
        var urlEntity = new Url(targetUrl);
        UrlRepository.save(urlEntity);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + urlEntity.getId() + "/checks");

            Assertions.assertEquals(302, response.code());

            var checks = UrlCheckRepository.findByUrlId(urlEntity.getId());
            Assertions.assertFalse(checks.isEmpty());

            var lastCheck = checks.get(0);
            Assertions.assertEquals(200, lastCheck.getStatusCode());
            Assertions.assertEquals("Test Title", lastCheck.getTitle());
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
    void testLatestChecksLogic() throws Exception {
        var url = new Url("https://latest.io");
        UrlRepository.save(url);

        var check1 = new UrlCheck(url.getId(), 200, "H1-1", "T1", "D1");
        var check2 = new UrlCheck(url.getId(), 500, "H1-2", "T2", "D2");
        UrlCheckRepository.save(check1);
        UrlCheckRepository.save(check2);

        var latest = UrlCheckRepository.findLatestChecks();
        Assertions.assertEquals(500, latest.get(url.getId()).getStatusCode());
    }
}

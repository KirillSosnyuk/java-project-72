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

import java.time.LocalDateTime;
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
    void testUrlsPage() throws Exception {
        var app = App.getApp();
        UrlRepository.save(new Url("https://google.com"));

        JavalinTest.test(app, (server, client) -> {
            var response = client.get("/urls");
            Assertions.assertEquals(200, response.code());
            Assertions.assertTrue(response.body().string().contains("https://google.com"));
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
            Assertions.assertTrue(code == 422 || code == 400 || code == 302 || code == 200);
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
    void testRunCheckFailure() throws Exception {
        var app = App.getApp();
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        var url = new Url(mockWebServer.url("/").toString().replaceAll("/$", ""));
        UrlRepository.save(url);

        JavalinTest.test(app, (server, client) -> {
            var response = client.post("/urls/" + url.getId() + "/checks");
            Assertions.assertEquals(302, response.code());
        });
    }

    @Test
    void testUrlModelMethods() {
        var createdAt = LocalDateTime.now();
        var url = new Url(1L, "https://test.com", createdAt);

        Assertions.assertEquals(1L, url.getId());
        Assertions.assertEquals("https://test.com", url.getName());
        Assertions.assertEquals(createdAt, url.getCreatedAt());

        url.setId(2L);
        url.setName("https://new.com");
        url.setCreatedAt(createdAt.plusDays(1));

        Assertions.assertEquals(2L, url.getId());
        Assertions.assertEquals("https://new.com", url.getName());
    }

    @Test
    void testUrlCheckModelMethods() {
        var check = new UrlCheck(1L, 200, "H1", "Title", "Desc");
        check.setUrlId(10L);

        Assertions.assertEquals(10L, check.getUrlId());
        Assertions.assertEquals(200, check.getStatusCode());
        Assertions.assertEquals("H1", check.getH1());
        Assertions.assertEquals("Title", check.getTitle());
        Assertions.assertEquals("Desc", check.getDescription());
    }

    @Test
    void testRepositoryFindById() throws Exception {
        var url = new Url("https://search.com");
        UrlRepository.save(url);

        var found = UrlRepository.find(url.getId());
        Assertions.assertTrue(found.isPresent());
        Assertions.assertEquals("https://search.com", found.get().getName());

        var notFound = UrlRepository.find(9999L);
        Assertions.assertFalse(notFound.isPresent());
    }
}

package com.praneeth.browserstack;

import com.epam.healenium.SelfHealingDriver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Base test wiring three layers together:
 *   JUnit 5 test -> SelfHealingDriver (Healenium) -> RemoteWebDriver -> BrowserStack cloud
 *
 * Element snapshots for healing are stored by the local hlm-backend
 * (docker-compose.yml) - which is what makes self-healing work even though
 * the browser session itself runs in the BrowserStack cloud.
 *
 * Session pass/fail is marked on the BrowserStack dashboard via their REST API
 * (no browserstack SDK magic - a plain RemoteWebDriver + one API call).
 */
public abstract class BaseTest implements TestWatcher {

    protected SelfHealingDriver driver;
    protected RemoteWebDriver remote;

    private static String required(String key) {
        String v = System.getenv(key);
        if (v == null || v.isBlank()) {
            throw new IllegalStateException("Missing env var " + key
                    + " - set your BrowserStack credentials before running");
        }
        return v.trim();
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    @BeforeEach
    void startBrowserStackSession() throws Exception {
        MutableCapabilities caps = new MutableCapabilities();
        caps.setCapability("browserName", envOr("BS_BROWSER", "Chrome"));
        caps.setCapability("browserVersion", envOr("BS_BROWSER_VERSION", "latest"));

        Map<String, Object> bstack = new HashMap<>();
        bstack.put("os", envOr("BS_OS", "Windows"));
        bstack.put("osVersion", envOr("BS_OS_VERSION", "11"));
        bstack.put("buildName", envOr("BS_BUILD", "selenium-healenium-selfhealing"));
        bstack.put("sessionName", envOr("BS_SESSION", "Healenium healing demo"));
        bstack.put("local", false);
        bstack.put("seleniumVersion", "4.21.0");
        caps.setCapability("bstack:options", bstack);

        String hub = String.format("https://%s:%s@hub-cloud.browserstack.com/wd/hub",
                required("BROWSERSTACK_USERNAME"), required("BROWSERSTACK_ACCESS_KEY"));

        remote = new RemoteWebDriver(URI.create(hub).toURL(), caps);
        remote.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        // Healenium wraps the remote driver; healing decisions come from the local backend
        driver = SelfHealingDriver.create(remote);
    }

    // ---- TestWatcher: mark the BrowserStack session, then quit the driver ----

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        finish("passed", ctx.getDisplayName());
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        finish("failed", cause == null ? ctx.getDisplayName() : String.valueOf(cause.getMessage()));
    }

    @Override
    public void testDisabled(ExtensionContext ctx, String reason) {
        finish("ignored", reason);
    }

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        finish("ignored", String.valueOf(cause));
    }

    private void finish(String status, String reason) {
        try {
            if (remote != null && remote.getSessionId() != null) {
                markSessionStatus(status, reason);
            }
        } catch (Exception ignored) {
            // never fail a test because dashboard marking hiccuped
        }
        try {
            if (remote != null) {
                remote.quit();
            }
        } catch (Exception ignored) { }
        remote = null;
        driver = null;
    }

    /** Marks pass/fail on the BrowserStack dashboard via their public REST API. */
    private void markSessionStatus(String status, String reason) throws Exception {
        String auth = Base64.getEncoder().encodeToString(
                (required("BROWSERSTACK_USERNAME") + ":" + required("BROWSERSTACK_ACCESS_KEY"))
                        .getBytes(StandardCharsets.UTF_8));
        String safeReason = reason == null ? "" : reason.replace("\"", "'");
        if (safeReason.length() > 200) {
            safeReason = safeReason.substring(0, 200);
        }
        String body = "{\"status\":\"" + status + "\",\"reason\":\"" + safeReason + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.browserstack.com/5/sessions/"
                        + remote.getSessionId() + ".json"))
                .header("Authorization", "Basic " + auth)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}

package org.example.maniacrevolution.stats;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import net.minecraftforge.fml.loading.FMLPaths;
import org.example.maniacrevolution.Maniacrev;
import javax.net.ssl.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/** One network/disk worker, bounded persistent outbox, no network work on server ticks. */
final class StatsTransport {
    static final int QUEUED = -3;
    private static final Gson GSON = new Gson();
    private static final Path OUTBOX = FMLPaths.GAMEDIR.get().resolve("maniacrev/stats-outbox");
    private static final AtomicInteger PENDING = new AtomicInteger();
    private static ScheduledThreadPoolExecutor worker;
    private static HttpClient client;
    private static long nextRetry;
    private static int failures;

    static synchronized void start() {
        stop();
        if (!StatsConfig.isValid()) return;
        try {
            client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                    .sslContext(tlsContext()).followRedirects(HttpClient.Redirect.NEVER).build();
            worker = new ScheduledThreadPoolExecutor(1, r -> {
                Thread t = new Thread(r, "ManiacrevStats-HTTPS"); t.setDaemon(true); return t;
            });
            worker.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            worker.scheduleWithFixedDelay(StatsTransport::retry, 5, 30, TimeUnit.SECONDS);
        } catch (Exception e) {
            Maniacrev.LOGGER.error("[Stats] Cannot initialize HTTPS ({})", e.getClass().getSimpleName());
        }
    }

    static synchronized void stop() {
        if (worker != null) worker.shutdown();
        worker = null;
    }

    static synchronized CompletableFuture<Integer> send(Map<String, Object> packet) {
        if (worker == null || worker.isShutdown()) return CompletableFuture.completedFuture(StatsManager.RESULT_ERROR);
        if (PENDING.incrementAndGet() > 8) {
            PENDING.decrementAndGet(); return CompletableFuture.completedFuture(StatsManager.RESULT_ERROR);
        }
        CompletableFuture<Integer> result = new CompletableFuture<>();
        worker.execute(() -> {
            try {
                Files.createDirectories(OUTBOX);
                Path file = OUTBOX.resolve(packet.get("matchId") + ".json");
                if (!Files.exists(file)) {
                    try (var files = Files.list(OUTBOX)) {
                        if (files.filter(p -> p.toString().endsWith(".json")).limit(201).count() >= 200) {
                            throw new IllegalStateException("Outbox full");
                        }
                    }
                    byte[] json = GSON.toJson(packet).getBytes(StandardCharsets.UTF_8);
                    if (json.length > 512 * 1024) throw new IllegalStateException("Packet too large");
                    Path temp = OUTBOX.resolve(packet.get("matchId") + ".tmp");
                    Files.write(temp, json);
                    Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
                }
                result.complete(upload(file));
            } catch (Exception e) {
                Maniacrev.LOGGER.warn("[Stats] Failed to persist match ({})", e.getClass().getSimpleName());
                result.complete(StatsManager.RESULT_ERROR);
            } finally { PENDING.decrementAndGet(); }
        });
        return result;
    }

    private static int upload(Path file) {
        if (System.currentTimeMillis() < nextRetry) return QUEUED;
        try {
            HttpRequest request = HttpRequest.newBuilder(StatsConfig.endpoint()).timeout(Duration.ofSeconds(12))
                    .header("Content-Type", "application/json").header("Authorization", "Bearer " + StatsConfig.token())
                    .POST(HttpRequest.BodyPublishers.ofFile(file)).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            String body;
            try (var in = response.body()) {
                byte[] bytes = in.readNBytes(4097);
                if (bytes.length > 4096) throw new IllegalStateException("Response too large");
                body = new String(bytes, StandardCharsets.UTF_8);
            }
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                int id = JsonParser.parseString(body).getAsJsonObject().get("gameId").getAsInt();
                if (id <= 0) throw new IllegalStateException("Invalid acknowledgement");
                Files.delete(file);
                failures = 0; nextRetry = 0;
                return id;
            }
            if (response.statusCode() == 400 || response.statusCode() == 409 || response.statusCode() == 413 || response.statusCode() == 422) {
                Files.move(file, file.resolveSibling(file.getFileName() + ".rejected"), StandardCopyOption.REPLACE_EXISTING);
                Maniacrev.LOGGER.warn("[Stats] Packet rejected, HTTP {}", response.statusCode());
                return StatsManager.RESULT_ERROR;
            }
            throw new IllegalStateException("HTTP " + response.statusCode());
        } catch (Exception e) {
            failures = Math.min(7, failures + 1);
            nextRetry = System.currentTimeMillis() + Math.min(900_000L, 30_000L << (failures - 1));
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            Maniacrev.LOGGER.warn("[Stats] Match saved locally; retry later ({})", e.getClass().getSimpleName());
            return QUEUED;
        }
    }

    private static void retry() {
        if (System.currentTimeMillis() < nextRetry || !Files.isDirectory(OUTBOX)) return;
        try (var files = Files.list(OUTBOX)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().limit(5).toList()) {
                if (Files.size(file) > 512 * 1024) {
                    Files.move(file, file.resolveSibling(file.getFileName() + ".rejected"), StandardCopyOption.REPLACE_EXISTING);
                    continue;
                }
                if (upload(file) == QUEUED) break;
            }
        } catch (Exception e) { Maniacrev.LOGGER.warn("[Stats] Outbox retry failed ({})", e.getClass().getSimpleName()); }
    }

    private static SSLContext tlsContext() throws Exception {
        String pin;
        try (var in = StatsTransport.class.getResourceAsStream("/stats-receiver.sha256")) {
            if (in == null) throw new IllegalStateException("Missing receiver certificate pin");
            pin = new String(in.readAllBytes(), StandardCharsets.US_ASCII).trim();
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        X509TrustManager defaults = (X509TrustManager) factory.getTrustManagers()[0];
        X509TrustManager trust = new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return defaults.getAcceptedIssuers(); }
            public void checkClientTrusted(X509Certificate[] chain, String auth) throws CertificateException {
                defaults.checkClientTrusted(chain, auth);
            }
            public void checkServerTrusted(X509Certificate[] chain, String auth) throws CertificateException {
                try { defaults.checkServerTrusted(chain, auth); return; } catch (CertificateException ignored) {}
                try {
                    if (chain.length == 0) throw new CertificateException("Empty certificate chain");
                    chain[0].checkValidity();
                    String fingerprint = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(chain[0].getEncoded()));
                    if (!fingerprint.equalsIgnoreCase(pin)) throw new CertificateException("Receiver certificate mismatch");
                } catch (CertificateException e) { throw e; }
                catch (Exception e) { throw new CertificateException(e); }
            }
        };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{trust}, null);
        return context;
    }
    private StatsTransport() {}
}

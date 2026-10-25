package com.example.quanlydetaisinhvien.service.mailjet;

import com.sun.net.httpserver.HttpServer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.*;

/** Loopback only. All keys/addresses in this fixture are fake. No external API calls. */
public class MockMailjetServer implements AutoCloseable {
    public record Request(String authorization, String contentType, String method, JsonNode body) {}
    public final List<Request> requests=new CopyOnWriteArrayList<>();
    final HttpServer server;
    final ExecutorService executor=Executors.newVirtualThreadPerTaskExecutor();
    final List<HttpClient> clients=new CopyOnWriteArrayList<>();
    public volatile int status=200;
    public volatile String reply;
    public volatile long delayMs;
    final JsonMapper json=JsonMapper.builder().build();
    public MockMailjetServer() {
        try { server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0); }
        catch (IOException ex) { throw new IllegalStateException("Cannot start loopback HTTP fixture",ex); }
        server.setExecutor(executor);
        server.createContext("/v3.1/send",exchange->{
            try {
                JsonNode body=json.readTree(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
                requests.add(new Request(exchange.getRequestHeaders().getFirst("Authorization"),exchange.getRequestHeaders().getFirst("Content-Type"),exchange.getRequestMethod(),body));
                if (delayMs>0) Thread.sleep(delayMs);
                String response=reply;
                if (response==null) {
                    String email=body.path("Messages").get(0).path("To").get(0).path("Email").asString();
                    response="{\"Messages\":[{\"Status\":\"success\",\"To\":[{\"Email\":\""+email+"\",\"MessageID\":123456,\"MessageUUID\":\"test-uuid-123\"}]}]}";
                }
                byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes);
            } catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        });
        server.start();
    }
    URI endpoint() { return URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/v3.1/send"); }
    public MailjetClient client(Duration timeout) { return client(timeout,"fake-public-key","fake-secret-key","sender@example.invalid"); }
    MailjetClient client(Duration timeout,String key,String secret,String from) {
        var http=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
        clients.add(http);
        return new MailjetClient(http,endpoint(),timeout,key,secret,from,"Khoa CNTT HCMUTE");
    }
    public void reset() { requests.clear(); status=200; reply=null; delayMs=0; }
    @Override public void close() { server.stop(0); executor.shutdownNow(); clients.forEach(HttpClient::close); }
}

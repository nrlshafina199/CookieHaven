package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import server.model.*;

public class MainServer {
    private static final Map<String, ShoppingCart> SESSIONS = new ConcurrentHashMap<>();
    private static final String SESSION_COOKIE_KEY = "AUTH_SESSION";

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        AdminHandler adminHandler = new AdminHandler();
        server.createContext("/admin/products/api", adminHandler);
        server.createContext("/admin", new AdminHandler());
        server.createContext("/api/cart", new CartAPIServlet());
        server.createContext("/checkout", new CheckoutHandler());
        server.createContext("/api/place-order", new CheckoutHandler());
        server.createContext("/cart.html", new CartPageHandler());
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api", new AuthHandler());


        AuthHandler authHandler = new AuthHandler();
        server.createContext("/api/register", authHandler);
        server.createContext("/api/login", authHandler);
        server.createContext("/logout", authHandler);

        server.createContext("/login.html", new StaticFileHandler());
        server.createContext("/register.html", new StaticFileHandler());
        server.createContext("/my_profile.html", new StaticFileHandler());
        server.createContext("/order_history.html", new StaticFileHandler());
        server.createContext("/order_detail.html", new StaticFileHandler());
        server.createContext("/privacy.html", new StaticFileHandler());
        server.createContext("/terms.html", new StaticFileHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("\n===========================================");
        System.out.println("🍪 CookieHaven Server is Running!");
        System.out.println("Shop URL:  http://localhost:8080/order.html");
        System.out.println("Admin URL: http://localhost:8080/admin");
        System.out.println("Admin Login: admin / 1234");
        System.out.println("===========================================\n");
    }

    public static byte[] readFile(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + path);
        }
        return Files.readAllBytes(Paths.get(path));
    }

    public static ShoppingCart getCart(HttpExchange ex) {
        String sid = getSid(ex);
        return SESSIONS.computeIfAbsent(sid, k -> new ShoppingCart());
    }

    public static void clearCart(HttpExchange ex) {
        SESSIONS.remove(getSid(ex));
    }

    private static String getSid(HttpExchange ex) {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");

        if (cookie != null) {
            String[] cookies = cookie.split(";");
            for (String c : cookies) {
                String[] parts = c.trim().split("=");
                // 2. Use "AUTH_SESSION" to match your AuthHandler
                if (parts.length == 2 && parts[0].equals("AUTH_SESSION")) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    public static Map<String, String> parse(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) return map;

        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                try {
                    String key = java.net.URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String value = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    map.put(key, value);
                } catch (Exception e) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange ex) throws IOException {
            String path = ex.getRequestURI().getPath();

            if (path.equals("/") || path.isEmpty()) {
                path = "/order.html";
            }

            File file = new File("web" + path);

            if (file.exists() && !file.isDirectory()) {
                try {
                    byte[] data = readFile("web" + path);
                    String type = getMimeType(path);

                    ex.getResponseHeaders().add("Content-Type", type);
                    ex.getResponseHeaders().add("X-Frame-Options", "DENY");

                    ex.sendResponseHeaders(200, data.length);
                    ex.getResponseBody().write(data);
                } catch (Exception e) {
                    sendError(ex, 500, "Internal Server Error");
                }
            } else {
                sendError(ex, 404, "404 Not Found: " + path);
            }
            ex.close();
        }

        private String getMimeType(String path) {
            if (path.endsWith(".css")) return "text/css; charset=UTF-8";
            if (path.endsWith(".js")) return "application/javascript; charset=UTF-8";
            if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
            if (path.endsWith(".png")) return "image/png";
            if (path.endsWith(".gif")) return "image/gif";
            return "text/html; charset=UTF-8";
        }

        private void sendError(HttpExchange ex, int code, String msg) throws IOException {
            byte[] response = msg.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(code, response.length);
            ex.getResponseBody().write(response);
        }
    }
}
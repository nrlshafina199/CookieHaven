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
    private static final String SESSION_COOKIE_KEY = "SESSION_ID";

    public static void main(String[] args) throws Exception {
        // Initialize sample products if database is empty
        if (ProductDatabase.getAllProducts().isEmpty()) {
            ProductDatabase.addProduct(new Product(
                    "CHIP01", "Classic Choco Chip", 5.00, 100,
                    "Rich chocolate bits", "Flour, Sugar, Butter", "Dairy"
            ));
            ProductDatabase.addProduct(new Product(
                    "OAT02", "Oatmeal Raisin", 6.50, 80,
                    "Chewy oats with raisins", "Oats, Raisins", "None"
            ));
            ProductDatabase.addProduct(new Product(
                    "PB03", "Peanut Butter Delight", 7.00, 60,
                    "Smooth peanut butter cookies", "Peanut Butter, Flour", "Nuts"
            ));
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Register handlers
        server.createContext("/api/cart", new CartAPIServlet());
        server.createContext("/checkout", new CheckoutHandler());
        server.createContext("/admin", new AdminHandler());
        server.createContext("/cart.html", new CartPageHandler());
        server.createContext("/", new StaticFileHandler());

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
        if (cookie != null && cookie.contains(SESSION_COOKIE_KEY + "=")) {
            String[] parts = cookie.split(SESSION_COOKIE_KEY + "=");
            if (parts.length > 1) {
                return parts[1].split(";")[0];
            }
        }
        String id = UUID.randomUUID().toString();
        ex.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE_KEY + "=" + id + "; Path=/; HttpOnly");
        return id;
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
            if (path.equals("/")) path = "/order.html";

            try {
                byte[] data = readFile("web" + path);

                String type = "text/html; charset=UTF-8";
                if (path.endsWith(".css")) {
                    type = "text/css; charset=UTF-8";
                } else if (path.endsWith(".js")) {
                    type = "application/javascript; charset=UTF-8";
                } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
                    type = "image/jpeg";
                } else if (path.endsWith(".png")) {
                    type = "image/png";
                } else if (path.endsWith(".gif")) {
                    type = "image/gif";
                }

                ex.getResponseHeaders().add("Content-Type", type);
                ex.sendResponseHeaders(200, data.length);
                ex.getResponseBody().write(data);

            } catch (FileNotFoundException e) {
                String err = "404 Not Found: " + path;
                byte[] errBytes = err.getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().add("Content-Type", "text/plain; charset=UTF-8");
                ex.sendResponseHeaders(404, errBytes.length);
                ex.getResponseBody().write(errBytes);
            }
            ex.close();
        }
    }
}
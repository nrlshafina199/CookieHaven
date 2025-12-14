package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import server.model.ShoppingCart;

public class MainServer {

    // --- TEMPORARY SESSION & MODEL MANAGEMENT ---
    private static final Map<String, ShoppingCart> SESSIONS = new ConcurrentHashMap<>();
    private static final String SESSION_COOKIE_KEY = "SESSION_ID";

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // --- HANDLER REGISTRATION ---
        server.createContext("/api/cart", new CartAPIServlet());
        server.createContext("/checkout", new CheckoutHandler()); // Handles POST form submission

        // Explicitly register static/dynamic pages to avoid conflicts
        server.createContext("/checkout.html", new StaticFileHandler());
        server.createContext("/order_confirmation.html", new StaticFileHandler());

        server.createContext("/admin", new AdminHandler());
        server.createContext("/cart.html", new CartPageHandler());

        // Serve all remaining static files from "web" folder (Fallback)
        server.createContext("/", new StaticFileHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("Server running at http://localhost:8080/");
    }

    // Extracted Static Handler - NOW HANDLES DYNAMIC TOTAL FOR CHECKOUT.HTML
    static class StaticFileHandler implements HttpHandler {

        private static final DecimalFormat DF = new DecimalFormat("0.00");

        @Override
        public void handle(HttpExchange ex) throws IOException {
            getSessionId(ex);

            String path = ex.getRequestURI().getPath();
            if (path.equals("/")) path = "/order.html"; // default page

            // --- PATH CONSTRUCTION ---
            File file = new File(System.getProperty("user.dir") + File.separator + "web" + path);
            System.out.println("DEBUG: Trying to load static file at: " + file.getAbsolutePath());

            if (!file.exists() || file.isDirectory()) {
                String notFound = "404 Not Found. File not found at " + file.getAbsolutePath();
                ex.sendResponseHeaders(404, notFound.length());
                ex.getResponseBody().write(notFound.getBytes());
                ex.close();
                return;
            }

            String contentType = getContentType(path);
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());


            if (path.equals("/checkout.html")) {
                ShoppingCart cart = MainServer.getCart(ex);
                double total = cart.calculateTotal();

                String totalString = DF.format(total);
                String htmlContent = new String(data, StandardCharsets.UTF_8);

                // Replace the TOTAL_PLACEHOLDER (must be updated in checkout.html)
                String modifiedContent = htmlContent.replace("TOTAL_PLACEHOLDER", totalString);

                data = modifiedContent.getBytes(StandardCharsets.UTF_8);
                contentType = "text/html";
            }


            ex.getResponseHeaders().add("Content-Type", contentType);
            ex.sendResponseHeaders(200, data.length);
            ex.getResponseBody().write(data);
            ex.close();
        }
    }

    // --- SESSION UTILITY METHODS --- (No changes)
    public static ShoppingCart getCart(HttpExchange exchange) {
        String sessionId = getSessionId(exchange);
        return SESSIONS.computeIfAbsent(sessionId, k -> new ShoppingCart());
    }

    public static void clearCart(HttpExchange exchange) {
        String sessionId = getSessionId(exchange);
        SESSIONS.remove(sessionId);
    }

    private static String getSessionId(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        String sessionId = null;

        if (cookieHeader != null) {
            String[] cookies = cookieHeader.split("; ");
            for (String cookie : cookies) {
                if (cookie.startsWith(SESSION_COOKIE_KEY + "=")) {
                    sessionId = cookie.substring(SESSION_COOKIE_KEY.length() + 1);
                    break;
                }
            }
        }

        if (sessionId == null || !SESSIONS.containsKey(sessionId)) {
            sessionId = UUID.randomUUID().toString();
            exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE_KEY + "=" + sessionId + "; Path=/");
        }

        return sessionId;
    }

    // --- EXISTING HANDLERS (AdminHandler, parse, getContentType all remain the same) ---

    static class AdminHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("orderdata.txt");

            if (!file.exists()) {
                file.createNewFile();
            }
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());

            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        }
    }

    public static Map<String,String> parse(String body){
        Map<String,String> map = new HashMap<>();
        String[] pairs = body.split("&");
        for (String p : pairs) {
            String[] kv = p.split("=");
            if (kv.length == 2) {
                try {
                    String decodedValue = java.net.URLDecoder.decode(kv[1], StandardCharsets.UTF_8.name());
                    map.put(kv[0], decodedValue);
                } catch (UnsupportedEncodingException e) {
                    map.put(kv[0], kv[1]);
                }
            }
        }
        return map;
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".json")) return "application/json";
        return "text/plain";
    }
}
package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import server.model.*;

public class AdminHandler implements HttpHandler {

    private static final String ADMIN_USER = "admin";
    private static final String ADMIN_PASS = "1234";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        try {
            if (path.equals("/admin")) {
                serveLoginPage(exchange);
            } else if (path.equals("/admin/dashboard")) {
                if (!checkAuth(exchange)) return;
                serveDashboard(exchange);
            } else if (path.startsWith("/admin/products")) {
                if (!checkAuth(exchange)) return;
                serveProducts(exchange);
            } else if (path.equals("/admin/orders") || path.equals("/admin/orders/api")) {
                if (!checkAuth(exchange)) return;
                serveOrders(exchange);
            } else if (path.equals("/admin/reports") || path.equals("/admin/reports/api")) {
                if (!checkAuth(exchange)) return;
                serveReports(exchange);
            } else if (path.equals("/admin/logout")) {
                handleLogout(exchange);
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String err = "Internal Server Error: " + e.getMessage();
            exchange.sendResponseHeaders(500, err.length());
            exchange.getResponseBody().write(err.getBytes());
            exchange.close();
        }
    }

    private void serveLoginPage(HttpExchange exchange) throws IOException {
        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            byte[] htmlData = MainServer.readFile("web/admin_login.html");
            sendResponse(exchange, 200, "text/html", htmlData);
        } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = MainServer.parse(body);

            if (ADMIN_USER.equals(data.get("username")) && ADMIN_PASS.equals(data.get("password"))) {
                exchange.getResponseHeaders().add("Set-Cookie", "ADMIN_SESSION=true; Path=/; HttpOnly");
                sendJSON(exchange, true, "Login successful");
            } else {
                sendJSON(exchange, false, "Invalid credentials");
            }
        }
    }

    private void serveDashboard(HttpExchange exchange) throws IOException {
        byte[] htmlData = MainServer.readFile("web/admin_dashboard.html");
        List<Order> allOrders = OrderDatabase.getAllOrders();

        long todayCount = allOrders.stream()
                .filter(o -> isToday(o.getOrderDate()))
                .count();

        double totalSales = allOrders.stream().mapToDouble(Order::getTotal).sum();

        String lowStockNames = ProductDatabase.getAllProducts().stream()
                .filter(p -> p.getStock() < 10)
                .map(Product::getName)
                .collect(Collectors.joining(", "));

        if (lowStockNames.isEmpty()) lowStockNames = "All stocks healthy";

        String htmlStr = new String(htmlData, StandardCharsets.UTF_8)
                .replace("{{TOTAL_SALES}}", String.format("%.2f", totalSales))
                .replace("{{TODAY_ORDERS}}", String.valueOf(todayCount))
                .replace("{{LOW_STOCK}}", lowStockNames);

        sendResponse(exchange, 200, "text/html", htmlStr.getBytes(StandardCharsets.UTF_8));
    }

    private boolean isToday(Date date) {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        return fmt.format(date).equals(fmt.format(new Date()));
    }

    private void serveProducts(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        if (path.equals("/admin/products/api")) {
            List<Product> list = ProductDatabase.getAllProducts();
            String json = "[" + list.stream().map(p ->
                    String.format("{\"id\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"stock\":%d,\"description\":\"%s\",\"ingredients\":\"%s\",\"allergens\":\"%s\"}",
                            escapeJson(p.getId()),
                            escapeJson(p.getName()),
                            p.getPrice(),
                            p.getStock(),
                            escapeJson(p.getDescription()),
                            escapeJson(p.getIngredients()),
                            escapeJson(p.getAllergens()))
            ).collect(Collectors.joining(",")) + "]";
            sendResponse(ex, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
            return;
        }

        if (ex.getRequestMethod().equalsIgnoreCase("POST")) {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = MainServer.parse(body);
            String action = data.get("action");

            if ("add".equals(action)) {
                Product p = new Product(data.get("productId"), data.get("name"),
                        Double.parseDouble(data.get("price")), Integer.parseInt(data.get("stock")),
                        data.get("description"), data.get("ingredients"), data.get("allergens"));
                ProductDatabase.addProduct(p);
                sendJSON(ex, true, "Added");
            } else if ("delete".equals(action)) {
                ProductDatabase.deleteProduct(data.get("productId"));
                sendJSON(ex, true, "Deleted");
            }
        } else {
            byte[] htmlData = MainServer.readFile("web/admin_products.html");
            sendResponse(ex, 200, "text/html", htmlData);
        }
    }

    private void serveOrders(HttpExchange exchange) throws IOException {
        if (exchange.getRequestURI().getPath().endsWith("/api")) {
            List<Order> orders = OrderDatabase.getAllOrders();
            // Simple JSON generation for orders
            String json = "[" + orders.stream().map(o ->
                    String.format("{\"orderId\":%d,\"customerName\":\"%s\",\"total\":%.2f,\"status\":\"%s\"}",
                            o.getOrderId(), escapeJson(o.getCustomerName()), o.getTotal(), o.getStatus())
            ).collect(Collectors.joining(",")) + "]";
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        } else {
            byte[] htmlData = MainServer.readFile("web/admin_orders.html");
            sendResponse(exchange, 200, "text/html", htmlData);
        }
    }

    private void serveReports(HttpExchange exchange) throws IOException {
        if (exchange.getRequestURI().getPath().endsWith("/api")) {
            List<Order> orders = OrderDatabase.getAllOrders();
            Map<String, Double> monthlySales = new TreeMap<>();
            Map<String, Integer> productSales = new HashMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("MMM yyyy");

            for (Order o : orders) {
                String month = sdf.format(o.getOrderDate());
                monthlySales.put(month, monthlySales.getOrDefault(month, 0.0) + o.getTotal());
                for (CartItem item : o.getItems()) {
                    productSales.put(item.getProductName(), productSales.getOrDefault(item.getProductName(), 0) + item.getQuantity());
                }
            }

            String json = String.format("{\"monthlySales\":%s,\"productSales\":%s}",
                    mapToJson(monthlySales), mapToJson(productSales));
            sendResponse(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
        } else {
            byte[] htmlData = MainServer.readFile("web/admin_reports.html");
            sendResponse(exchange, 200, "text/html", htmlData);
        }
    }

    private String mapToJson(Map<String, ?> map) {
        return "{" + map.entrySet().stream()
                .map(e -> "\"" + e.getKey() + "\":" + (e.getValue() instanceof String ? "\"" + e.getValue() + "\"" : e.getValue()))
                .collect(Collectors.joining(",")) + "}";
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Set-Cookie", "ADMIN_SESSION=; Path=/; Max-Age=0; HttpOnly");
        exchange.getResponseHeaders().add("Location", "/admin");
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private boolean checkAuth(HttpExchange ex) throws IOException {
        String cookie = ex.getRequestHeaders().getFirst("Cookie");
        if (cookie != null && cookie.contains("ADMIN_SESSION=true")) return true;
        ex.getResponseHeaders().add("Location", "/admin");
        ex.sendResponseHeaders(302, -1);
        ex.close();
        return false;
    }

    private void sendJSON(HttpExchange ex, boolean success, String msg) throws IOException {
        String json = String.format("{\"success\":%b,\"message\":\"%s\"}", success, escapeJson(msg));
        sendResponse(ex, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    private void sendResponse(HttpExchange ex, int code, String type, byte[] content) throws IOException {
        ex.getResponseHeaders().add("Content-Type", type + "; charset=UTF-8");
        ex.sendResponseHeaders(code, content.length);
        ex.getResponseBody().write(content);
        ex.close();
    }

    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"");
    }
}
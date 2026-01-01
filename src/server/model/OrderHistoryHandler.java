package server.model;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import server.MainServer;

public class OrderHistoryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            // 1. Get the username from the query (e.g., ?user=Arin)
            String query = exchange.getRequestURI().getQuery();
            String username = null;

            if (query != null && query.contains("user=")) {
                Map<String, String> params = MainServer.parse(query);
                username = params.get("user");
            }

            if (username == null) {
                sendResponse(exchange, 400, "[]"); // No user specified
                return;
            }

            // 2. Fetch ALL orders from the real database
            List<Order> allOrders = OrderDatabase.getAllOrders();

            // 3. Filter: Keep only orders for THIS customer
            String finalUsername = username;
            List<Order> myOrders = allOrders.stream()
                    .filter(o -> o.getCustomerName().equalsIgnoreCase(finalUsername))
                    .collect(Collectors.toList());

            // 4. Convert to JSON
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < myOrders.size(); i++) {
                Order o = myOrders.get(i);
                json.append(String.format("{\"id\":%d, \"date\":\"%s\", \"total\":%.2f, \"status\":\"%s\"}",
                        o.getOrderId(),
                        o.getOrderDate().toString(),
                        o.getTotal(),
                        o.getStatus()));

                if (i < myOrders.size() - 1) json.append(",");
            }
            json.append("]");

            sendResponse(exchange, 200, json.toString());
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    private void sendResponse(HttpExchange ex, int code, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}

package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import server.model.ShoppingCart;

public class CartAPIServlet implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 1. Get the user's cart from the session (will create if session is new)
        ShoppingCart cart = MainServer.getCart(exchange);

        // --- HANDLER FOR GET REQUEST (To fetch initial count) ---
        if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            String jsonResponse = String.format("{\"success\": true, \"cartCount\": %d}", cart.getTotalItems());
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, jsonResponse.length());
            exchange.getResponseBody().write(jsonResponse.getBytes());
            exchange.close();
            return; // Exit after serving GET request
        }

        // --- ONLY PROCEED WITH POST FOR ADD/UPDATE/DELETE ---
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1); // Method not allowed for non-GET, non-POST
            return;
        }

        // 2. Read the request body (POST data)
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> data = MainServer.parse(body);

        // CRITICAL CHECK: Ensure quantity is parsed safely
        String quantityStr = data.getOrDefault("quantity", "1");
        int quantity;
        try {
            quantity = Integer.parseInt(quantityStr);
        } catch (NumberFormatException e) {
            quantity = 0; // Default to 0 or handle error
        }

        String action = data.getOrDefault("action", "add");
        String productId = data.get("productId");

        // --- Core Cart Logic ---
        if (action.equals("add")) {
            cart.addItem(productId, quantity);
        } else if (action.equals("update")) {
            cart.updateItem(productId, quantity);
        } else if (action.equals("delete")) {
            cart.deleteItem(productId);
        }
        // -------------------------

        // 3. Send back a JSON response for the AJAX call
        String jsonResponse = String.format("{\"success\": true, \"cartCount\": %d}", cart.getTotalItems());

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, jsonResponse.length());
        exchange.getResponseBody().write(jsonResponse.getBytes());
        exchange.close();
    }
}
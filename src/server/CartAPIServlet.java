package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import server.model.*;

public class CartAPIServlet implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        ShoppingCart cart = MainServer.getCart(ex);

        if (method.equalsIgnoreCase("POST")) {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = MainServer.parse(body);

            String action = params.get("action");
            String productId = params.get("productId");

            if ("add".equals(action)) {
                int qty = Integer.parseInt(params.getOrDefault("quantity", "1"));

                // Find the product in the database to get ALL its details
                Product p = ProductDatabase.getProductById(productId);

                if (p != null) {
                    // Pass complete product information to cart
                    cart.addItem(p.getId(), p.getName(), p.getPrice(), qty);

                    // Return success with cart count
                    String json = String.format("{\"success\":true, \"cartCount\":%d, \"message\":\"Added to cart!\"}",
                            cart.getItems().size());
                    sendResponse(ex, 200, json);
                } else {
                    // Product not found
                    String json = "{\"success\":false, \"message\":\"Product not found\"}";
                    sendResponse(ex, 404, json);
                }

            } else if ("delete".equals(action)) {
                cart.removeItem(productId);

                // Return updated cart count
                String json = String.format("{\"success\":true, \"cartCount\":%d}",
                        cart.getItems().size());
                sendResponse(ex, 200, json);

            } else {
                String json = "{\"success\":false, \"message\":\"Invalid action\"}";
                sendResponse(ex, 400, json);
            }

        } else if (method.equalsIgnoreCase("GET")) {
            // GET request: return cart count and items
            int itemCount = cart.getItems().stream()
                    .mapToInt(item -> item.getQuantity())
                    .sum();

            String json = String.format("{\"cartCount\":%d}", itemCount);
            sendResponse(ex, 200, json);
        }
    }

    private void sendResponse(HttpExchange ex, int code, String json) throws IOException {
        byte[] res = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, res.length);
        ex.getResponseBody().write(res);
        ex.close();
    }
}
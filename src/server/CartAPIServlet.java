package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import server.model.*;

public class CartAPIServlet implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        // --- NEW SECURITY LAYER START ---
        // 1. Check for the session cookie
        String cookieHeader = ex.getRequestHeaders().getFirst("Cookie");
        String sid = extractSid(cookieHeader);

        // 2. Look up the username in the global active sessions map
        String username = (sid != null) ? AuthHandler.activeSessions.get(sid) : null;

        // 3. If no valid session is found, stop here and return 401
        if (username == null) {
            String json = "{\"success\":false, \"message\":\"Please login first!\"}";
            sendResponse(ex, 401, json);
            return; // Stops the guest from reaching the cart logic below
        }
        // --- NEW SECURITY LAYER END ---

        // --- YOUR ORIGINAL LOGIC (UNCHANGED) ---
        String method = ex.getRequestMethod();
        ShoppingCart cart = MainServer.getCart(ex);

        if (method.equalsIgnoreCase("POST")) {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = MainServer.parse(body);

            String action = params.get("action");
            String productId = params.get("productId");

            if ("add".equals(action)) {
                int qty = Integer.parseInt(params.getOrDefault("quantity", "1"));
                Product p = ProductDatabase.getProductById(productId);

                if (p != null) {
                    cart.addItem(p.getId(), p.getName(), p.getPrice(), qty);
                    String json = String.format("{\"success\":true, \"cartCount\":%d, \"message\":\"Added to cart!\"}",
                            cart.getItems().size());
                    sendResponse(ex, 200, json);
                } else {
                    String json = "{\"success\":false, \"message\":\"Product not found\"}";
                    sendResponse(ex, 404, json);
                }
            } else if ("delete".equals(action)) {
                cart.removeItem(productId);
                String json = String.format("{\"success\":true, \"cartCount\":%d}",
                        cart.getItems().size());
                sendResponse(ex, 200, json);
            }
        } else if (method.equalsIgnoreCase("GET")) {
            int itemCount = cart.getItems().stream()
                    .mapToInt(CartItem::getQuantity)
                    .sum();
            String json = String.format("{\"cartCount\":%d}", itemCount);
            sendResponse(ex, 200, json);
        }
    }

    // This helper method is safe to add
    private String extractSid(String cookieHeader) {
        if (cookieHeader == null) return null;
        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=");
            if (parts.length == 2 && parts[0].equals("AUTH_SESSION")) {
                return parts[1];
            }
        }
        return null;
    }

    private void sendResponse(HttpExchange ex, int code, String json) throws IOException {
        byte[] res = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(code, res.length);
        ex.getResponseBody().write(res);
        ex.close();
    }
}
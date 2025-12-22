package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import server.model.*;

public class CheckoutHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();

        // 1. If user is just trying to open the page (GET)
        if (method.equalsIgnoreCase("GET")) {
            File file = new File("web/checkout.html");
            if (!file.exists()) {
                String error = "Checkout page missing in web folder!";
                ex.sendResponseHeaders(404, error.length());
                ex.getResponseBody().write(error.getBytes());
                ex.close();
                return;
            }
            // Now 'Files' will be recognized because of the import above
            byte[] content = Files.readAllBytes(file.toPath());
            ex.getResponseHeaders().add("Content-Type", "text/html");
            ex.sendResponseHeaders(200, content.length);
            ex.getResponseBody().write(content);
            ex.close();
            return;
        }

        // 2. If user clicks "Place Order" (POST)
        if (method.equalsIgnoreCase("POST")) {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = MainServer.parse(body);

            // Get the current user's cart
            ShoppingCart cart = MainServer.getCart(ex);

            // Create the order
            long orderId = System.currentTimeMillis();
            Order order = new Order(
                    orderId,
                    data.get("name"),
                    data.get("phone"),
                    data.get("address"),
                    data.get("paymentMethod"),
                    data.getOrDefault("cc_number", "N/A"),
                    data.getOrDefault("cc_expiry", "N/A"),
                    new ArrayList<>(cart.getItems()),
                    cart.calculateTotal()
            );

            // Save to database and file
            OrderDatabase.addOrder(order);

            // Clear the cart after purchase
            MainServer.clearCart(ex);

            // REDIRECT (The "Summon" code)
            ex.getResponseHeaders().add("Location", "/order_confirmation.html?id=" + orderId);
            ex.sendResponseHeaders(302, -1);
            ex.close();
        }
    }
}
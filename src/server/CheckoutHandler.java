package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import server.model.ShoppingCart;
import server.model.Order;

public class CheckoutHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        try {
            ShoppingCart cart = MainServer.getCart(exchange);
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = MainServer.parse(body);

            String customerName = data.get("name");
            String phone = data.get("phone");
            String address = data.get("address");
            String paymentMethod = data.get("paymentMethod"); // Capture payment method

            // Safely read optional CC fields to prevent NullPointerException
            String ccNumber = data.getOrDefault("cc_number", "-");
            String ccExpiry = data.getOrDefault("cc_expiry", "-");


            if (cart.isEmpty()) {
                String error = "Cart is empty. Cannot checkout.";
                exchange.sendResponseHeaders(400, error.length());
                exchange.getResponseBody().write(error.getBytes());
                return;
            }

            long orderId = System.currentTimeMillis() % 1000000;
            double total = cart.calculateTotal();

            //Update constructor call to pass ALL parameters (9 total)
            Order finalOrder = new Order(
                    orderId,
                    customerName,
                    phone,
                    address,
                    paymentMethod,
                    ccNumber,     // Added CC number parameter
                    ccExpiry,     // Added CC expiry parameter
                    cart.getItems(),
                    total
            );

            // toText() method must exist on the Order class
            try (FileWriter fw = new FileWriter("final_orders.txt", true)) {
                fw.write(finalOrder.toText()); // <--- This method must exist
            } catch (IOException fileException) {
                System.err.println("Warning: Could not save final order to file: " + fileException.getMessage());
            }

            MainServer.clearCart(exchange);

            String confirmationUrl = "/order_confirmation.html?id=" + orderId + "&method=" + paymentMethod;
            exchange.getResponseHeaders().add("Location", confirmationUrl);
            exchange.sendResponseHeaders(302, -1);

        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in CheckoutHandler: " + e.getMessage());
            e.printStackTrace();
            String error = "Internal Server Error during checkout. See server console for details.";
            exchange.sendResponseHeaders(500, error.length());
            exchange.getResponseBody().write(error.getBytes());
        } finally {
            exchange.close();
        }
    }
}
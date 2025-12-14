package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import server.model.CartItem;
import server.model.ShoppingCart;

public class CartPageHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ShoppingCart cart = MainServer.getCart(exchange);

        // Build the dynamic HTML content for the table body
        StringBuilder tableRows = new StringBuilder();

        for (CartItem item : cart.getItems()) {
            String productId = item.getProductId();
            String productName = getProductName(productId);
            double subtotal = item.getPrice() * item.getQuantity();

            // This row structure must match the HTML template and include JS hooks
            String row = String.format(
                    "<tr data-product-id=\"%s\">" +
                            "<td>%s</td>" +
                            "<td class=\"item-price\">%.2f</td>" +
                            "<td>" +
                            "<input type=\"number\" value=\"%d\" min=\"1\" class=\"item-quantity-input\" onchange=\"updateCartQuantity('%s', this)\">" +
                            "</td>" +
                            "<td class=\"item-subtotal\">%.2f</td>" +
                            "<td><button onclick=\"removeItemFromCart('%s', this)\">Remove</button></td>" +
                            "</tr>",
                    productId, productName, item.getPrice(), item.getQuantity(), productId, subtotal, productId
            );
            tableRows.append(row);
        }

        // Generate the final page using the template and dynamic data
        String dynamicContent = generateHtmlTemplate(tableRows.toString(), cart.getTotalItems(), cart.calculateTotal());

        // Send the response
        exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        byte[] response = dynamicContent.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    private String getProductName(String productId) {
        // Ensure these IDs match what's in your order.html
        if (productId.equals("CHIP001")) return "Classic Choco Chip";
        if (productId.equals("OAT002")) return "Oatmeal Raisin Dream";
        return "Unknown Product: " + productId;
    }

    private String generateHtmlTemplate(String tableRows, int totalItems, double grandTotal) {
        // This is the structure of your cart.html file, with placeholders (%s, %d, %.2f)
        return String.format(
                "<!DOCTYPE html>\n" +
                        "<html lang=\"en\">\n" +
                        "<head>\n" +
                        "    <meta charset=\"UTF-8\">\n" +
                        "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                        "    <title>Shopping Cart | CookieHaven</title>\n" +
                        "    <link rel=\"stylesheet\" href=\"style.css\">\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "<header>\n" +
                        "    <h1>Your Shopping Cart</h1>\n" +
                        "    <nav>\n" +
                        "        <a href=\"/order.html\">Continue Shopping</a>\n" +
                        "    </nav>\n" +
                        "</header>\n" +
                        "\n" +
                        "<main>\n" +
                        "    <section id=\"cart-items\">\n" +
                        "        <table>\n" +
                        "            <thead>\n" +
                        "            <tr><th>Product</th><th>Price</th><th>Quantity</th><th>Subtotal</th><th>Actions</th></tr>\n" +
                        "            </thead>\n" +
                        "            <tbody id=\"cart-table-body\">" +
                        "                %s" + // Dynamic table rows inserted here
                        "            </tbody>\n" +
                        "        </table>\n" +
                        "    </section>\n" +
                        "\n" +
                        "    <section id=\"cart-summary\">\n" +
                        "        <h2>Order Summary</h2>\n" +
                        "        <p>Total Items: <span id=\"cart-count\">%d</span></p>\n" + // Dynamic total items
                        "        <p>Total: RM <span id=\"cart-grand-total\">%.2f</span></p>\n" + // Dynamic grand total
                        "\n" +
                        "        <a href=\"/checkout.html\">\n" +
                        "            <button id=\"checkout-button\">Proceed to Checkout</button>\n" +
                        "        </a>\n" +
                        "    </section>\n" +
                        "</main>\n" +
                        "\n" +
                        "<footer>\n" +
                        "    <p>&copy; 2025 CookieHaven</p>\n" +
                        "</footer>\n" +
                        "<script src=\"script.js\"></script>" + // JS is needed for live updates
                        "</body>\n" +
                        "</html>",
                tableRows, totalItems, grandTotal
        );
    }
}

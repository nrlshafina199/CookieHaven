package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import server.model.*;

public class CartPageHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange ex) throws IOException {
        ShoppingCart cart = MainServer.getCart(ex);
        String html = new String(MainServer.readFile("web/cart.html"), StandardCharsets.UTF_8);

        StringBuilder rows = new StringBuilder();
        for (CartItem item : cart.getItems()) {
            rows.append("<tr>")
                    .append("<td>").append(item.getProductName()).append("</td>")
                    .append("<td>RM ").append(String.format("%.2f", item.getPrice())).append("</td>")
                    .append("<td>").append(item.getQuantity()).append("</td>")
                    .append("<td>RM ").append(String.format("%.2f", item.getSubtotal())).append("</td>")
                    .append("<td><button type='button' onclick=\"removeItem('")
                    .append(item.getProductId())
                    .append("')\">Remove</button></td>")
                    .append("</tr>");
        }

        html = html.replace("<tbody id=\"cart-table-body\"></tbody>",
                "<tbody id=\"cart-table-body\">" + rows.toString() + "</tbody>");

        html = html.replace("id=\"cart-grand-total\">0.00",
                "id=\"cart-grand-total\">" + String.format("%.2f", cart.calculateTotal()));

        byte[] response = html.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, response.length);
        ex.getResponseBody().write(response);
        ex.close();
    }
}
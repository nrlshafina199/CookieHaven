package server;

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import server.model.*;

public class PublicProductHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {

        if (!ex.getRequestMethod().equalsIgnoreCase("GET")) {
            ex.sendResponseHeaders(405, -1);
            return;
        }

        List<Product> list = ProductDatabase.getAllProducts();

        String json = "[" + list.stream().map(p ->
                String.format(
                        "{\"id\":\"%s\",\"name\":\"%s\",\"price\":%.2f,\"stock\":%d," +
                                "\"description\":\"%s\",\"ingredients\":\"%s\",\"allergens\":\"%s\"}",
                        escape(p.getId()),
                        escape(p.getName()),
                        p.getPrice(),
                        p.getStock(),
                        escape(p.getDescription()),
                        escape(p.getIngredients()),
                        escape(p.getAllergens())
                )
        ).collect(Collectors.joining(",")) + "]";

        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(200, response.length);
        ex.getResponseBody().write(response);
        ex.close();
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "\\\"");
    }
}

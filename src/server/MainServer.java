package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainServer {

    public static void main(String[] args) throws Exception {
        // Start server at port 8080
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        // Handle order form submission
        server.createContext("/order", new OrderHandler());

        // Handle admin view
        server.createContext("/admin", new AdminHandler());

        server.setExecutor(null); // default executor
        server.start();

        System.out.println("Server running at http://localhost:8080/");
    }

    // Handles order submissions
    static class OrderHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1); // Method not allowed
                return;
            }

            // Read data from form
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> data = parse(body);

            // Create order object
            Order order = new Order(
                    data.get("name"),
                    data.get("phone"),
                    data.get("email"),
                    data.get("cookie"),
                    Integer.parseInt(data.get("qty")),
                    data.get("method"),
                    data.getOrDefault("address", "-"),
                    data.get("allergy"),
                    data.get("privacy")
            );

            // Save order to text file
            FileWriter fw = new FileWriter("orderdata.txt", true);
            fw.write(order.toText());
            fw.close();

            String response = "Order saved successfully!";
            exchange.sendResponseHeaders(200, response.length());
            exchange.getResponseBody().write(response.getBytes());
            exchange.close();
        }
    }

    // Handles admin viewing all orders
    static class AdminHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            File file = new File("orderdata.txt");
            if (!file.exists()) {
                file.createNewFile();
            }
            byte[] data = java.nio.file.Files.readAllBytes(file.toPath());

            exchange.sendResponseHeaders(200, data.length);
            exchange.getResponseBody().write(data);
            exchange.close();
        }
    }

    // Parse URL-encoded form data
    static Map<String,String> parse(String body){
        Map<String,String> map = new HashMap<>();
        String[] pairs = body.split("&");
        for (String p : pairs) {
            String[] kv = p.split("=");
            if (kv.length == 2) {
                map.put(kv[0], kv[1].replace("+"," "));
            }
        }
        return map;
    }
}


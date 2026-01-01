package server.model;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import server.MainServer;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class ReviewHandler implements HttpHandler {

    private static final String FILE_PATH = "reviews.txt";

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        if ("GET".equalsIgnoreCase(method)) {
            handleGetReviews(exchange);
        } else if ("POST".equalsIgnoreCase(method)) {
            handlePostReview(exchange);
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }

    // 1. LOAD REVIEWS
    private void handleGetReviews(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String filter = "all";

        if (query != null && query.contains("filter=")) {
            filter = query.split("filter=")[1].split("&")[0];
        }

        List<Map<String, String>> reviews = readReviewsFromFile();

        // Apply Logic: Filter by Star Rating
        if (!filter.equals("all")) {
            String target = filter;
            if (target.equals("3")) {
                reviews = reviews.stream()
                        .filter(r -> Integer.parseInt(r.get("rating")) <= 3)
                        .collect(Collectors.toList());
            } else {
                reviews = reviews.stream()
                        .filter(r -> r.get("rating").equals(target))
                        .collect(Collectors.toList());
            }
        }

        // Convert list to JSON
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < reviews.size(); i++) {
            Map<String, String> r = reviews.get(i);

            // We need to be careful with JSON syntax (avoid breaking quotes)
            String safeComment = r.get("comment").replace("\"", "'").replace("\n", " ");

            json.append(String.format("{\"cookie\":\"%s\", \"rating\":%s, \"comment\":\"%s\", \"user\":\"%s\", \"image\":\"%s\"}",
                    r.get("cookie"), r.get("rating"), safeComment, r.get("user"), r.get("image")));

            if (i < reviews.size() - 1) json.append(",");
        }
        json.append("]");

        byte[] response = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }

    // 2. SAVE NEW REVIEW
    private void handlePostReview(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        // Note: MainServer.parse might choke on huge Base64 strings if they contain '&' or '=',
        // but for a simple project, standard parsing usually survives standard Base64.
        Map<String, String> params = MainServer.parse(body);

        String cookie = params.get("cookieType");
        String rating = params.get("rating");
        String comment = params.get("comment");
        String user = "Anonymous Guest";

        // Retrieve the image string (it will be very long!)
        String image = params.getOrDefault("image", "none");

        // Clean up inputs to prevent file corruption
        if(comment != null) comment = comment.replace("|", "-").replace("\n", " ");
        if(image != null) image = image.replace("\n", ""); // Remove newlines from base64

        // Save to file (Format: Cookie|Rating|Comment|User|Image)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.write(cookie + "|" + rating + "|" + comment + "|" + user + "|" + image);
            writer.newLine();
        }

        String response = "{\"success\":true}";
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    // Helper: Read file
    private List<Map<String, String>> readReviewsFromFile() {
        List<Map<String, String>> list = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // We split by pipe symbol
                String[] parts = line.split("\\|");
                // Check if we have at least 5 parts (now including image)
                if (parts.length >= 5) {
                    Map<String, String> map = new HashMap<>();
                    map.put("cookie", parts[0]);
                    map.put("rating", parts[1]);
                    map.put("comment", parts[2]);
                    map.put("user", parts[3]);
                    map.put("image", parts[4]);
                    list.add(map);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Collections.reverse(list);
        return list;
    }
}
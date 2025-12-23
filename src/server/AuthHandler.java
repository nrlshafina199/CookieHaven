package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mindrot.jbcrypt.BCrypt;
import server.model.User;

public class AuthHandler implements HttpHandler {
    private static final Map<String, User> userDatabase = new ConcurrentHashMap<>();
    public static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private static final String USER_FILE = "users.txt";
    private static final String ORDER_FILE = "orderdata.txt";

    public AuthHandler() {
        // Automatically creates files if they don't exist
        ensureFileExists(USER_FILE);
        ensureFileExists(ORDER_FILE);
        loadUsersFromFile();
    }

    private void ensureFileExists(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        // Ignore static files
        if (path.equals("/") || path.endsWith(".html") || path.endsWith(".css") || path.endsWith(".png") || path.endsWith(".jpg")) {
            return;
        }

        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {
            InputStreamReader isr = new InputStreamReader(ex.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            Map<String, String> data = MainServer.parse(br.readLine());

            if (path.contains("/api/register")) {
                String username = data.get("username");
                String hashed = BCrypt.hashpw(data.get("password"), BCrypt.gensalt());
                User newUser = new User(username, data.get("email"), hashed);
                userDatabase.put(username, newUser);
                saveUserToFile(newUser);
                sendRedirect(ex, "/login.html");
            }
            else if (path.contains("/api/login")) {
                User user = userDatabase.get(data.get("username"));
                if (user != null && BCrypt.checkpw(data.get("password"), user.getPassword())) {
                    String sessionId = UUID.randomUUID().toString();
                    activeSessions.put(sessionId, user.getUsername());
                    // Set secure cookie for session tracking
                    ex.getResponseHeaders().add("Set-Cookie", "AUTH_SESSION=" + sessionId + "; Path=/; HttpOnly");
                    sendJsonResponse(ex, "{\"success\": true}");
                } else {
                    ex.sendResponseHeaders(401, -1);
                }
                ex.close();
            }
        }
        else if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {
            String cookie = ex.getRequestHeaders().getFirst("Cookie");
            String sid = extractSid(cookie);
            String username = (sid != null) ? activeSessions.get(sid) : null;

            // 1. FORGOT PASSWORD (Must be accessible without login)
            if (path.contains("/api/forgot-password")) {
                String query = ex.getRequestURI().getQuery();
                if (query != null && query.contains("=")) {
                    String email = query.split("=")[1];
                    boolean found = false;
                    for (User u : userDatabase.values()) {
                        if (u.getEmail().equalsIgnoreCase(email)) {
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        sendJsonResponse(ex, "{\"success\": true}");
                    } else {
                        ex.sendResponseHeaders(404, -1);
                        ex.close();
                    }
                }
                return;
            }

            // 2. USER DATA (Modified to return null if guest, allowing Home page to load)
            if (path.contains("/api/user-data")) {
                if (username != null) {
                    User user = userDatabase.get(username);
                    sendJsonResponse(ex, "{\"username\":\"" + user.getUsername() + "\", \"email\":\"" + user.getEmail() + "\"}");
                } else {
                    // Send null so index.html knows to show the guest view
                    sendJsonResponse(ex, "{\"username\": null}");
                }
                return;
            }

            // 3. STRICT SESSION CHECK (Only for protected actions like logout/history)
            if (username == null) {
                ex.sendResponseHeaders(401, -1);
                ex.close();
                return;
            }

            if (path.contains("/api/user-orders")) {
                loadOrdersForUser(ex, username);
            }
            else if (path.contains("/logout")) {
                activeSessions.remove(sid);
                sendRedirect(ex, "/login.html");
            }
        }
    }

    private void saveUserToFile(User user) {
        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(USER_FILE, true)))) {
            out.println(user.getUsername() + "," + user.getEmail() + "," + user.getPassword());
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadUsersFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length == 3) userDatabase.put(p[0], new User(p[0], p[1], p[2]));
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadOrdersForUser(HttpExchange ex, String username) throws IOException {
        StringBuilder json = new StringBuilder("[");
        try (BufferedReader reader = new BufferedReader(new FileReader(ORDER_FILE))) {
            String line; boolean first = true;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length >= 5 && p[0].equals(username)) {
                    if (!first) json.append(",");
                    json.append(String.format("{\"id\":\"%s\", \"date\":\"%s\", \"total\":\"%s\", \"status\":\"%s\"}", p[1], p[2], p[3], p[4]));
                    first = false;
                }
            }
        }
        json.append("]");
        sendJsonResponse(ex, json.toString());
    }

    private void sendJsonResponse(HttpExchange ex, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, json.getBytes().length);
        ex.getResponseBody().write(json.getBytes());
        ex.close();
    }

    private String extractSid(String cookieHeader) {
        if (cookieHeader == null) return null;
        for (String cookie : cookieHeader.split(";")) {
            String[] parts = cookie.trim().split("=");
            if (parts.length == 2 && parts[0].equals("AUTH_SESSION")) return parts[1];
        }
        return null;
    }

    private void sendRedirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }
}
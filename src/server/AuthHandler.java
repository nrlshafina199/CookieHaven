package server;

import com.sun.net.httpserver.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.mindrot.jbcrypt.BCrypt;

import server.model.User;
import server.model.OrderDatabase;
import server.model.OrderWithStatus;

public class AuthHandler implements HttpHandler {

    private static final Map<String, User> userDatabase = new ConcurrentHashMap<>();
    public static final Map<String, String> activeSessions = new ConcurrentHashMap<>();
    private static final String USER_FILE = "users.txt";

    public AuthHandler() {
        ensureFileExists(USER_FILE);
        loadUsersFromFile();
    }

    private void ensureFileExists(String fileName) {
        try {
            File file = new File(fileName);
            if (!file.exists()) file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        // Ignore static files
        if (path.equals("/") || path.endsWith(".html") || path.endsWith(".css")
                || path.endsWith(".png") || path.endsWith(".jpg")) {
            return;
        }

        /* ===================== POST ===================== */
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(ex.getRequestBody(), "utf-8"));
            Map<String, String> data = MainServer.parse(br.readLine());

            if (path.contains("/api/register")) {
                String username = data.get("username");
                String hashed = BCrypt.hashpw(data.get("password"), BCrypt.gensalt());

                User user = new User(username, data.get("email"), hashed);
                userDatabase.put(username, user);
                saveUserToFile(user);

                sendRedirect(ex, "/login.html");
            }

            else if (path.contains("/api/login")) {
                User user = userDatabase.get(data.get("username"));

                if (user != null && BCrypt.checkpw(data.get("password"), user.getPassword())) {
                    String sessionId = UUID.randomUUID().toString();
                    activeSessions.put(sessionId, user.getUsername());

                    ex.getResponseHeaders().add(
                            "Set-Cookie",
                            "AUTH_SESSION=" + sessionId + "; Path=/; HttpOnly"
                    );

                    sendJsonResponse(ex, "{\"success\":true}");
                } else {
                    ex.sendResponseHeaders(401, -1);
                    ex.close();
                }
            }
        }

        /* ===================== GET ===================== */
        else if ("GET".equalsIgnoreCase(ex.getRequestMethod())) {

            String cookie = ex.getRequestHeaders().getFirst("Cookie");
            String sid = extractSid(cookie);
            String username = (sid != null) ? activeSessions.get(sid) : null;

            // USER DATA (guest allowed)
            if (path.contains("/api/user-data")) {
                if (username != null) {
                    User u = userDatabase.get(username);
                    sendJsonResponse(ex,
                            "{\"username\":\"" + u.getUsername() +
                                    "\",\"email\":\"" + u.getEmail() + "\"}");
                } else {
                    sendJsonResponse(ex, "{\"username\":null}");
                }
                return;
            }

            // PROTECTED ROUTES
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

    /* ===================== USERS ===================== */

    private void saveUserToFile(User user) {
        try (PrintWriter out = new PrintWriter(
                new BufferedWriter(new FileWriter(USER_FILE, true)))) {

            out.println(user.getUsername() + "," +
                    user.getEmail() + "," +
                    user.getPassword());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadUsersFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(USER_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split(",");
                if (p.length == 3) {
                    userDatabase.put(p[0], new User(p[0], p[1], p[2]));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ===================== USER ORDERS ===================== */

    private void loadOrdersForUser(HttpExchange ex, String username) throws IOException {

        List<OrderWithStatus> orders = OrderDatabase.getAllOrders();
        StringBuilder json = new StringBuilder("[");
        boolean first = true;

        for (OrderWithStatus o : orders) {

            if (!o.getCustomerName().equalsIgnoreCase(username)) continue;

            if (!first) json.append(",");
            json.append(String.format(
                    "{\"id\":%d,\"date\":\"%tF\",\"total\":%.2f,\"status\":\"%s\"}",
                    o.getOrderId(),
                    o.getOrderDate(),
                    o.getTotal(),
                    o.getStatus()
            ));
            first = false;
        }

        json.append("]");
        sendJsonResponse(ex, json.toString());
    }

    /* ===================== HELPERS ===================== */

    private void sendJsonResponse(HttpExchange ex, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(200, json.getBytes().length);
        ex.getResponseBody().write(json.getBytes());
        ex.close();
    }

    private String extractSid(String cookieHeader) {
        if (cookieHeader == null) return null;

        for (String c : cookieHeader.split(";")) {
            String[] p = c.trim().split("=");
            if (p.length == 2 && p[0].equals("AUTH_SESSION")) {
                return p[1];
            }
        }
        return null;
    }

    private void sendRedirect(HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().set("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }
}

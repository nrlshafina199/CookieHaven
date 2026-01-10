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

        // Skip static file requests
        if (path.equals("/") || path.endsWith(".html") || path.endsWith(".css")
                || path.endsWith(".png") || path.endsWith(".jpg")) {
            return;
        }

        /* ===================== POST ===================== */
        if ("POST".equalsIgnoreCase(ex.getRequestMethod())) {

            BufferedReader br = new BufferedReader(
                    new InputStreamReader(ex.getRequestBody(), "utf-8"));
            Map<String, String> data = MainServer.parse(br.readLine());

            // Register new user
            if (path.contains("/api/register")) {
                String username = data.get("username");
                String hashed = BCrypt.hashpw(data.get("password"), BCrypt.gensalt());

                User user = new User(username, data.get("email"), hashed);
                userDatabase.put(username, user);
                saveUserToFile(user);

                sendRedirect(ex, "/login.html");
            }

            // User login
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

            // Forgot Password Validation: Checks if email exists in users.txt
            if (path.contains("/api/forgot-password")) {
                String query = ex.getRequestURI().getQuery();
                String submittedEmail = null;

                if (query != null && query.contains("email=")) {
                    // Extract email value from query string
                    String[] parts = query.split("=");
                    if (parts.length > 1) {
                        submittedEmail = parts[1];
                        // Decode URL characters (e.g., %40 to @) and trim spaces
                        submittedEmail = java.net.URLDecoder.decode(submittedEmail, "UTF-8").trim();
                    }
                }

                boolean foundInRecords = false;
                if (submittedEmail != null && !submittedEmail.isEmpty()) {
                    // Loop through loaded users to find a match for the email
                    for (User user : userDatabase.values()) {
                        if (user.getEmail().equalsIgnoreCase(submittedEmail)) {
                            foundInRecords = true;
                            break;
                        }
                    }
                }

                if (foundInRecords) {
                    // Email found, return success
                    sendJsonResponse(ex, "{\"success\":true}");
                } else {
                    // Email not found in users.txt, return 404 error
                    ex.sendResponseHeaders(404, -1);
                    ex.close();
                }
                return;
            }

            String cookie = ex.getRequestHeaders().getFirst("Cookie");
            String sid = extractSid(cookie);
            String username = (sid != null) ? activeSessions.get(sid) : null;

            // Retrieve user data for profile (allow guest view)
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

            // Handle Protected Routes (Session Required)
            if (username == null) {
                ex.sendResponseHeaders(401, -1);
                ex.close();
                return;
            }

            // Load orders for current user
            if (path.contains("/api/user-orders")) {
                loadOrdersForUser(ex, username);
            }

            // Handle Logout
            else if (path.contains("/logout")) {
                activeSessions.remove(sid);
                sendRedirect(ex, "/login.html");
            }
        }
    }

    /* ===================== USERS DATA MANAGEMENT ===================== */

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
                    // Trim fields to ensure accurate comparison (removes hidden spaces)
                    String username = p[0].trim();
                    String email = p[1].trim();
                    String password = p[2].trim();
                    userDatabase.put(username, new User(username, email, password));
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

    /* ===================== HELPER METHODS ===================== */

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
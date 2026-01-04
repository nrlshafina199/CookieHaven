package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import server.model.Review;
import server.model.ReviewDatabase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class ReviewHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();

        /* ================= SUBMIT REVIEW (USER) ================= */
        if (method.equals("POST") && path.equals("/api/reviews")) {

            String body = new String(
                    ex.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            Map<String, String> form = MainServer.parse(body);

            ReviewDatabase.add(
                    form.get("cookieType"),
                    form.get("comment"),
                    Integer.parseInt(form.get("rating")),
                    form.getOrDefault("image", "none")
            );

            ex.sendResponseHeaders(200, 0);
            ex.close();
            return;
        }

        /* ================= DELETE REVIEW (ADMIN) ================= */
        if (method.equals("POST") && path.equals("/admin/reviews")) {

            String body = new String(
                    ex.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
            );
            String[] d = body.split("\\|");

            if ("delete".equals(d[0])) {
                ReviewDatabase.delete(Integer.parseInt(d[1]));
            }

            ex.sendResponseHeaders(200, 0);
            ex.close();
            return;
        }

        /* ================= GET REVIEWS (JSON) ================= */
        if (method.equals("GET") && path.equals("/api/reviews")) {

            StringBuilder json = new StringBuilder("[");
            boolean first = true;

            for (Review r : ReviewDatabase.all()) {
                if (!first) json.append(",");
                first = false;

                json.append("{")
                        .append("\"id\":").append(r.id).append(",")
                        .append("\"cookie\":\"").append(r.product).append("\",")
                        .append("\"comment\":\"").append(r.text).append("\",")
                        .append("\"rating\":").append(r.rating).append(",")
                        .append("\"image\":\"").append(r.image).append("\"")
                        .append("}");
            }

            json.append("]");

            byte[] out = json.toString().getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        }
    }
}

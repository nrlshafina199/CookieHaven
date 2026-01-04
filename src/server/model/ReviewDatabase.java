package server.model;

import java.io.*;
import java.util.*;

public class ReviewDatabase {

    private static final List<Review> reviews = new ArrayList<>();
    private static int idCounter = 1;
    private static final String FILE = "reviews.txt";

    static {
        load();
    }

    /* ================= ADD REVIEW ================= */
    public static void add(String product, String text, int rating, String image) {
        Review r = new Review(idCounter++, product, text, rating, image);
        reviews.add(r);
        save();
    }

    /* ================= GET ALL ================= */
    public static List<Review> all() {
        return reviews;
    }

    /* ================= DELETE REVIEW ================= */
    public static void delete(int id) {
        reviews.removeIf(r -> r.id == id);
        save();
    }

    /* ================= SAVE TO FILE ================= */
    private static void save() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE))) {
            for (Review r : reviews) {
                out.println(
                        r.id + "|" +
                                r.product + "|" +
                                r.text.replace("|", "/") + "|" +
                                r.rating + "|" +
                                r.image
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* ================= LOAD FROM FILE ================= */
    private static void load() {
        File f = new File(FILE);
        if (!f.exists()) return;

        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] d = line.split("\\|", 5);
                if (d.length == 5) {
                    int id = Integer.parseInt(d[0]);
                    reviews.add(new Review(
                            id,
                            d[1],
                            d[2],
                            Integer.parseInt(d[3]),
                            d[4]
                    ));
                    idCounter = Math.max(idCounter, id + 1);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

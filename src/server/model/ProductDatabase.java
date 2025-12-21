package server.model;

import java.io.*;
import java.util.*;

public class ProductDatabase {
    private static final Map<String, Product> products = new LinkedHashMap<>();
    private static final String FILE_PATH = "products.txt";

    static {
        loadFromFile();
    }

    public static void addProduct(Product p) {
        products.put(p.getId(), p);
        saveToFile(); // Saves immediately to disk
    }

    public static void deleteProduct(String id) {
        products.remove(id);
        saveToFile();
    }

    public static void reduceStock(String id, int qty) {
        Product p = products.get(id);
        if (p != null) {
            int currentStock = p.getStock();
            int newStock = Math.max(0, currentStock - qty);

            // Re-create the product with new stock value
            Product updated = new Product(p.getId(), p.getName(), p.getPrice(),
                    newStock, p.getDescription(),
                    p.getIngredients(), p.getAllergens());
            products.put(id, updated);
            saveToFile();
        }
    }

    public static List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public static Product getProductById(String id) {
        return products.get(id);
    }

    private static void saveToFile() {
        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_PATH))) {
            for (Product p : products.values()) {
                // Save ALL 7 fields separated by |
                out.println(p.getId() + "|" + p.getName() + "|" + p.getPrice() + "|" +
                        p.getStock() + "|" + p.getDescription() + "|" +
                        p.getIngredients() + "|" + p.getAllergens());
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static void loadFromFile() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] f = line.split("\\|");
                if (f.length >= 7) { // Corrected check
                    products.put(f[0], new Product(f[0], f[1], Double.parseDouble(f[2]),
                            Integer.parseInt(f[3]), f[4], f[5], f[6]));
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}
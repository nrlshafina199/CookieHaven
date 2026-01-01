package server.model;

import java.io.*;
import java.util.*;

public class ProductDatabase {

    private static final String FILE_PATH = "products.txt";
    private static final List<Product> products = new ArrayList<>();

    static {
        loadProducts();
    }

    public static List<Product> getAllProducts() { return products; }

    public static Product getProductById(String id) {
        return products.stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
    }

    public static void addProduct(Product p) {
        products.add(p);
        saveProducts();
    }

    public static void deleteProduct(String id) {
        products.removeIf(p -> p.getId().equals(id));
        saveProducts();
    }

    public static void reduceStock(String productId, int quantityPurchased) {
        for (Product p : products) {
            if (p.getId().equals(productId)) {
                int newStock = p.getStock() - quantityPurchased;
                if (newStock < 0) newStock = 0;
                p.setStock(newStock);
                saveProducts();
                break;
            }
        }
    }

    private static void saveProducts() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (Product p : products) {
                // We add the IMAGE at the end of the line
                String line = String.format("%s|%s|%.2f|%d|%s|%s|%s|%s",
                        p.getId(), p.getName(), p.getPrice(), p.getStock(),
                        p.getDescription(), p.getIngredients(), p.getAllergens(), p.getImage());
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadProducts() {
        File file = new File(FILE_PATH);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\|");

                // If line has 8 parts, it includes the Image.
                // If it has 7 parts, it's an old product (use "none").
                if (parts.length >= 7) {
                    String img = (parts.length >= 8) ? parts[7] : "none";

                    products.add(new Product(
                            parts[0], parts[1], Double.parseDouble(parts[2]),
                            Integer.parseInt(parts[3]), parts[4], parts[5], parts[6], img
                    ));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
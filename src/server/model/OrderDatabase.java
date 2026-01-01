package server.model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class OrderDatabase {

    private static final String FILE_PATH = "orders.ser";
    private static List<Order> orders = new ArrayList<>();

    static {
        loadOrders();
    }

    public static List<Order> getAllOrders() {
        return orders;
    }

    public static void addOrder(Order order) {
        orders.add(order);

        // --- FIX: REDUCE STOCK WHEN ORDER IS PLACED ---
        if (order.getItems() != null) {
            for (CartItem item : order.getItems()) {
                // This tells the ProductDatabase to lower the count for this ID
                try {
                    ProductDatabase.reduceStock(item.getProductId(), item.getQuantity());
                } catch (Exception e) {
                    System.err.println("Warning: Could not reduce stock for " + item.getProductName());
                }
            }
        }
        // ----------------------------------------------

        saveOrders();
    }

    public static void saveOrders() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_PATH))) {
            oos.writeObject(orders);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static void loadOrders() {
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                orders = (List<Order>) ois.readObject();
                System.out.println("✅ Loaded " + orders.size() + " orders from " + FILE_PATH);
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ No order database found. Starting fresh.");
        }
    }
}
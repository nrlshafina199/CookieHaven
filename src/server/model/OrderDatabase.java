package server.model;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderDatabase {
    private static final Map<Long, OrderWithStatus> ORDERS = new ConcurrentHashMap<>();
    private static final String ORDERS_FILE = "final_orders.txt";
    // Modified to be more flexible with the time zone format seen in your text file
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss", Locale.ENGLISH);

    static {
        loadOrders();
        System.out.println("OrderDatabase initialized. Total orders: " + ORDERS.size());
    }

    public static void addOrder(Order order) {
        OrderWithStatus orderWithStatus = new OrderWithStatus(
                order.getOrderId(),
                order.getCustomerName(),
                order.getPhone(),
                order.getAddress(),
                order.getPaymentMethod(),
                order.getCcNumber(),
                order.getCcExpiry(),
                order.getItems(),
                order.getTotal(),
                "Pending",
                new Date()
        );

        ORDERS.put(order.getOrderId(), orderWithStatus);
        saveOrders();

        System.out.println("✓ Order #" + order.getOrderId() + " saved to final_orders.txt");

        for (CartItem item : order.getItems()) {
            ProductDatabase.reduceStock(item.getProductId(), item.getQuantity());
        }
    }

    public static void updateOrderStatus(long orderId, String newStatus) {
        OrderWithStatus order = ORDERS.get(orderId);
        if (order != null) {
            OrderWithStatus updated = new OrderWithStatus(
                    order.getOrderId(),
                    order.getCustomerName(),
                    order.getPhone(),
                    order.getAddress(),
                    order.getPaymentMethod(),
                    order.getCcNumber(),
                    order.getCcExpiry(),
                    order.getItems(),
                    order.getTotal(),
                    newStatus,
                    order.getOrderDate()
            );
            ORDERS.put(orderId, updated);
            saveOrders();
        }
    }

    public static List<OrderWithStatus> getAllOrders() {
        return new ArrayList<>(ORDERS.values());
    }

    private static void saveOrders() {
        // Updated Date Format for saving to include full details
        SimpleDateFormat saveFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(ORDERS_FILE), StandardCharsets.UTF_8))) {

            List<OrderWithStatus> sortedOrders = new ArrayList<>(ORDERS.values());
            sortedOrders.sort(Comparator.comparingLong(OrderWithStatus::getOrderId));

            for (OrderWithStatus order : sortedOrders) {
                writer.write("--- FINAL ORDER #" + order.getOrderId() + " ---");
                writer.newLine();
                writer.write("Date: " + saveFormat.format(order.getOrderDate()));
                writer.newLine();
                writer.write("Name: " + order.getCustomerName());
                writer.newLine();
                writer.write("Phone: " + order.getPhone());
                writer.newLine();
                writer.write("Address: " + order.getAddress());
                writer.newLine();
                writer.write("Payment: " + order.getPaymentMethod());
                writer.newLine();

                if (order.getPaymentMethod().equals("CC") && !order.getCcNumber().equals("N/A")) {
                    writer.write("  Card #: " + order.getCcNumber());
                    writer.newLine();
                    writer.write("  Expiry: " + order.getCcExpiry());
                    writer.newLine();
                }

                if (!order.getStatus().equals("Pending")) {
                    writer.write("Status: " + order.getStatus());
                    writer.newLine();
                }

                writer.write(String.format("Total: RM %.2f", order.getTotal()));
                writer.newLine();

                writer.write("Items:");
                writer.newLine();
                for (CartItem item : order.getItems()) {
                    writer.write(String.format("  - Product ID: %s (Qty: %d, Price: %.2f, Subtotal: %.2f)",
                            item.getProductId(),
                            item.getQuantity(),
                            item.getPrice(),
                            item.getPrice() * item.getQuantity()));
                    writer.newLine();
                }

                writer.write("-------------------------");
                writer.newLine();
                writer.newLine();
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadOrders() {
        File file = new File(ORDERS_FILE);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            Long currentOrderId = null;
            Date currentDate = new Date();
            String currentName = "", currentPhone = "", currentAddress = "", currentPaymentMethod = "";
            String currentCcNumber = "N/A", currentCcExpiry = "N/A", currentStatus = "Pending";
            double currentTotal = 0.0;
            List<CartItem> currentItems = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                if (line.equals("-------------------------")) {
                    if (currentOrderId != null) {
                        saveLoadedOrder(currentOrderId, currentName, currentPhone, currentAddress,
                                currentPaymentMethod, currentCcNumber, currentCcExpiry,
                                currentTotal, currentItems, currentDate, currentStatus);
                    }
                    // Reset
                    currentOrderId = null;
                    currentCcNumber = "N/A";
                    currentCcExpiry = "N/A";
                    currentStatus = "Pending";
                    currentItems = new ArrayList<>();
                    continue;
                }

                if (line.startsWith("--- FINAL ORDER #")) {
                    currentOrderId = Long.parseLong(line.replace("--- FINAL ORDER #", "").replace("---", "").trim());
                } else if (line.startsWith("Date:")) {
                    try {
                        // Takes the first 19 characters to avoid TimeZone parsing issues
                        currentDate = DATE_FORMAT.parse(line.substring(5, 25).trim());
                    } catch (Exception e) {
                        currentDate = new Date();
                    }
                } else if (line.startsWith("Name:")) {
                    currentName = line.substring(5).trim();
                } else if (line.startsWith("Phone:")) {
                    currentPhone = line.substring(6).trim();
                } else if (line.startsWith("Address:")) {
                    currentAddress = line.substring(8).trim();
                } else if (line.startsWith("Payment:")) {
                    currentPaymentMethod = line.substring(8).trim();
                } else if (line.contains("Card #:")) {
                    currentCcNumber = line.substring(line.indexOf("Card #:") + 7).trim();
                } else if (line.contains("Expiry:")) {
                    currentCcExpiry = line.substring(line.indexOf("Expiry:") + 7).trim();
                } else if (line.startsWith("Status:")) {
                    currentStatus = line.substring(7).trim();
                } else if (line.startsWith("Total:")) {
                    currentTotal = Double.parseDouble(line.replaceAll("[^0-9.]", ""));
                } else if (line.startsWith("- Product ID:")) {
                    try {
                        String pId = extractValue(line, "Product ID:", "(");
                        String qStr = extractValue(line, "Qty:", ",");
                        String prStr = extractValue(line, "Price:", ",");

                        Product p = ProductDatabase.getProductById(pId);
                        String pName = (p != null) ? p.getName() : pId;

                        currentItems.add(new CartItem(pId, pName, Double.parseDouble(prStr), Integer.parseInt(qStr)));
                    } catch (Exception e) {
                        System.err.println("Skip item line: " + line);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void saveLoadedOrder(Long orderId, String name, String phone, String address,
                                        String paymentMethod, String ccNumber, String ccExpiry,
                                        double total, List<CartItem> items, Date date, String status) {
        ORDERS.put(orderId, new OrderWithStatus(orderId, name, phone, address, paymentMethod,
                ccNumber, ccExpiry, new ArrayList<>(items), total, status, date));
    }

    private static String extractValue(String line, String startMarker, String endMarker) {
        int start = line.indexOf(startMarker);
        if (start == -1) return "";
        start += startMarker.length();
        int end = line.indexOf(endMarker, start);
        if (end == -1) end = line.length();
        return line.substring(start, end).trim();
    }
}
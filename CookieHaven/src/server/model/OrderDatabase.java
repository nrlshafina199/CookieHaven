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
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH);

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
        System.out.println("  Customer: " + order.getCustomerName());
        System.out.println("  Items: " + order.getItems().size());
        System.out.println("  Total: RM " + order.getTotal());

        for (CartItem item : order.getItems()) {
            ProductDatabase.reduceStock(item.getProductId(), item.getQuantity());
        }

        ORDERS.put(order.getOrderId(), orderWithStatus);
        saveOrders();
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
            System.out.println("✓ Order #" + orderId + " status updated to: " + newStatus);
        }
    }

    public static List<Order> getAllOrders() {
        return new ArrayList<>(ORDERS.values());
    }

    private static void saveOrders() {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(ORDERS_FILE), StandardCharsets.UTF_8))) {

            // Sort orders by ID for consistent output
            List<OrderWithStatus> sortedOrders = new ArrayList<>(ORDERS.values());
            sortedOrders.sort(Comparator.comparingLong(OrderWithStatus::getOrderId));

            for (OrderWithStatus order : sortedOrders) {
                // Write order header
                writer.write("--- FINAL ORDER #" + order.getOrderId() + " ---");
                writer.newLine();

                // Write date
                writer.write("Date: " + DATE_FORMAT.format(order.getOrderDate()));
                writer.newLine();

                // Write customer details
                writer.write("Name: " + order.getCustomerName());
                writer.newLine();
                writer.write("Phone: " + order.getPhone());
                writer.newLine();
                writer.write("Address: " + order.getAddress());
                writer.newLine();

                // Write payment details
                writer.write("Payment: " + order.getPaymentMethod());
                writer.newLine();

                if (order.getPaymentMethod().equals("CC") &&
                        !order.getCcNumber().equals("N/A")) {
                    writer.write("  Card #: " + order.getCcNumber());
                    writer.newLine();
                    writer.write("  Expiry: " + order.getCcExpiry());
                    writer.newLine();
                }

                // Write status (if not Pending)
                if (!order.getStatus().equals("Pending")) {
                    writer.write("Status: " + order.getStatus());
                    writer.newLine();
                }

                // Write total
                writer.write(String.format("Total: RM %.2f", order.getTotal()));
                writer.newLine();

                // Write items
                writer.write("Items:");
                writer.newLine();
                for (CartItem item : order.getItems()) {
                    double subtotal = item.getPrice() * item.getQuantity();
                    writer.write(String.format("  - Product ID: %s (Qty: %d, Price: %.2f, Subtotal: %.2f)",
                            item.getProductId(),
                            item.getQuantity(),
                            item.getPrice(),
                            subtotal));
                    writer.newLine();
                }

                // Write separator
                writer.write("-------------------------");
                writer.newLine();
                writer.newLine();
            }

            writer.flush();
            System.out.println("✓ Orders saved to final_orders.txt: " + ORDERS.size() + " orders");

        } catch (IOException e) {
            System.err.println("Error saving orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void loadOrders() {
        File file = new File(ORDERS_FILE);
        if (!file.exists()) {
            System.out.println("No final_orders.txt found. Will create on first order.");
            return;
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            Long currentOrderId = null;
            Date currentDate = null;
            String currentName = null;
            String currentPhone = null;
            String currentAddress = null;
            String currentPaymentMethod = null;
            String currentCcNumber = "N/A";
            String currentCcExpiry = "N/A";
            String currentStatus = "Pending";
            double currentTotal = 0.0;
            List<CartItem> currentItems = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }

                // End of order block
                if (line.equals("-------------------------")) {
                    if (currentOrderId != null) {
                        saveLoadedOrder(currentOrderId, currentName, currentPhone, currentAddress,
                                currentPaymentMethod, currentCcNumber, currentCcExpiry,
                                currentTotal, currentItems, currentDate, currentStatus);
                    }

                    // Reset for next order
                    currentOrderId = null;
                    currentCcNumber = "N/A";
                    currentCcExpiry = "N/A";
                    currentStatus = "Pending";
                    currentItems = new ArrayList<>();
                    continue;
                }

                // Parse order data
                if (line.startsWith("--- FINAL ORDER #")) {
                    String orderIdStr = line.replace("--- FINAL ORDER #", "").replace("---", "").trim();
                    currentOrderId = Long.parseLong(orderIdStr);

                } else if (line.startsWith("Date:")) {
                    String dateStr = line.substring(5).trim();
                    try {
                        currentDate = DATE_FORMAT.parse(dateStr);
                    } catch (ParseException e) {
                        currentDate = new Date();
                        System.err.println("Could not parse date: " + dateStr);
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
                    String totalStr = line.substring(6).trim()
                            .replace("RM", "").trim();
                    currentTotal = Double.parseDouble(totalStr);

                } else if (line.contains("Product ID:")) {
                    // Parse: "- Product ID: OAT002 (Qty: 1, Price: 6.50, Subtotal: 6.50)"
                    try {
                        String productId = extractValue(line, "Product ID:", "(").trim();
                        String qtyStr = extractValue(line, "Qty:", ",").trim();
                        String priceStr = extractValue(line, "Price:", ",").trim();

                        int qty = Integer.parseInt(qtyStr);
                        double price = Double.parseDouble(priceStr);

                        // Get product name from database
                        Product product = ProductDatabase.getProductById(productId);
                        String productName = product != null ? product.getName() : productId;

                        currentItems.add(new CartItem(productId, productName, price, qty));

                    } catch (Exception e) {
                        System.err.println("Error parsing item: " + line);
                        e.printStackTrace();
                    }
                }
            }

            // Save last order if exists
            if (currentOrderId != null) {
                saveLoadedOrder(currentOrderId, currentName, currentPhone, currentAddress,
                        currentPaymentMethod, currentCcNumber, currentCcExpiry,
                        currentTotal, currentItems, currentDate, currentStatus);
            }

            System.out.println("✓ Loaded " + ORDERS.size() + " orders from final_orders.txt");

        } catch (IOException e) {
            System.err.println("Error loading orders: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void saveLoadedOrder(Long orderId, String name, String phone, String address,
                                        String paymentMethod, String ccNumber, String ccExpiry,
                                        double total, List<CartItem> items, Date date, String status) {
        OrderWithStatus order = new OrderWithStatus(
                orderId, name, phone, address, paymentMethod,
                ccNumber, ccExpiry, new ArrayList<>(items), total,
                status, date
        );
        ORDERS.put(orderId, order);
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
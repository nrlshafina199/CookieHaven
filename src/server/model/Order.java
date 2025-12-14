package server.model;

import java.util.List;
import server.model.CartItem;

public class Order {
    private final long orderId;
    private final String customerName;
    private final String phone;
    private final String address;
    private final String paymentMethod;
    private final String ccNumber; // <--- NEW FIELD
    private final String ccExpiry; // <--- NEW FIELD
    private final List<CartItem> items;
    private final double total;

    public Order(long orderId, String customerName, String phone, String address, String paymentMethod,
                 String ccNumber, String ccExpiry,
                 List<CartItem> items, double total) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.phone = phone;
        this.address = address;
        this.paymentMethod = paymentMethod;
        this.ccNumber = ccNumber;   // Initialized
        this.ccExpiry = ccExpiry;   // Initialized
        this.items = items;
        this.total = total;
    }

    // Formats the order details for saving to a file
    public String toText() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("--- FINAL ORDER #%d ---\n", orderId));
        sb.append(String.format("Date: %s\n", new java.util.Date().toString()));
        sb.append(String.format("Name: %s\n", customerName));
        sb.append(String.format("Phone: %s\n", phone));
        sb.append(String.format("Address: %s\n", address));
        sb.append(String.format("Payment: %s\n", paymentMethod));

        // Include payment details for verification (Will show "-" for COD/QR)
        if (!ccNumber.equals("-")) {
            sb.append(String.format("  Card #: %s\n", ccNumber));
            sb.append(String.format("  Expiry: %s\n", ccExpiry));
        }

        sb.append(String.format("Total: RM %.2f\n", total));
        sb.append("Items:\n");
        for (CartItem item : items) {
            sb.append(String.format("  - Product ID: %s (Qty: %d, Price: %.2f, Subtotal: %.2f)\n",
                    item.getProductId(), item.getQuantity(), item.getPrice(), item.getPrice() * item.getQuantity()));
        }
        sb.append("-------------------------\n\n");
        return sb.toString();
    }
}
package server.model;

import java.io.Serializable;
import java.util.*;

public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final long orderId;
    protected final String customerName;
    protected final String phone;
    protected final String address;
    protected final String paymentMethod;
    protected final String ccNumber;
    protected final String ccExpiry;
    protected final List<CartItem> items;
    protected final double total;
    protected final String status;
    protected final Date orderDate;

    public Order(long orderId, String customerName, String phone, String address,
                 String paymentMethod, String ccNumber, String ccExpiry,
                 List<CartItem> items, double total) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.phone = phone;
        this.address = address;
        this.paymentMethod = paymentMethod;
        this.ccNumber = ccNumber;
        this.ccExpiry = ccExpiry;
        this.items = items;
        this.total = total;
        this.status = "Pending";
        this.orderDate = new Date();
    }

    // --- GETTERS (Fixed: All symbols now exist) ---
    public long getOrderId() { return orderId; }
    public String getCustomerName() { return customerName; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getCcNumber() { return ccNumber; } // This fixes your error
    public String getCcExpiry() { return ccExpiry; } // Added for safety
    public List<CartItem> getItems() { return items; }
    public double getTotal() { return total; }
    public String getStatus() { return status; }
    public Date getOrderDate() { return orderDate; }

    public String toText() {
        return String.format("ID: %d | Customer: %s | Total: RM %.2f | Status: %s | Phone: %s\n",
                orderId, customerName, total, status, phone);
    }
}
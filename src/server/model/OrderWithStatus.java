package server.model;

import java.util.Date;
import java.util.List;

public class OrderWithStatus extends Order {
    private final String status;
    private final Date orderDate;

    public OrderWithStatus(long orderId, String customerName, String phone, String address,
                           String paymentMethod, String ccNumber, String ccExpiry,
                           List<CartItem> items, double total, String status, Date orderDate) {
        // Calls the constructor of the Order class
        super(orderId, customerName, phone, address, paymentMethod, ccNumber, ccExpiry, items, total);
        this.status = status;
        this.orderDate = orderDate;
    }

    @Override
    public String getStatus() { return status; }

    @Override
    public Date getOrderDate() { return orderDate; }
}
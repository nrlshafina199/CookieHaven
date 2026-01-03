package server.model;

import java.io.Serializable;

public class CartItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String productId;
    private String productName;
    private double price;
    private int quantity;

    public CartItem(String productId, String productName, double price, int quantity) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
    }

    // --- GETTERS (Fixes the "Cannot resolve method" errors) ---

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }

    // FIX: This method calculates the total for this specific row (Price * Qty)
    public double getSubtotal() {
        return this.price * this.quantity;
    }

    // --- SETTERS (Optional, but useful) ---

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
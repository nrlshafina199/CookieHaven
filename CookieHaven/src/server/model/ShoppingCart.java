package server.model;

import java.io.Serializable;
import java.util.*;

public class ShoppingCart implements Serializable {
    private final List<CartItem> items = new ArrayList<>();

    // FIX: Updated parameters to include 'name'
    public void addItem(String id, String name, double price, int qty) {
        for (CartItem item : items) {
            if (item.getProductId().equals(id)) {
                item.setQuantity(item.getQuantity() + qty);
                return;
            }
        }
        // This now matches the (String, String, double, int) constructor
        items.add(new CartItem(id, name, price, qty));
    }

    public void removeItem(String id) {
        items.removeIf(item -> item.getProductId().equals(id));
    }

    public List<CartItem> getItems() {
        return items;
    }

    public double calculateTotal() {
        return items.stream().mapToDouble(CartItem::getSubtotal).sum();
    }
}
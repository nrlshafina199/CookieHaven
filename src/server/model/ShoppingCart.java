package server.model;

import java.util.ArrayList;
import java.util.List;

public class ShoppingCart {
    // List of items in the cart.
    private final List<CartItem> items = new ArrayList<>();

    public void addItem(String productId, int quantity) {
        // Find existing item
        for (CartItem item : items) {
            if (item.getProductId().equals(productId)) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }


        double price = 5.00; // Default price

        if (productId.equals("CHIP001")) {
            price = 5.00;
        } else if (productId.equals("OAT002")) {
            price = 6.50;
        }

        // Add new item using the price determined above
        items.add(new CartItem(productId, quantity, price));
    }

    public void updateItem(String productId, int newQuantity) {
        // Find and set new quantity
        items.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .ifPresent(item -> item.setQuantity(newQuantity));
    }

    public void deleteItem(String productId) {
        items.removeIf(item -> item.getProductId().equals(productId));
    }

    public int getTotalItems() {
        // Safe calculation using the method reference
        return items.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double calculateTotal() {
        // Safe calculation using the method reference
        return items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public List<CartItem> getItems() {
        return items;
    }
}
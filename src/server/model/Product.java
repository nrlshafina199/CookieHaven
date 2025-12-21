package server.model;

import java.io.Serializable;

public class Product implements Serializable {
    private String id;
    private String name;
    private double price;
    private int stock;
    private String description;
    private String ingredients;
    private String allergens;

    public Product(String id, String name, double price, int stock, String description, String ingredients, String allergens) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.description = description;
        this.ingredients = ingredients;
        this.allergens = allergens;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public String getDescription() { return description; }
    public String getIngredients() { return ingredients; }
    public String getAllergens() { return allergens; }
}
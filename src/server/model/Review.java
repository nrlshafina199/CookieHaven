package server.model;

public class Review {
    public int id;
    public String product;
    public String text;
    public int rating;
    public String image;
    public boolean approved;

    public Review(int id, String product, String text, int rating, String image) {
        this.id = id;
        this.product = product;
        this.text = text;
        this.rating = rating;
        this.image = image;
        this.approved = false;
    }
}

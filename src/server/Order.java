package server;

public class Order {
    private String name, phone, email, cookie, method, address, allergy, privacy;
    private int qty;

    public Order(String name, String phone, String email, String cookie, int qty, String method, String address, String allergy, String privacy) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.cookie = cookie;
        this.qty = qty;
        this.method = method;
        this.address = address;
        this.allergy = allergy;
        this.privacy = privacy;
    }

    public String toText() {
        return name + "," + phone + "," + email + "," + cookie + "," + qty + "," + method + "," + address + "," + allergy + "," + privacy + "\n";
    }
}


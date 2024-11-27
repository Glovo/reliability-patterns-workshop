package stability;

public class Item {
    private long id;
    private String name;
    private int quantity;

    public Item() {
    }

    public Item(long id, String name, int quantity) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getQuantity() {
        return quantity;
    }
}

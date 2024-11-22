package stability;

import java.util.List;

public class Order {
    private final long id;
    private final List<Item> items;
    private final long userId;

    public Order(long id, List<Item> items, long userId) {
        this.id = id;
        this.items = items;
        this.userId = userId;
    }

    public long getId() {
        return id;
    }

    public List<Item> getItems() {
        return items;
    }

    public long getUserId() {
        return userId;
    }
}

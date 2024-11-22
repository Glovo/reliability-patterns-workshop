package fixtures;

import stability.Item;
import stability.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class TestHelpers {

    private static final Set<String> ITEMS = Set.of("burger", "pizza", "sushi", "poke", "");

    public static List<Order> someValidOrders(int ordersCount) {
        Random random = new Random();
        List<Order> orders = new ArrayList<>();

        for (int i = 1; i <= ordersCount; i++) {
            String randomItem = ITEMS.stream().skip(random.nextInt(ITEMS.size())).findFirst().orElse("");
            orders.add(new Order((long) i, List.of(new Item((long) i, randomItem, 1)), (long) i));
        }

        return orders;
    }
}

import com.github.mrmks.utils.IntQueue;
import com.github.mrmks.utils.StringIntMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtilsTest {
    @Test
    public void testIntQueue() {
        IntQueue queue = new IntQueue();

        for (int i = 0; i < 14; i++) queue.offer(i);

        while (!queue.isEmpty()) queue.remove();

        for (int i = 15; i < 31; i++) queue.offer(i);

        while (!queue.isEmpty()) queue.remove();
        for (int i = 0; i < 17; i++) queue.offer(i);
        while (!queue.isEmpty()) queue.remove();
    }

    @Test
    public void testDoubleCast() {
        System.out.println((int) -50.8);
        System.out.println((int) 50.8);
    }

    @Test
    public void testStringIntMap() {
        StringIntMap map = new StringIntMap();

        for (int i = 0; i < 25; i++) {
            map.put(Integer.toString(i), i);
        }

        Assertions.assertEquals(25, map.size());

        Assertions.assertEquals(2, map.get("2"));

        Assertions.assertEquals(16, map.get("16"));

        Assertions.assertEquals(7, map.remove("7"));
        Assertions.assertEquals(24, map.size());

        Assertions.assertEquals(11, map.remove("11"));
        Assertions.assertEquals(21, map.remove("21"));
        Assertions.assertEquals(22, map.size());
        Assertions.assertFalse(map.containKey("21"));

        map.put("7", 7);
        map.put("11", 11);
        map.put("21", 21);

        for (int i = 0; i < 25; i++) {
            Assertions.assertEquals(i, map.remove(Integer.toString(i)));
        }
    }
}

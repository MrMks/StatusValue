import com.github.mrmks.utils.IntQueue;
import com.github.mrmks.utils.ObjectIntRoMap;
import com.github.mrmks.utils.StringIntMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Iterator;

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
        int tarLen = 128;
        StringIntMap map = new StringIntMap(tarLen);

        // operations when map is empty;
        Assertions.assertEquals(-10, map.getOrDefault("", -10));
        Assertions.assertEquals(0, map.size());
        Assertions.assertTrue(map.isEmpty());
        Assertions.assertEquals(0, map.remove(""));
        Assertions.assertFalse(map.iterator().hasNext());

        for (int i = 0; i < tarLen; i++) {
            map.put(Integer.toString(i), i);
        }

        Assertions.assertEquals(tarLen, map.size());

        Assertions.assertEquals(2, map.getOrDefault("2", 0));

        Assertions.assertEquals(16, map.getOrDefault("16", 0));

        Assertions.assertEquals(7, map.remove("7"));
        Assertions.assertEquals(tarLen - 1, map.size());

        Assertions.assertEquals(11, map.remove("11"));
        Assertions.assertEquals(21, map.remove("21"));
        Assertions.assertEquals(tarLen - 3, map.size());
        Assertions.assertFalse(map.containKey("21"));

        map.put("7", 7);
        map.put("11", 11);
        map.put("21", 21);

        ObjectIntRoMap<String> roMap = map.readMap();

        for (int i = 0; i < tarLen; ++i) {
            Assertions.assertEquals(i, roMap.getOrDefault(Integer.toString(i), -1));
        }

        for (int i = 0; i < tarLen; ++i) {
            Assertions.assertEquals(i, map.remove(Integer.toString(i)));
        }

        map.clear();
        testIntMapRemoveLink();
    }

    @Test
    public void testIntMapRemoveLink() {
        StringIntMap map = new StringIntMap();
        map.put("0", 0);
        map.put("11", 11);
        map.put("22", 22);

        Iterator<String> ki = map.keyIterator();
        while (ki.hasNext()) {
            System.out.println(ki.next());
        }

        map.remove("11");
    }
}

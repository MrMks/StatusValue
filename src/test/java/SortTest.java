import com.github.mrmks.mc.status.utils.IntMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class SortTest {
    @Test
    public void testSort() {
        IntMap<String> map = new IntMap<>();
        Random random = new Random();
        int[] c = new int[3];
        for (int i = 0; i < 33; i++) {
            if (i % 10 == 3) {
                map.put(c[i / 10] = random.nextInt(), String.valueOf(i));
            } else {
                map.put(random.nextInt(), String.valueOf(i));
            }
        }

        //map.put(Integer.MAX_VALUE, "33");
        //map.put(Integer.MIN_VALUE, "34");

        Assertions.assertEquals("3", map.remove(c[0]));
        Assertions.assertEquals("13", map.remove(c[1]));
        Assertions.assertEquals("23", map.remove(c[2]));
        Assertions.assertNull(map.remove(c[0]));
    }
}

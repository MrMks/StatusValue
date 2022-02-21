import com.github.mrmks.mc.status.utils.IntQueue;
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
}

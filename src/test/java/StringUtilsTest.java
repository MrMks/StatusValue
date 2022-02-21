import com.github.mrmks.utils.ekey.EKey;
import com.github.mrmks.utils.ekey.EKeyBuilder;
import com.github.mrmks.utils.ekey.EKeyHandler;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

public class StringUtilsTest {
    private static class TestKey extends EKey {
        protected TestKey(String s, int i){
            super(s,i);
        }
    }

    @Test
    public void textHandler() {
        EKeyBuilder<TestKey> builder = new EKeyBuilder<>();
        builder.add("z", "a", "b", "c", "d", "ad", "ab", "am").add("am");
        EKeyHandler<TestKey> handler = builder.build(TestKey::new);

        System.out.println(handler.of(""));
        System.out.println(handler.of("ac"));
        System.out.println(handler.of("an"));

        handler = new EKeyBuilder<TestKey>().add("b").build(TestKey::new);

        System.out.println(handler.of("a"));
        System.out.println(handler.of("c"));
    }
}

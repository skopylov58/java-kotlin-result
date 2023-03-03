package result;

import static org.junit.Assert.*;

import org.junit.Test;

public class RunnableResultTest {
    
    @Test
    public void testRunnable() throws Exception {
        
        Result<Void> run = Result.of(() -> System.out.println("Runnable"));
        assertTrue(run.isSuccess());
        Void v = run.getOrNull();
        assertNull(v);
        
        switch(run) {
        case Success<Void>(Void val) -> assertNull(val);
        case Failure f -> fail();
        default -> fail();
        }
    }

}

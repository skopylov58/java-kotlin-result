package result;

public class MainTest {
    
    public void testRecordPatternMatching() throws Exception {
        var s = Success.of(1);
        
        if (s instanceof Success<Integer>(Integer value) ) {
            value.toHexString(0);
        }
        
    }
}

package result;

import java.net.URL;
import java.util.List;

public class MainTest {
    
    public void testRecordPatternMatching() throws Exception {
        var s = Success.of(1);
        
        if (s instanceof Success<Integer>(Integer value) ) {
            value.toHexString(0);
        }
        
    }
    
    List<URL> foo(List<String> urls) {
        return urls.stream()
        .map(Result.lift(URL::new))
        .flatMap(Result::stream)
        .toList();
    }
    
}

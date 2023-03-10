package result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.NoSuchElementException;

public class ComposeTest {

    sealed interface Option<T> permits Some, None {}
    record Some<T>(T value) implements Option<T> {}
    record None<T>() implements Option<T> {}
    
    @org.junit.Test
    public void testResult() throws Exception {
        
        var res1 = Success.of(1);
        var res2 = Success.of(2);

        Result<Integer> sum = sumResultsNaive(res1, res2);
        assertTrue(sum.isSuccess());
        assertEquals(3, sum.getOrNull().intValue());

        sum = sumResultsFlatMap(res1, res2);
        assertTrue(sum.isSuccess());
        assertEquals(3, sum.getOrNull().intValue());

    }

    Result<Integer> sumResultsNaive(Result<Integer> i1, Result<Integer> i2) {
        if (i1.isSuccess() && i2.isSuccess()) {
            Integer x1 = i1.getOrNull();
            Integer x2 = i2.getOrNull();
            return Success.of(sum(x1, x2));
        } 
        return i1.isFailure() ? i1 : i2;
    }

    Result<Integer> sumResultsFlatMap(Result<Integer> i1, Result<Integer> i2) {
        return i1.flatMap(x1 -> i2.map(x2 -> sum(x1, x2)));
    }
    
    Result<Integer> sumResultsPatternMatching(Result<Integer> i1, Result<Integer> i2) {
        record TwoInts(Result<Integer> i1, Result<Integer> i2) {};
        return switch (new TwoInts(i1,i2)) {
        case TwoInts(Success<Integer>(Integer x1), Success<Integer>(Integer x2)) -> Success.of(sum(x1, x2));
        default -> i1.isFailure() ? i1 : i2;
        };
    }

    Result<Integer> sumResultsFromDifferentMonads(Option<Integer> i1, Result<Integer> i2) {
        record TwoInts(Option<Integer> i1, Result<Integer> i2) {};
        return switch (new TwoInts(i1,i2)) {
        case TwoInts(Some<Integer>(Integer x1), Success<Integer>(Integer x2)) -> Success.of(sum(x1, x2));
        default -> i2.isFailure() ? i2 : Failure.of(new NoSuchElementException()); 
        };
    }

    
    
    int sum(int i1, int i2) {
        return i1+i2;
    }

}
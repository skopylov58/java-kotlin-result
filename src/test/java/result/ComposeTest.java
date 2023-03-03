package result;

import junit.framework.Test;
import static org.junit.Assert.*;

import java.util.NoSuchElementException;
import java.util.Optional;

public class ComposeTest {


//    @org.junit.Test
//    public void test0() throws Exception {
//        
//        var opt1 = Optional.of(1);
//        var opt2 = Optional.of(2);
//        
//        
//        record Product(Optional<Integer> i1, Optional<Integer> i2) {};
//        
//        var pr = new Product(opt1, opt2);
//        var s = switch (pr) {
//            case Product(Optional.of(var i1), Optional.of(var i2)) -> "";
//            default -> "";
//        };
//    }

    sealed interface Option<T> permits Some, None {}
    record Some<T>(T value) implements Option<T> {}
    record None<T>() implements Option<T> {}
    
    @org.junit.Test
    public void testResult() throws Exception {
        
        var res1 = Success.of(1);
        var res2 = Success.of(2);
        
        
        record Product(Result<Integer> i1, Result<Integer> i2) {};
        
        var pr = new Product(res1, res2);
        var s = switch (pr) {
            case Product(Success<Integer>(Integer i1), Success<Integer>(Integer i2)) -> "";
            default -> "";
        };
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
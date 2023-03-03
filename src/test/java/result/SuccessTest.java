package result;

import static org.junit.Assert.*;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Test;


public class SuccessTest {
    
    @Test
    public void testGet() {
        
        var s = Success.of(1);
        
        Integer expected = Integer.valueOf(1);
        
        assertEquals(expected, s.getOrNull());
        assertEquals(expected, s.getOrDefault(-1));
        assertEquals(expected, s.getOrElse(e -> -1));
        assertEquals(null, s.exceptionOrNull());
        
        try {
            Integer i = s.getOrThrow();
            assertEquals(expected, i);
        } catch (Exception e1) {
            fail("Should not throw");
        }
    }

    @Test
    public void testIsSuccess() throws Exception {
        var s = Success.of(1);
        
        assertTrue(s.isSuccess());
        assertFalse(s.isFailure());
    }
    
    @Test
    public void testMap() throws Exception {
        var s = Success.of(1);
        var mapped = s.map(i -> i * 2);
        assertTrue(mapped instanceof Success);
        assertEquals(Integer.valueOf(2), mapped.getOrNull());
        
        var mappCatching = s.mapCatching(i -> i*3);
        assertTrue(mappCatching instanceof Success);
        assertEquals(Integer.valueOf(3), mappCatching.getOrNull());

        var flatMapped = s.flatMap(i -> Success.of("one"));
        assertTrue(flatMapped instanceof Success);
        assertEquals("one", flatMapped.getOrNull());
    }
    
    @Test
    public void testOn() throws Exception {
        var s = Success.of(1);
        var cons = new Cons<Integer>();
        s.onSuccess(cons);
        assertTrue(cons.isAccepted());
        s.onFailure(e -> fail("Should not happen for success"));
        
        var caught = s.onSuccessCatching(i -> {throw new NullPointerException();});
        assertTrue(caught instanceof Failure);
    }
    
    @Test
    public void testOpt() throws Exception {
        var s = Success.of(1);
        
        var opt = s.optional();
        assertTrue(opt instanceof Optional);
        assertEquals(1, opt.get().intValue());
        
        var st = s.stream();
        var list = st.toList();
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).intValue());
        
    }
    
    @Test
    public void testRecover() throws Exception {
        var s = Success.of(1);
        
        var recovered = s.recover(e -> 2);
        assertTrue(s.equals(recovered));
        assertEquals(1, recovered.getOrNull().intValue());
        
        var recCatching = s.recoverCatching(e -> {
            throw new IllegalStateException();
        });
        
        assertTrue(s.equals(recCatching));
        assertEquals(1, ((Integer) recCatching.getOrNull()).intValue());
    }
    
    @Test
    public void testFilter() throws Exception {
        var s = Success.of(1);
        
        var filtered = s.filter(i -> i > 0);
        assertEquals(s, filtered);
        
        filtered = s.filter(i -> i%2==0);
        assertTrue(filtered instanceof Failure);
        assertTrue(filtered.exceptionOrNull() instanceof NoSuchElementException);
    }
    
    public static class Cons<T> implements Consumer<T>{
        boolean accepted = false; 
        @Override
        public void accept(T t) {
            accepted = true;
        }
        
        boolean isAccepted() {
            return accepted;
        }
    }
    
}

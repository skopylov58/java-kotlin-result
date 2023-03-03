package result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Test;


public class FailureTest {
    
    @Test
    public void testGet() {
        
        var f = Failure.of(new FileNotFoundException());
        
        assertEquals(null, f.getOrNull());
        assertEquals(-1, f.getOrDefault(-1));
        assertEquals(-3, f.getOrElse(e -> -3));
        
        var e = f.exceptionOrNull();
        assertNotNull(e);
        assertTrue(e instanceof FileNotFoundException);
        
        try {
            var i = f.getOrThrow();
            fail("Should throw");
        } catch (Exception e1) {
            assertTrue(e1 instanceof FileNotFoundException);
        }
    }

    @Test
    public void testIsSuccess() throws Exception {
        var f = Failure.of(new FileNotFoundException());
        
        assertTrue(f.isFailure());
        assertFalse(f.isSuccess());
    }
    
    @Test
    public void testMap() throws Exception {
        Result<Integer> f = Failure.of(new FileNotFoundException());
        var mapped = f.map(i -> i * 2);
        assertTrue(mapped instanceof Failure);
        assertEquals(f, mapped);
        
        var mappCatching = f.mapCatching(i -> i*3);
        assertTrue(mappCatching instanceof Failure);
        assertEquals(f, mappCatching);

        var flatMapped = f.flatMap(i -> Success.of("one"));
        assertTrue(flatMapped instanceof Failure);
        assertEquals(f, flatMapped);
    }
    
    @Test
    public void testOn() throws Exception {
        Result<Integer> f = Failure.of(new FileNotFoundException());
        var cons = new SuccessTest.Cons<Exception>();
        f.onSuccess(i -> fail("Should not happen for failure"));
        f.onFailure(cons);
        assertTrue(cons.isAccepted());
        
        var caught = f.onSuccessCatching(i -> {throw new NullPointerException();});
        assertEquals(f, caught);
    }
    
    @Test
    public void testOpt() throws Exception {
        var f = Failure.of(new FileNotFoundException());
        
        var opt = f.optional();
        assertTrue(opt instanceof Optional);
        assertTrue(opt.isEmpty());
        
        var st = f.stream();
        var list = st.toList();
        assertEquals(0, list.size());
        
    }
    
    @Test
    public void testRecover() throws Exception {
        Result<Integer> f = Failure.of(new FileNotFoundException());
        
        var recovered = f.recover(e -> 2);
        assertTrue(recovered instanceof Success);
        assertEquals(2, recovered.getOrNull().intValue());
        
        var recCatching = f.recoverCatching(e -> 3);
        assertTrue(recCatching instanceof Success);
        assertEquals(3, recCatching.getOrNull().intValue());

        recCatching = f.recoverCatching(e -> {
            throw new IllegalStateException();
        });
        assertTrue(recCatching instanceof Failure);
        assertTrue(recCatching.exceptionOrNull() instanceof IllegalStateException);
    }
    
    @Test
    public void testFilter() throws Exception {
        Result<Integer> f = Failure.of(new FileNotFoundException());
        
        var filtered = f.filter(i -> i > 0);
        assertEquals(f, filtered);
    }

}

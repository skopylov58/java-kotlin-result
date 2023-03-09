package result;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.Socket;
import java.util.function.Consumer;

import org.junit.Test;

public class AutocloseableTest {

    @Test
    public void testClose() throws Exception {
        var myCloseable = new MyClosable(false);
        var success = Success.of(myCloseable);
        try (success) {
            
        }
        assertTrue(myCloseable.isClosedInvoked());
    }
    
    
    @Test
    public void testCloseWithException() throws Exception {
        var exceptionHandler = new ExceptionHandler();
        Failure.withInterceptor(exceptionHandler);
        var myCloseable = new MyClosable(true);
        var success = Success.of(myCloseable);
        try (success) {
            
        }
        assertTrue(myCloseable.isClosedInvoked());
        assertTrue(exceptionHandler.exceptionHandled);
        Failure.withInterceptor(null);
    }
    
    @Test
    public void testSocket() throws Exception {
        try (var socket = Result.runCatching(() -> new Socket("localhost", 1234))) {
            socket.mapCatching(Socket::getOutputStream)
            .onSuccessCatching(o -> o.write(new byte[] { 1, 2, 3, 4 }))
            .onFailure(e -> System.out.println(e));
        };
        
    }
    
    
    class MyClosable implements AutoCloseable {
        boolean closedInvoked = false;
        final boolean throwException;
        
        public MyClosable(boolean throwException) {
            this.throwException = throwException;
        }
        
        @Override
        public void close() throws Exception {
            closedInvoked = true;
            if (throwException) {
                throw new IOException();
            }
        }
        
        boolean isClosedInvoked() {
            return closedInvoked;
        }
    }
    
    class ExceptionHandler implements Consumer<Exception> {
        boolean exceptionHandled = false;
        @Override
        public void accept(Exception t) {
            exceptionHandled = true;
        }
        
    }
}

package result;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 
 * Failure result.
 * 
 * @author skopylov
 *
 * @param <T> result type
 */
public record Failure<T>(Exception exception) implements Result<T> {

    private static AtomicReference<Consumer<Exception>> interceptorRef = new AtomicReference<>();
    
    /**
     * Customizing constructor. Invokes exception intercepter if it is set.
     */
    public Failure {
        Consumer<Exception> interc = interceptorRef.get();
        if (interc != null) {
            interc.accept(exception);
        }
    }
    /**
     * Factory method to produce Failure result.
     * @param <T> result type
     * @param e exception
     * @return Failure result
     */
    static <T> Result<T> of(Exception e) {
        return new Failure<T>(e);
    }

    /**
     * Sets global exception interceptor.
     * @param interceptor exception interceptor
     * @return previous old interceptor
     */
    static Consumer<Exception> withInterceptor(Consumer<Exception> interceptor) {
        return interceptorRef.getAndSet(interceptor);
    }
    
    @Override
    public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Exception, ? extends R> onError) {
        return onError.apply(exception);
    }

    @Override
    public T getOrThrow() throws Exception {
        throw exception;
    }
    @Override
    public void close() {
        // nothing to close
    }

}
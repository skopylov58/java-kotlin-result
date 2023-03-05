package result;

import java.util.function.Consumer;
import java.util.function.Function;

public record Failure<T>(Exception exception) implements Result<T> {

    private transient static final Consumer<Exception> interceptor;
    
    static {
        String className = System.getProperty("result.interceptor");
        if (className != null) {
            try {
                interceptor = (Consumer<Exception>) Class.forName(className).getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            interceptor = null; 
        }
    }
    
    public Failure {
        if (interceptor != null) {
            interceptor.accept(exception);
        }
    }
    
    static <T> Result<T> of(Exception e) {
        return new Failure<T>(e);
    }
    
    @Override
    public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Exception, ? extends R> onError) {
        return onError.apply(exception);
    }

    @Override
    public T getOrThrow() throws Exception {
        throw exception;
    }

}
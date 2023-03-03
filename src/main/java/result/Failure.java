package result;

import java.util.function.Function;

public record Failure<T>(Exception exception) implements Result<T> {

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
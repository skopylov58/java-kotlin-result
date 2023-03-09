package result;

import java.util.function.Function;

/**
 * Success result.
 * 
 * @author skopylov
 *
 * @param <T>
 */
public record Success<T>(T value) implements Result<T> {

    public static <T> Success<T> of(T t) {
        return new Success<>(t);
    }

    @Override
    public <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Exception, ? extends R> onError) {
        return onSuccess.apply(value);
    }

    @Override
    public T getOrThrow() throws Exception {
        return value;
    }

    @Override
    public void close() {
        if (value instanceof AutoCloseable auto) {
            try {
                auto.close();
            } catch (Exception e) {
                Failure.of(e);  //
            }
        }
    }
}
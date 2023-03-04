package result;

import java.util.function.Function;

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

}
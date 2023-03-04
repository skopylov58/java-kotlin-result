package result;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public sealed interface Result<T> permits Success, Failure {
    
    final Exception NO_SUCH_ELEMENT = new NoSuchElementException();
    
    @SuppressWarnings("unchecked")
    default <R> Result<R> map(Function<? super T, ? extends R> mapper) {
        return (Result<R>) fold(v -> Success.of(mapper.apply(v)), __ -> this);
    }
    
    @SuppressWarnings("unchecked")
    default <R> Result<R> flatMap(Function<? super T, ? extends Result<? extends R>> mapper) {
        return (Result<R>) fold(v -> mapper.apply(v), __ -> this);
    }
    
    @SuppressWarnings("unchecked")
    default <R> Result<R> mapCatching(CheckedFunction<? super T, ? extends R> mapper) {
        return (Result<R>) fold(__ -> flatMap(Result.lift(mapper)), __ -> this);
    }

    default Result<T> filter(Predicate<? super T> predicate) {
        return fold(v -> predicate.test(v) ? this : Failure.of(NO_SUCH_ELEMENT), __ -> this);
    }
    
    default Result<T> onFailure(Consumer<Exception> errHandler) {
        return fold(__ -> this, e -> {errHandler.accept(e); return this;});
    }

    default Result<T> onSuccess(Consumer<T> consumer) {
        return fold(v -> {consumer.accept(v); return this;}, e -> this);
    }

    default Result<T> onSuccessCatching(CheckedConsumer<T> consumer) {
        return fold(v -> lift(toFunction(consumer)).apply(v), e -> this);
    }

    /**
     * Converts this result to {@code Optional<T>}.
     * @return {@code Optional<T>} for Success or Optional.empty() for Failure.
     */
    default Optional<T> optional() {
        return fold(v -> Optional.of(v), __ -> Optional.empty());
    }
    
    /**
     * Converts this result to {@code Stream<T>}.
     * @return {@code Stream<T>} for Success or Stream.empty() for Failure.
     */
    default Stream<T> stream() {
        return fold(v -> Stream.of(v), __ -> Stream.empty());
    }
    
    default boolean isSuccess() {
        return fold(__ -> true, __ -> false);
    }
    
    default boolean isFailure() {
        return !isSuccess();
    }
    
    default T getOrNull() {
        return fold(v -> v, __ -> null);
    }

    default T getOrDefault(T defaultValue) {
        return getOrElse(e -> defaultValue);
    }
    
    default T getOrElse(Function<? super Exception, ? extends T> func) {
        return fold(v -> v, e -> func.apply(e));
    }

    default Exception exceptionOrNull() {
        return fold(__ -> null, e -> e);
    }
    

    @SuppressWarnings("unchecked")
    default <R> Result<R> recover(Function<? super Exception, ? extends R> func) {
        return (Result<R>) fold(__ -> this, e -> Success.of(func.apply(e)));
    }
    
    @SuppressWarnings("unchecked")
    default <R> Result<R> recoverCatching(CheckedFunction<? super Exception, ? extends R> func) {
        return (Result<R>) fold(__ -> this, e -> lift(func).apply(e));
    }
    
    T getOrThrow() throws Exception;
    <R> R fold(Function<? super T, ? extends R> onSuccess, Function<? super Exception, ? extends R> onError);
    
    
    /**Supplier that may throw an exception */
    @FunctionalInterface
    interface CheckedSupplier<T> {T get() throws Exception;}
    
    /**Function that may throw an exception */
    @FunctionalInterface
    interface CheckedFunction<T, R> {R apply(T t) throws Exception;}

    /**Consumer that may throw an exception */
    @FunctionalInterface
    interface CheckedConsumer<T> {void accept(T t) throws Exception;}

    /**Runnable that may throw an exception */
    @FunctionalInterface
    interface CheckedRunnable {void run() throws Exception;}

    /**
     * Factory method to produce Result from supplier that may throw an exception.
     * @param <T> Result type
     * @param supplier supplier that may throw an exception.
     * @return {@code Result<T>}
     */
    static <T> Result<T> of(CheckedSupplier<T> supplier) {
        return lift(toFunction(supplier)).apply(null);
    }

    /**
     * Factory method to produce Result from runnable that may throw an exception/
     * @param runnable runnable
     * @return {@code Result<Void>}
     */
    static Result<Void> of(CheckedRunnable runnable) {
        return lift(toFunction(runnable)).apply(null);
    }
    
    /**
     * Converts partial function that may throw an exception to total function returning {@code Result<R>}.
     * @param <T> function parameter type
     * @param <R> function result type
     * @param partialFunc function that may throw an exception
     * @return total function returning {@code Result<R>}.
     */
    static <T, R> Function<T, Result<R>> lift(CheckedFunction<T, R> partialFunc) {
        return (T t) -> {
            try {
                return new Success<>(partialFunc.apply(t));
            } catch (Exception e) {
                return new Failure<>(e);
            }
        };
    }
    
    /**
     * Converts supplier to function.
     */
    static <U> CheckedFunction<Object, U> toFunction(CheckedSupplier<U> supplier) {
        return (Object o) -> supplier.get();
    }

    /**
     * Converts consumer to function.
     */
    static <T> CheckedFunction<T, T> toFunction(CheckedConsumer<T> consumer) {
        return (T t) -> {
            consumer.accept(t);
            return t;
        };
    }
    
    /**
     * Converts runnable to function.
     */
    static CheckedFunction<Object, Void> toFunction(CheckedRunnable runnable) {
        return (Object o) -> {
            runnable.run();
            return null;
        };
    }
    
    
}


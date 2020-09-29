package net.maxsmr.commonutils.rx.functions;

import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Function;


/**
 * {@link Function} без обязательной
 * проверки на {@link Exception}
 *
 * @see Function
 */
public interface FunctionSafe<T, R> extends Function<T, R> {
    @Override
    R apply(@NonNull T t);
}

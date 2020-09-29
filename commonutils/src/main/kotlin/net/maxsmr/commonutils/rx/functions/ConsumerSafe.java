package net.maxsmr.commonutils.rx.functions;


import io.reactivex.functions.Consumer;

/**
 * {@link Consumer} без обязательной
 * проверки на {@link Exception}
 *
 * @see Consumer
 */
public interface ConsumerSafe<T> extends Consumer<T> {
    void accept(T t);
}

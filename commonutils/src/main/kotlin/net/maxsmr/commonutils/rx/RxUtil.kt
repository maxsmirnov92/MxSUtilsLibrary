package net.maxsmr.commonutils.rx

import io.reactivex.disposables.Disposable
import io.reactivex.exceptions.OnErrorNotImplementedException
import io.reactivex.plugins.RxJavaPlugins
import net.maxsmr.commonutils.rx.functions.ActionSafe
import net.maxsmr.commonutils.rx.functions.ConsumerSafe

val EMPTY_ACTION: ActionSafe = ActionSafe {}

val ON_ERROR_MISSING: ConsumerSafe<Throwable> = ConsumerSafe<Throwable> { throwable -> RxJavaPlugins.onError(OnErrorNotImplementedException(throwable)) }

fun isDisposableInactive(disposable: Disposable?): Boolean = disposable == null || disposable.isDisposed

fun isDisposableActive(disposable: Disposable?): Boolean = disposable != null && !disposable.isDisposed
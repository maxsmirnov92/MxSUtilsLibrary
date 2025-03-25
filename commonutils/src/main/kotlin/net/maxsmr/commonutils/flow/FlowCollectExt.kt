package net.maxsmr.commonutils.flow

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import net.maxsmr.commonutils.live.event.VmEvent

inline fun repeatOnLifecycle(
    owner: LifecycleOwner,
    lifecycleState: Lifecycle.State,
    crossinline action: suspend () -> Unit,
): Job {
    return owner.lifecycleScope.launch {
        owner.repeatOnLifecycle(lifecycleState) {
            action()
        }
    }
}

/**
 * Использовать, где LifecycleOwner не требуется (например, в VM)
 */
inline fun <T> Flow<T>.observe(
    scope: CoroutineScope,
    crossinline action: suspend (value: T) -> Unit,
): Job {
    return scope.launch {
        collectLatest {
            action(it)
        }
    }
}

inline fun <T> Flow<T>.observe(
    owner: LifecycleOwner,
    lifecycleState: Lifecycle.State = Lifecycle.State.RESUMED,
    crossinline action: suspend (value: T) -> Unit,
): Job {
    return repeatOnLifecycle(owner, lifecycleState) {
        collectLatest { action(it) }
    }
}

inline fun <T> StateFlow<VmEvent<T>?>.observeEvents(
    scope: CoroutineScope,
    crossinline action: (value: T) -> Unit
): Job {
    return scope.launch {
        collectLatest { event ->
            event?.get(true)?.let {
                action(it)
            }
        }
    }
}

inline fun <T> StateFlow<VmEvent<T>?>.observeEvents(
    owner: LifecycleOwner,
    lifecycleState: Lifecycle.State = Lifecycle.State.RESUMED,
    crossinline action: suspend (value: T) -> Unit,
): Job {
    return repeatOnLifecycle(owner, lifecycleState) {
        collectLatest { event ->
            event?.get(true)?.let {
                action(it)
            }
        }
    }
}
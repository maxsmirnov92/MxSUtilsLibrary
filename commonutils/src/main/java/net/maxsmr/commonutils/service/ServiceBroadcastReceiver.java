package net.maxsmr.commonutils.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.maxsmr.commonutils.Predicate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static net.maxsmr.commonutils.text.TextUtilsKt.isEmpty;

public class ServiceBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_SERVICE_CREATED = "service_created";
    public static final String ACTION_SERVICE_STARTED = "service_started";
    public static final String ACTION_SERVICE_DESTROYED = "service_destroyed";

    public static final String EXTRA_SERVICE_NAME = "service_name";

    @NotNull
    public final Set<Class<? extends Service>> serviceClasses = new LinkedHashSet<>();

    /**
     * @param serviceClasses null or empty if subscribe on all
     */
    @SuppressWarnings("unchecked")
    public ServiceBroadcastReceiver(Class<? extends Service>... serviceClasses) {
        if (serviceClasses != null) {
            this.serviceClasses.addAll(Arrays.asList(serviceClasses));
        }
    }

    @Override
    @CallSuper
    public void onReceive(@NotNull Context context, @Nullable Intent intent) {
        if (intent == null) return;
        String name = intent.getStringExtra(EXTRA_SERVICE_NAME);
        if (name != null && !isEmpty(name)) {
            Class<? extends Service> serviceClass = Predicate.Methods.find(serviceClasses, new Predicate<Class<? extends Service>>() {
                @Override
                public boolean apply(Class<? extends Service> element) {
                    return element.getName().equalsIgnoreCase(name);
                }
            });
            if (serviceClass != null) {
                final String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case ACTION_SERVICE_CREATED:
                            doActionOnCreate(name);
                            break;
                        case ACTION_SERVICE_STARTED:
                            doActionOnStart(name);
                            break;
                        case ACTION_SERVICE_DESTROYED:
                            doActionOnDestroyed(name);
                            break;
                    }
                }
            }
        }
    }

    @MainThread
    protected void doActionOnCreate(@NotNull String className) {
    }

    @MainThread
    protected void doActionOnStart(@NotNull String className) {
    }

    @MainThread
    protected void doActionOnDestroyed(@NotNull String className) {
    }

    public static <S extends Service> void sendBroadcastCreated(@NotNull S service) {
        sendBroadcast(service, ACTION_SERVICE_CREATED);
    }

    public static <S extends Service> void sendBroadcastStarted(@NotNull S service) {
        sendBroadcast(service, ACTION_SERVICE_STARTED);
    }

    public static <S extends Service> void sendBroadcastDestroyed(@NotNull S service) {
        sendBroadcast(service, ACTION_SERVICE_DESTROYED);
    }

    private static <S extends Service> void sendBroadcast(@NotNull S service, String action) {
        if (isEmpty(action)) {
            throw new IllegalArgumentException("empty action");
        }
        LocalBroadcastManager.getInstance(service).sendBroadcast(new Intent(action).putExtra(EXTRA_SERVICE_NAME, service.getClass().getName()));
    }

    public static void register(@NotNull Context context, @NotNull ServiceBroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SERVICE_CREATED);
        filter.addAction(ACTION_SERVICE_STARTED);
        filter.addAction(ACTION_SERVICE_DESTROYED);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
    }

    public static void unregister(@NotNull Context context, @NotNull ServiceBroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }
}

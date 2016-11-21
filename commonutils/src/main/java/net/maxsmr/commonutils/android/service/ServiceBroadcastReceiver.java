package net.maxsmr.commonutils.android.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.CallSuper;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class ServiceBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_SERVICE_CREATED = "service_created";
    public static final String ACTION_SERVICE_STARTED = "service_started";
    public static final String ACTION_SERVICE_DESTROYED = "service_destroyed";

    public static final String EXTRA_SERVICE_NAME = "service_name";

    @NonNull
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
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra(EXTRA_SERVICE_NAME);
        if (!TextUtils.isEmpty(name)) {
            boolean contains = true;
            if (!serviceClasses.isEmpty()) {
                contains = false;
                for (Class<? extends Service> c : serviceClasses) {
                    if (c.getName().equalsIgnoreCase(name)) {
                        contains = true;
                        break;
                    }
                }
            }
            if (contains) {
                switch (intent.getAction()) {
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

    @MainThread
    protected void doActionOnCreate(@NonNull String className) {
    }

    @MainThread
    protected void doActionOnStart(@NonNull String className) {
    }

    @MainThread
    protected void doActionOnDestroyed(@NonNull String className) {
    }

    public static <S extends Service> void sendBroadcastCreated(@NonNull S service) {
        sendBroadcast(service, ACTION_SERVICE_CREATED);
    }

    public static <S extends Service> void sendBroadcastStarted(@NonNull S service) {
        sendBroadcast(service, ACTION_SERVICE_STARTED);
    }

    public static <S extends Service> void sendBroadcastDestroyed(@NonNull S service) {
        sendBroadcast(service, ACTION_SERVICE_DESTROYED);
    }

    private static <S extends Service> void sendBroadcast(@NonNull S service, String action) {
        if (TextUtils.isEmpty(action)) {
            throw new IllegalArgumentException("empty action");
        }
        LocalBroadcastManager.getInstance(service).sendBroadcast(new Intent(action).putExtra(EXTRA_SERVICE_NAME, service.getClass().getName()));
    }

    public static void register(@NonNull Context context, @NonNull ServiceBroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SERVICE_CREATED);
        filter.addAction(ACTION_SERVICE_STARTED);
        filter.addAction(ACTION_SERVICE_DESTROYED);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);
    }

    public static void unregister(@NonNull Context context, @NonNull ServiceBroadcastReceiver receiver) {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver);
    }
}

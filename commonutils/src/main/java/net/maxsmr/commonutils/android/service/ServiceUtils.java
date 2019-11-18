package net.maxsmr.commonutils.android.service;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.text.TextUtils;

import net.maxsmr.commonutils.android.hardware.DeviceUtils;
import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ServiceUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ServiceUtils.class);

    private ServiceUtils() {
        throw new AssertionError("no instances.");
    }

    public static <S extends Service> boolean isServiceRunning(@NotNull Context context, @NotNull final Class<S> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            throw new RuntimeException(ActivityManager.class.getSimpleName() + " is null");
        }
        return Predicate.Methods.contains(manager.getRunningServices(Integer.MAX_VALUE), service -> serviceClass.getName().equals(service.service.getClassName()));
    }

    public static <S extends Service> boolean isServiceForeground(@NotNull Context context, @NotNull final Class<S> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            throw new RuntimeException(ActivityManager.class.getSimpleName() + " is null");
        }
        return Predicate.Methods.contains(manager.getRunningServices(Integer.MAX_VALUE), service -> serviceClass.getName().equals(service.service.getClassName()) && service.foreground);
    }

    public static <S extends Service> void startNoCheck(@NotNull Context context, @NotNull Class<S> serviceClass) {
        startNoCheck(context, serviceClass, null);
    }

    public static <S extends Service> void startNoCheck(@NotNull Context context, @NotNull Class<S> serviceClass, @Nullable Intent args) {
        context.startService(createServiceIntent(context.getPackageName(), serviceClass, args));
    }

    public static <S extends Service> void start(@NotNull Context context, @NotNull Class<S> serviceClass) {
        start(context, serviceClass, null);
    }

    public static <S extends Service> void start(@NotNull Context context, @NotNull Class<S> serviceClass, @Nullable Intent args) {
        if (!isServiceRunning(context, serviceClass)) {
            restartNoCheck(context, serviceClass, args);
        }
    }

    public static <S extends Service> void restart(@NotNull Context context, @NotNull Class<S> serviceClass) {
        restart(context, serviceClass, null);
    }

    public static <S extends Service> void restart(@NotNull Context context, @NotNull Class<S> serviceClass, @Nullable Intent args) {
        stop(context, serviceClass);
        startNoCheck(context, serviceClass, args);
    }

    private static <S extends Service> void stopNoCheck(@NotNull Context context, @NotNull Class<S> serviceClass) {
        Intent service = new Intent(context, serviceClass);
        context.stopService(service);
    }

    public static <S extends Service> void stop(@NotNull Context context, @NotNull Class<S> serviceClass) {
        logger.d("stop(), serviceClass=" + serviceClass);
        if (isServiceRunning(context, serviceClass)) {
            stopNoCheck(context, serviceClass);
        }
    }

    public static <S extends Service> void restartDelay(@NotNull Context context, @NotNull Class<S> serviceClass, long delay, boolean wakeUp) {
        restartDelay(context, serviceClass, null, delay, wakeUp);
    }

    public static <S extends Service> void restartDelay(@NotNull Context context, @NotNull Class<S> serviceClass, @Nullable Intent args, long delay, boolean wakeUp) {
        logger.d("restartDelay(), serviceClass=" + serviceClass + ", delay=" + delay);

        if (delay < 0) {
            throw new IllegalArgumentException("incorrect delay: " + delay);
        }
        stop(context, serviceClass);
        startDelayNoCheck(context, serviceClass, args, delay, wakeUp);
    }

    public static <S extends Service> void startDelayNoCheck(@NotNull Context context, @NotNull Class<S> serviceClass, @Nullable Intent args,
                                                             long delay, boolean wakeUp) {
        cancelDelay(context, serviceClass);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, createServiceIntent(context.getPackageName(), serviceClass, args),
                PendingIntent.FLAG_CANCEL_CURRENT);
        DeviceUtils.setAlarm(context, pendingIntent, System.currentTimeMillis() + delay, wakeUp ? DeviceUtils.AlarmType.RTC_WAKE_UP : DeviceUtils.AlarmType.RTC);
    }

    public static <S extends Service> void startDelay(@NotNull Context context, @NotNull Class<S> serviceClass, long delay, boolean wakeUp) {
        startDelay(context, serviceClass, null, delay, wakeUp);
    }

    public static <S extends Service> void startDelay(@NotNull Context context, @NotNull Class<S> serviceClass, @Nullable Intent args,
                                                      long delay, boolean wakeUp) {
        logger.d("startDelay(), serviceClass=" + serviceClass + ", delay=" + delay);
        if (!isServiceRunning(context, serviceClass)) {
            restartDelayNoCheck(context, serviceClass, args, delay, wakeUp);
        }
    }

    public static <S extends Service> void cancelDelay(@NotNull Context context, @NotNull Class<S> serviceClass) {
        cancelDelay(context, serviceClass, 0, 0);
    }

    public static <S extends Service> void cancelDelay(@NotNull Context context, @NotNull Class<S> serviceClass, int requestCode, int flags) {
        logger.d("cancelDelay(), serviceClass=" + serviceClass);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            throw new IllegalStateException("can't connect to " + AlarmManager.class.getSimpleName());
        }
        Intent intent = new Intent(context, serviceClass);
        PendingIntent pendingIntent = PendingIntent.getService(context, requestCode >= 0 ? requestCode : 0, intent, flags);
        alarmManager.cancel(pendingIntent);
    }

    public static <C extends ServiceConnection, S extends Service> boolean bindService(@NotNull Context ctx, @NotNull Class<S> serviceClass, @NotNull C serviceConnection, int flags) {
        logger.d("bindService(), serviceClass=" + serviceClass + ", serviceConnection=" + serviceConnection + ", flags=" + flags);
        boolean bounded = false;
        try {
            bounded = ctx.bindService(new Intent(ctx, serviceClass), serviceConnection, flags);
        } catch (Exception e) {
            logger.e("an Exception occurred during bindService()", e);
        }
        if (!bounded) {
            logger.e("binding to service " + serviceClass + " failed");
            restart(ctx, serviceClass);
            return false;
        }
        return true;
    }

    public static <C extends ServiceConnection> void unbindService(@NotNull Context context, @NotNull C serviceConnection) {
        logger.d("unbindService(), serviceConnection=" + serviceConnection);
        try {
            context.unbindService(serviceConnection);
        } catch (Exception e) {
            logger.e("an Exception occurred during unbindService()", e);
        }
    }

    private static <S extends Service> void restartNoCheck(@NotNull Context context, @NotNull Class<S> serviceClass) {
        restartNoCheck(context, serviceClass, null);
    }

    private static <S extends Service> void restartNoCheck(@NotNull Context context, @NotNull Class<S> serviceClass, @Nullable Intent args) {
        stopNoCheck(context, serviceClass);
        startNoCheck(context, serviceClass, args);
    }

    private static <S extends Service> void restartDelayNoCheck(@NotNull Context context, @NotNull Class<S> serviceClass, @Nullable Intent args, long delay, boolean wakeUp) {

        if (delay < 0) {
            throw new IllegalArgumentException("incorrect delay: " + delay);
        }

        stopNoCheck(context, serviceClass);
        startDelayNoCheck(context, serviceClass, args, delay, wakeUp);
    }

    @NotNull
    private static Intent createServiceIntent(String packageName, @NotNull Class<? extends Service> serviceClass, @Nullable Intent args)  {
        if (TextUtils.isEmpty(packageName)) {
            throw new IllegalArgumentException("Empty package name: " + packageName);
        }
        final ComponentName componentName = new ComponentName(packageName, serviceClass.getName());
        final Intent serviceIntent;
        if (args != null) {
            serviceIntent = new Intent(args);
        } else {
            serviceIntent = new Intent();
        }
        serviceIntent.setComponent(componentName);
        return serviceIntent;
    }


}

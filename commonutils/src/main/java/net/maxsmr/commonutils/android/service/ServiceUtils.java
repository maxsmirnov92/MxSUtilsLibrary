package net.maxsmr.commonutils.android.service;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import net.maxsmr.commonutils.android.hardware.DeviceUtils;
import net.maxsmr.commonutils.android.notification.NotificationActionInfo;
import net.maxsmr.commonutils.android.notification.NotificationController;
import net.maxsmr.commonutils.android.notification.NotificationInfo;
import net.maxsmr.commonutils.data.FileHelper;
import net.maxsmr.commonutils.data.Predicate;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;

import java.io.File;
import java.util.List;

public final class ServiceUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(ServiceUtils.class);

    private ServiceUtils() {
        throw new AssertionError("no instances.");
    }

    public static <S extends Service> boolean isServiceRunning(@NonNull Context context, @NonNull final Class<S> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            throw new RuntimeException(ActivityManager.class.getSimpleName() + " is null");
        }
        return Predicate.Methods.find(manager.getRunningServices(Integer.MAX_VALUE), new Predicate<ActivityManager.RunningServiceInfo>() {
            @Override
            public boolean apply(ActivityManager.RunningServiceInfo service) {
                return serviceClass.getName().equals(service.service.getClassName());
            }
        }) != null;
    }

    public static <S extends Service> boolean isServiceForeground(@NonNull Context context, @NonNull final Class<S> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) {
            throw new RuntimeException(ActivityManager.class.getSimpleName() + " is null");
        }
        return Predicate.Methods.find(manager.getRunningServices(Integer.MAX_VALUE), new Predicate<ActivityManager.RunningServiceInfo>() {
            @Override
            public boolean apply(ActivityManager.RunningServiceInfo service) {
                return serviceClass.getName().equals(service.service.getClassName()) && service.foreground;
            }
        }) != null;
    }

    private static <S extends Service> void startNoCheck(@NonNull Context context, @NonNull Class<S> serviceClass) {
        Intent service = new Intent(context, serviceClass);
        context.startService(service);
    }

    public static <S extends Service> void start(@NonNull Context context, @NonNull Class<S> serviceClass) {
        logger.d("start(), serviceClass= " + serviceClass);
        if (!isServiceRunning(context, serviceClass)) {
            restartNoCheck(context, serviceClass);
        }
    }

    private static <S extends Service> void restartNoCheck(@NonNull Context context, @NonNull Class<S> serviceClass) {
        logger.d("restart(), serviceClass=" + serviceClass);
        stopNoCheck(context, serviceClass);
        startNoCheck(context, serviceClass);
    }

    public static <S extends Service> void restart(@NonNull Context context, @NonNull Class<S> serviceClass) {
        logger.d("restart(), serviceClass=" + serviceClass);
        stop(context, serviceClass);
        startNoCheck(context, serviceClass);
    }

    private static <S extends Service> void stopNoCheck(@NonNull Context context, @NonNull Class<S> serviceClass) {
        Intent service = new Intent(context, serviceClass);
        context.stopService(service);
    }

    public static <S extends Service> void stop(@NonNull Context context, @NonNull Class<S> serviceClass) {
        logger.d("stop(), serviceClass=" + serviceClass);
        if (isServiceRunning(context, serviceClass)) {
            stopNoCheck(context, serviceClass);
        }
    }

    public static <S extends Service> void cancelDelay(@NonNull Context context, @NonNull Class<S> serviceClass) {
        cancelDelay(context, serviceClass, 0, 0);
    }

    public static <S extends Service> void cancelDelay(@NonNull Context context, @NonNull Class<S> serviceClass, int requestCode, int flags) {
        logger.d("cancelDelay(), serviceClass=" + serviceClass);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            throw new IllegalStateException("can't connect to " + AlarmManager.class.getSimpleName());
        }
        Intent intent = new Intent(context, serviceClass);
        PendingIntent pendingIntent = PendingIntent.getService(context, requestCode >= 0 ? requestCode : 0, intent, flags);
        alarmManager.cancel(pendingIntent);
    }

    public static <S extends Service> void restartDelay(@NonNull Context context, @NonNull Class<S> serviceClass, long delay, boolean wakeUp) {
        logger.d("restartDelay(), serviceClass=" + serviceClass + ", delay=" + delay);

        if (delay < 0) {
            throw new IllegalArgumentException("incorrect delay: " + delay);
        }
        stop(context, serviceClass);
        startDelayNoCheck(context, serviceClass, delay, wakeUp);
    }

    private static <S extends Service> void restartDelayNoCheck(@NonNull Context context, @NonNull Class<S> serviceClass, long delay, boolean wakeUp) {

        if (delay < 0) {
            throw new IllegalArgumentException("incorrect delay: " + delay);
        }

        stopNoCheck(context, serviceClass);
        startDelayNoCheck(context, serviceClass, delay, wakeUp);
    }

    public static <S extends Service> void startDelayNoCheck(@NonNull Context context, @NonNull Class<S> serviceClass, long delay, boolean wakeUp) {
        cancelDelay(context, serviceClass);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, new Intent(context, serviceClass), PendingIntent.FLAG_CANCEL_CURRENT);
        DeviceUtils.setAlarm(context, pendingIntent, System.currentTimeMillis() + delay, wakeUp ? DeviceUtils.AlarmType.RTC_WAKE_UP : DeviceUtils.AlarmType.RTC);
    }

    public static <S extends Service> void startDelay(@NonNull Context context, @NonNull Class<S> serviceClass, long delay, boolean wakeUp) {
        logger.d("startDelay(), serviceClass=" + serviceClass + ", delay=" + delay);
        if (!isServiceRunning(context, serviceClass)) {
            restartDelayNoCheck(context, serviceClass, delay, wakeUp);
        }
    }

    public static void startServiceForeground(@NonNull Service service, int id, @NonNull NotificationInfo notificationInfo) {
        logger.d("startServiceForeground(), service=" + service + ", id=" + id + ", notificationInfo=" + notificationInfo);
        startServiceForeground(service, id, notificationInfo.contentIntent, notificationInfo.tickerText, notificationInfo.contentTitle, notificationInfo.text, notificationInfo.iconResId, notificationInfo.actionInfos);
    }

    @Nullable
    public static Notification startServiceForeground(@NonNull Service service, int id, @Nullable Intent contentIntent, @Nullable String ticker, @Nullable String title, @Nullable String text, @DrawableRes int iconResId, NotificationActionInfo... actionInfos) {
        if (!ServiceUtils.isServiceForeground(service, service.getClass())) {
            NotificationController.getInstance().removeNotification(id);
            NotificationInfo info = new NotificationInfo();
            info.id = id;
            info.autoCancel = false;
            info.onlyUpdate = false;
            info.ongoing = true;
            info.contentIntent = contentIntent;
            info.actionInfos = actionInfos;
            info.tickerText = ticker;
            info.contentTitle = title;
            info.text = text;
            info.iconResId = iconResId;
            Notification noti = NotificationController.getInstance().createAndAddNotification(info);
            service.startForeground(id, noti);
            NotificationController.getInstance().updateNotification(id, noti);
            return noti;
        }
        return NotificationController.getInstance().getNotification(id);
    }

    public static <S extends Service> void stopServiceForeground(@NonNull S service, int id) {
        logger.d("stopServiceForeground(), service=" + service + ", id=" + id);
        if (isServiceForeground(service, service.getClass())) {
            service.stopForeground(true);
            NotificationController.getInstance().removeNotification(id);
        }
    }

    public static <C extends ServiceConnection, S extends Service> boolean bindService(@NonNull Context ctx, @NonNull Class<S> serviceClass, @NonNull C serviceConnection, int flags) {
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

    public static <C extends ServiceConnection> void unbindService(@NonNull Context context, @NonNull C serviceConnection) {
        logger.d("unbindService(), serviceConnection=" + serviceConnection);
        try {
            context.unbindService(serviceConnection);
        } catch (Exception e) {
            logger.e("an Exception occurred during unbindService()", e);
        }
    }

    @Nullable
    public static String getPackageNameFromApk(@NonNull Context context, File apkFile) {
        if (!FileHelper.isFileCorrect(apkFile) || !FileHelper.getFileExtension(apkFile.getName()).equalsIgnoreCase("apk")) {
            logger.e("incorrect apk: " + apkFile);
            return null;
        }
        PackageManager pm = context.getPackageManager();
        PackageInfo pi = pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
        if (pi != null) {
            return pi.packageName;
        }
        return null;
    }

    public static boolean canHandleActivityIntent(@NonNull Context context, @Nullable Intent intent) {
        List<ResolveInfo> resolveInfos = intent != null ? context.getPackageManager().queryIntentActivities(intent, 0) : null;
        return resolveInfos != null && !resolveInfos.isEmpty();
    }


}

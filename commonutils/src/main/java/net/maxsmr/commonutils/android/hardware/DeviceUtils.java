package net.maxsmr.commonutils.android.hardware;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.WindowManager;

import net.maxsmr.commonutils.shell.ShellUtils;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import static android.os.Build.VERSION.SDK_INT;


public final class DeviceUtils {

    private static final Logger logger = LoggerFactory.getLogger(DeviceUtils.class);

    public DeviceUtils() {
        throw new AssertionError("no instances.");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public static HashMap<String, UsbDevice> getDevicesList(@NonNull Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return usbManager.getDeviceList();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public static UsbAccessory[] getAccessoryList(@NonNull Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        return usbManager.getAccessoryList();
    }

    public static boolean isWakeLockHeld(@Nullable PowerManager.WakeLock wakeLock) {
        return wakeLock != null && wakeLock.isHeld();
    }

    public static boolean releaseWakeLock(@Nullable PowerManager.WakeLock wakeLock) {
        if (isWakeLockHeld(wakeLock)) {
            wakeLock.release();
            return true;
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static PowerManager.WakeLock wakeScreen(@NonNull Context context, @Nullable PowerManager.WakeLock wakeLock, @NonNull String name) {
        releaseWakeLock(wakeLock);
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, name);
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }
        return wakeLock;
    }

    public static void showKeyguard(@NonNull Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD); // doesn't require permission
    }

    /**
     * dismiss keyguard flag not working in all cases
     */
    public static void dismissKeyguard(@NonNull Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD); // doesn't require permission
        // KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        // final KeyguardManager.KeyguardLock kl = km.newKeyguardLock("MyKeyguardLock");
        // kl.disableKeyguard();
    }

    public static boolean isInteractive(@NonNull Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return pm.isInteractive();
        } else {
            return pm.isScreenOn();
        }
    }

    public static boolean isAppIsInBackground(Context context) {
        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (context.getPackageName().equals(activeProcess)) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (context.getPackageName().equals(componentInfo.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }


    public static void turnOnScreen(@NonNull Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public static void turnOffScreen(@NonNull Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public static int getScreenOffTimeout(@NonNull Context context) {
        int value = 0;
        try {
            value = android.provider.Settings.System.getInt(context.getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (android.provider.Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static boolean setScreenOffTimeout(@NonNull Context context, @NonNull ScreenTimeout timeout) {
        int value = getScreenOffTimeout(context);
        return !(value == 0 || value != timeout.value) ||
                android.provider.Settings.System.putInt(context.getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT, timeout.value);
    }

    public static LanguageCode getCurrentLanguageCode(@NonNull Context context) {
        final Locale current = context.getResources().getConfiguration().locale;

        if (current.getLanguage().equalsIgnoreCase(LanguageCode.RU.getCode())) {
            return LanguageCode.RU;
        } else if (current.getLanguage().equalsIgnoreCase(LanguageCode.EN.getCode())) {
            return LanguageCode.EN;
        } else {
            return LanguageCode.OTHER;
        }
    }

    /**
     * @param timeout in ms
     */
    public static long getCurrentNtpTime(int timeout, String hostName) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("incorrect timeout: " + timeout);
        }
        NTPUDPClient timeClient = new NTPUDPClient();
        timeClient.setDefaultTimeout(timeout);
        try {
            InetAddress inetAddress = InetAddress.getByName(hostName);
            TimeInfo timeInfo = timeClient.getTime(inetAddress);
            return timeInfo.getMessage().getTransmitTimeStamp().getTime();
        } catch (IOException e) {
            e.printStackTrace();
            timeClient.close();
        }
        return -1;
    }

    /** requires root */
    public static void setSystemTime(long timestamp) {
        logger.debug("setSystemTime(), timestamp=" + timestamp);

        if (timestamp < 0) {
            throw new IllegalArgumentException("incorrect timestamp: " + timestamp);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd.HHmmss", Locale.getDefault());

        ShellUtils.ShellCallback sc = new ShellUtils.ShellCallback() {
            @Override
            public boolean needToLogCommands() {
                return true;
            }

            @Override
            public void shellOut(@NonNull StreamType from, String shellLine) {
                logger.debug("shellOut(), from=" + from + ", shellLine=" + shellLine);
            }

            @Override
            public void processStartFailed(Throwable t) {
                logger.error("processStartFailed(), t=" + t);
            }

            @Override
            public void processComplete(int exitValue) {
                logger.debug("processComplete(), exitValue=" + exitValue);
            }
        };

//        ShellUtils.execProcess(Arrays.asList(ShellUtils.SU_PROCESS_NAME, "-c", "chmod", "666", "/dev/alarm"), null, sc, null, false);
//        SystemClock.setCurrentTimeMillis(timestamp);
//        ShellUtils.execProcess(Arrays.asList(ShellUtils.SU_PROCESS_NAME, "-c", "chmod", "664", "/dev/alarm"), null, sc, null, false);

        dateFormat.setTimeZone(TimeZone.getDefault());
        String formatTime = dateFormat.format(new Date(timestamp));
        ShellUtils.execProcess(Arrays.asList(ShellUtils.SU_BINARY_NAME, "-c", "date", "-s", formatTime), null, sc, null);
    }

    public static boolean setAlarm(@NonNull Context context, @NonNull PendingIntent pIntent, long triggerTime, @NonNull AlarmType alarmType) {
        logger.debug("setAlarm(), pIntent=" + pIntent + ", triggerTime=" + triggerTime + ", alarmType=" + alarmType);
        boolean result = false;
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        switch (alarmType) {
            case RTC:
                long currentTime = System.currentTimeMillis();
                logger.debug("currentTime=" + currentTime);
                if (triggerTime <= currentTime) {
                    logger.error("triggerTime (" + triggerTime + ") <= currentTime (" + currentTime + ")");
                    break;
                }
                if (SDK_INT < Build.VERSION_CODES.KITKAT) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pIntent);
                } else if (Build.VERSION_CODES.KITKAT <= SDK_INT && SDK_INT < Build.VERSION_CODES.M) {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pIntent);
                } else if (SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(triggerTime, null), pIntent);
                }
                result = true;
                break;

            case ELAPSED_REALTIME:
                long elapsedTime = SystemClock.elapsedRealtime();
                logger.debug("elapsedTime=" + elapsedTime);
                if (triggerTime <= elapsedTime) {
                    logger.error("triggerTime (" + triggerTime + ") <= elapsedTime (" + elapsedTime + ")");
                    break;
                }
                if (SDK_INT < Build.VERSION_CODES.KITKAT) {
                    alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pIntent);
                } else if (Build.VERSION_CODES.KITKAT <= SDK_INT && SDK_INT < Build.VERSION_CODES.M) {
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pIntent);
                } else if (SDK_INT >= Build.VERSION_CODES.M) {
                  alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, pIntent);
                }
                result = true;
                break;
        }
        return result;
    }

    public enum AlarmType {
        RTC, ELAPSED_REALTIME
    }

    public enum ScreenTimeout {

        NONE(Integer.MAX_VALUE),
        _15000(15000),
        _30000(30000),
        _60000(60000),
        _120000(12000),
        _600000(600000),
        _1800000(1800000);

        public final int value;

        ScreenTimeout(int value) {
            this.value = value;
        }
    }

    public enum LanguageCode {

        EN("EN"), RU("RU"), OTHER("OTHER");

        private final String code;

        public String getCode() {
            return code;
        }

        LanguageCode(String code) {
            this.code = code;
        }

        public LanguageCode fromValueNoThrow(String value) {
            for (LanguageCode e : LanguageCode.values()) {
                if (e.getCode().equalsIgnoreCase(value))
                    return e;
            }
            return null;
        }

        public LanguageCode fromValue(String value) {
            LanguageCode code = fromValueNoThrow(value);
            if (code == null) {
                throw new IllegalArgumentException("Incorrect value " + value + " for enum type " + LanguageCode.class.getName());
            }
            return code;
        }
    }
}

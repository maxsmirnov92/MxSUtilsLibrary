package net.maxsmr.commonutils.android.hardware;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.view.WindowManager;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.ShellCallback;
import net.maxsmr.commonutils.shell.ShellWrapper;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.commonutils.data.conversion.format.DateFormatUtilsKt.formatDate;
import static net.maxsmr.commonutils.shell.CommandResultKt.DEFAULT_TARGET_CODE;
import static net.maxsmr.commonutils.shell.ShellUtilsKt.execProcess;

public final class DeviceUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(DeviceUtils.class);

    public DeviceUtils() {
        throw new AssertionError("no instances.");
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public static HashMap<String, UsbDevice> getDevicesList(@NotNull Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new RuntimeException(UsbManager.class.getSimpleName() + " is null");
        }
        return usbManager.getDeviceList();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    public static UsbAccessory[] getAccessoryList(@NotNull Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new RuntimeException(UsbManager.class.getSimpleName() + " is null");
        }
        return usbManager.getAccessoryList();
    }

    public static int getBatteryPercentage(@NotNull Context context) {
        IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        return getBatteryPercentageFromIntent(context.registerReceiver(null, iFilter));
    }

    public static int getBatteryPercentageFromIntent(@Nullable Intent batteryStatus) {
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        float batteryPct = level / (float) scale;
        return (int) (batteryPct * 100);
    }

    public static boolean isWakeLockHeld(@Nullable PowerManager.WakeLock wakeLock) {
        return wakeLock != null && wakeLock.isHeld();
    }

    @Nullable
    public static PowerManager.WakeLock acquireWakeLockDefault(@NotNull Context context, @NotNull String name, long timeoutMillis) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) {
            throw new RuntimeException(PowerManager.class.getSimpleName() + " is null");
        }
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager
                .ACQUIRE_CAUSES_WAKEUP | PowerManager
                .ON_AFTER_RELEASE, name);
        return acquireWakeLock(context, wakeLock, timeoutMillis) ? wakeLock : null;
    }

    @SuppressLint("WakelockTimeout")
    public static boolean acquireWakeLock(@NotNull Context context, @Nullable PowerManager.WakeLock wakeLock, long timeoutMillis) {
        if (wakeLock != null && !isWakeLockHeld(wakeLock)) {
            // Even if we have the permission, some devices throw an exception in the try block nonetheless,
            // I'm looking at you, Samsung SM-T805

            try {
                if (timeoutMillis > 0) {
                    wakeLock.acquire(timeoutMillis);
                } else {
                    wakeLock.acquire();
                }
                return true;
            } catch (Exception e) {
                // saw an NPE on rooted Galaxy Nexus Android 4.1.1
                // android.os.IPowerManager$Stub$Proxy.acquireWakeLock(IPowerManager.java:288)
                logger.e("an Exception occurred during acquire(): " + e.getMessage(), e);
            }
        }
        return false;
    }

    public static void releaseWakeLock(@Nullable PowerManager.WakeLock wakeLock) {
        try {
            if (isWakeLockHeld(wakeLock)) {
                wakeLock.release();
            }
        } catch (Exception e) {
            // just to make sure if the PowerManager crashes while acquiring a wake lock
            logger.e("an Exception occurred during release(): " + e.getMessage(), e);
        }
    }

    public static void showKeyguard(@NotNull Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD); // doesn't require permission
    }

    /**
     * dismiss keyguard flag not working in all cases
     */
    public static void dismissKeyguard(@NotNull Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD); // doesn't require permission
        // KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        // final KeyguardManager.KeyguardLock kl = km.newKeyguardLock("MyKeyguardLock");
        // kl.disableKeyguard();
    }

    /**
     * Get the IMEI
     *
     * @param context Context to use
     * @return IMEI or null if not accessible
     */
    @SuppressLint({"HardwareIds", "MissingPermission"})
    @Nullable
    public static String getIMEI(@NotNull Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            throw new RuntimeException(TelephonyManager.class.getSimpleName() + " is null, cannot get IMEI");
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? telephonyManager.getImei() : telephonyManager.getDeviceId();
    }

    /**
     * Get the IMSI
     *
     * @param context Context to use
     * @return IMSI or null if not accessible
     */
    @SuppressLint({"HardwareIds", "MissingPermission"})
    @Nullable
    public static String getIMSI(@NotNull Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            throw new RuntimeException(TelephonyManager.class.getSimpleName() + " is null, cannot get IMSI");
        }
        return telephonyManager.getSubscriberId();
    }

    public static boolean isScreenOn(@NotNull Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null) {
            throw new RuntimeException(PowerManager.class.getSimpleName() + " is null");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            return pm.isInteractive();
        } else {
            //noinspection deprecation
            return pm.isScreenOn();
        }
    }

    /**
     * requires reboot permission that granted only to system apps
     */
    public static boolean reboot(Context context) {
        logger.d("reboot()");
        PowerManager powerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
        if (powerManager == null) {
            throw new RuntimeException(PowerManager.class.getSimpleName() + " is not null");
        }
        try {
            powerManager.reboot(null);
            return true;
        } catch (Exception e) {
            logger.e("an Exception occurred during reboot(): " + e.getMessage());
            return false;
        }
    }

    public static void toggleAirplaneMode(boolean enable, Context context) {
        logger.d("toggleAirplaneMode(), enable=" + enable);

        // boolean isEnabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;

        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, enable ? 1 : 0); // isEnabled

        // send an intent to reload
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enable); // !isEnabled
        context.sendBroadcast(intent);
    }

    public static boolean isSimCardInserted(@NotNull Context context) {
        final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager == null) {
            throw new RuntimeException(TelephonyManager.class.getSimpleName() + " is null");
        }
        return telephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT;
    }

    public static void turnOnScreen(@NotNull Activity activity) {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public static void turnOffScreen(@NotNull Activity activity) {
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
    }

    public static int getScreenOffTimeout(@NotNull Context context) {
        int value = 0;
        try {
            value = android.provider.Settings.System.getInt(context.getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT);
        } catch (android.provider.Settings.SettingNotFoundException e) {
            logger.e(e);
        }
        return value;
    }

    public static boolean setScreenOffTimeout(@NotNull Context context, int newTimeout) {
        if (newTimeout < 0) {
            throw new IllegalArgumentException("Incorrect timeout: " + newTimeout);
        }
        int currentTimeout = getScreenOffTimeout(context);
        return currentTimeout >= 0 && currentTimeout == newTimeout ||
                android.provider.Settings.System.putInt(context.getContentResolver(), android.provider.Settings.System.SCREEN_OFF_TIMEOUT, newTimeout);
    }

    public static LanguageCode getCurrentLanguageCode(@NotNull Context context) {
        final Locale current = context.getResources().getConfiguration().locale;

        if (current.getLanguage().equalsIgnoreCase(LanguageCode.RU.getCode())) {
            return LanguageCode.RU;
        } else if (current.getLanguage().equalsIgnoreCase(LanguageCode.EN.getCode())) {
            return LanguageCode.EN;
        } else {
            return LanguageCode.OTHER;
        }
    }

    public static boolean isPhoneCallActive(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (manager == null) {
            throw new RuntimeException(AudioManager.class.getSimpleName() + " is null");
        }
        return manager.getMode() == AudioManager.MODE_IN_CALL;
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
            logger.e(e);
            timeClient.close();
        }
        return -1;
    }

    public static void setSystemTime(long timestamp) {
        logger.d("setSystemTime(), timestamp=" + timestamp);

        if (timestamp < 0) {
            throw new IllegalArgumentException("incorrect timestamp: " + timestamp);
        }

        ShellCallback sc = new ShellCallback() {
            @Override
            public boolean getNeedToLogCommands() {
                return true;
            }

            @Override
            public void shellOut(@NotNull StreamType from, @NotNull String shellLine) {
                logger.d("shellOut(), from=" + from + ", shellLine=" + shellLine);
            }

            @Override
            public void processStarted() {
                logger.d("processStarted()");
            }

            @Override
            public void processStartFailed(Throwable t) {
                logger.e("processStartFailed(), t=" + t);
            }

            @Override
            public void processComplete(int exitValue) {
                logger.d("processComplete(), exitValue=" + exitValue);
            }
        };

//        ShellUtils.execProcess(Arrays.asList(ShellUtils.SU_PROCESS_NAME, "-c", "chmod", "666", "/dev/alarm"), null, sc, null, false);
//        SystemClock.setCurrentTimeMillis(timestamp);
//        ShellUtils.execProcess(Arrays.asList(ShellUtils.SU_PROCESS_NAME, "-c", "chmod", "664", "/dev/alarm"), null, sc, null, false);

        String formatTime = formatDate(new Date(timestamp), "yyyyMMdd.HHmmss", null);
        new ShellWrapper(false).executeCommand(Arrays.asList("date", "-s", formatTime), true, DEFAULT_TARGET_CODE, 0, TimeUnit.MILLISECONDS, sc);
    }

    /**
     * Get CPU ABI
     */
    private static String getAbi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return Build.SUPPORTED_ABIS[0];
        } else {
            //noinspection deprecation
            return Build.CPU_ABI;
        }
    }

    /**
     * Get CPU architecture
     */
    public static Arch getArch() {
        String abi = getAbi();

        switch (abi) {
            case "armeabi":
            case "armeabi-v7a":
            case "armeabi-v7a-hard":
                return Arch.ARM;

            case "arm64":
            case "arm64-v8a":
                return Arch.ARM64;

            case "x86":
                return Arch.X86;

            case "x86_64":
                return Arch.X86_64;

            default:
                return Arch.UNKNOWN;
        }
    }

    public enum Arch {
        ARM,
        ARM64,
        X86,
        X86_64,
        UNKNOWN
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

        public LanguageCode fromValue(String value) {
            for (LanguageCode e : LanguageCode.values()) {
                if (e.getCode().equalsIgnoreCase(value))
                    return e;
            }
            return null;
        }

        public LanguageCode fromValueOrThrow(String value) {
            LanguageCode code = fromValue(value);
            if (code == null) {
                throw new IllegalArgumentException("Incorrect value " + value + " for enum type " + LanguageCode.class.getName());
            }
            return code;
        }
    }
}

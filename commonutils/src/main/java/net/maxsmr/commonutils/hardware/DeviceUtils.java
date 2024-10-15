package net.maxsmr.commonutils.hardware;

import static net.maxsmr.commonutils.SdkVersionsKt.isAtLeastLollipop;
import static net.maxsmr.commonutils.format.DateFormatUtilsKt.formatDate;
import static net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.formatException;
import static net.maxsmr.commonutils.shell.CommandResultKt.DEFAULT_TARGET_CODE;
import static net.maxsmr.commonutils.text.SymbolConstsKt.EMPTY_STRING;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.RequiresPermission;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.ShellCallback;
import net.maxsmr.commonutils.shell.ShellWrapper;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public final class DeviceUtils {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(DeviceUtils.class);

    public DeviceUtils() {
        throw new AssertionError("no instances.");
    }

    public static HashMap<String, UsbDevice> getDevicesList(@NotNull Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new RuntimeException(UsbManager.class.getSimpleName() + " is null");
        }
        return usbManager.getDeviceList();
    }

    public static List<UsbAccessory> getAccessoryList(@NotNull Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new RuntimeException(UsbManager.class.getSimpleName() + " is null");
        }
        return Arrays.asList(usbManager.getAccessoryList());
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
            throw new RuntimeException("PowerManager is null");
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
                logger.e(formatException(e, "acquire"));
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
            logger.e(formatException(e, "release"));
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

    @SuppressLint({"HardwareIds", "MissingPermission"})
    @Nullable
    public static String getDeviceId(@NotNull Context context) {
        String deviceId = EMPTY_STRING;

        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            try {
                deviceId = telephonyManager.getDeviceId();
            } catch (RuntimeException e) {
                logger.e(e);
            }
        }
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        }
        // FirebaseInstanceId.getInstance().getId();

        return deviceId;
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
            throw new RuntimeException("PowerManager is null");
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
    @RequiresPermission(Manifest.permission.REBOOT)
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
            logger.e(formatException(e, "reboot"));
            return false;
        }
    }

    public static void toggleAirplaneMode(boolean enable, Context context) {
        logger.d("toggleAirplaneMode(), enable=" + enable);

        // boolean isEnabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;

        Settings.System.putInt(context.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, enable ? 1 : 0); // isEnabled

        // send an intent to reload
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enable);
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

        String formatTime = formatDate(new Date(timestamp), "yyyyMMdd.HHmmss", Locale.getDefault());
        new ShellWrapper(false).executeCommand(Arrays.asList("date", "-s", formatTime), true, DEFAULT_TARGET_CODE, 0, TimeUnit.MILLISECONDS, sc);
    }

    public DeviceType getScreenType(Activity activity) {
        DisplayMetrics outMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(outMetrics);
        final int shortSize = Math.min(outMetrics.widthPixels, outMetrics.heightPixels);
        final int shortSizeDp = shortSize * DisplayMetrics.DENSITY_DEFAULT / outMetrics.densityDpi;
        if (shortSizeDp < 600) { // 0-599dp: "phone" UI with a separate status & navigation bar
            return DeviceType.PHONE;
        } else if (shortSizeDp < 720) { // 600-719dp: "phone" UI with modifications for larger screens
            return DeviceType.HYBRID;
        } else { // 720dp: "tablet" UI with a single combined status & navigation bar
            return DeviceType.TABLET;
        }
    }

    /**
     * Get CPU ABI
     */
    private static String getAbi() {
        if (isAtLeastLollipop()) {
            return Build.SUPPORTED_ABIS[0];
        } else {
            return Build.CPU_ABI;
        }
    }

    /**
     * Get CPU architecture
     */
    public static Arch getArch() {
        String abi = getAbi();

        return switch (abi) {
            case "armeabi", "armeabi-v7a", "armeabi-v7a-hard" -> Arch.ARM;
            case "arm64", "arm64-v8a" -> Arch.ARM64;
            case "x86" -> Arch.X86;
            case "x86_64" -> Arch.X86_64;
            default -> Arch.UNKNOWN;
        };
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

        @Nullable
        public LanguageCode resolve(String value) {
            for (LanguageCode e : LanguageCode.values()) {
                if (e.getCode().equalsIgnoreCase(value))
                    return e;
            }
            return null;
        }

    }

    public enum DeviceType {
        PHONE, HYBRID, TABLET
    }
}

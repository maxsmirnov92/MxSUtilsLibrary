package net.maxsmr.networkutils.watcher;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.PowerManager;
import android.provider.Settings;

import net.maxsmr.commonutils.data.MathUtils;
import net.maxsmr.networkutils.NETWORK_TYPE;
import net.maxsmr.networkutils.NetworkHelper;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.LinkedList;

public class NetworkWatcher {

    private final static Logger logger = LoggerFactory.getLogger(NetworkWatcher.class);

    private final Context context;

    public interface OnPhoneRebootListener {
        void onPhoneReboot();

        void onPhoneRebootFailed();
    }

    private final LinkedList<OnPhoneRebootListener> rebootListeners = new LinkedList<OnPhoneRebootListener>();

    public void addOnPhoneRebootListener(OnPhoneRebootListener listener) throws NullPointerException {

        if (listener == null)
            throw new NullPointerException();

        synchronized (rebootListeners) {
            if (!rebootListeners.contains(listener)) {
                rebootListeners.add(listener);
            }
        }
    }

    public void removeOnPhoneRebootListener(OnPhoneRebootListener listener) {
        synchronized (rebootListeners) {
            if (rebootListeners.contains(listener)) {
                rebootListeners.remove(listener);
            }
        }
    }

    private static NetworkWatcher sInstance;

    public static void initInstance(Context context) {
        if (sInstance == null) {
            synchronized (NetworkWatcher.class) {
                logger.debug("initInstance()");
                sInstance = new NetworkWatcher(context);
            }
        }
    }

    public static NetworkWatcher getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("initInstance() was not called");
        }
        return sInstance;
    }

    private NetworkWatcher(Context context) {
        this.context = context;
    }

    public final static NETWORK_TYPE DEFAULT_PREFERABLE_NETWORK_TYPE = NETWORK_TYPE.WIFI;
    private NETWORK_TYPE preferableNetworkType = DEFAULT_PREFERABLE_NETWORK_TYPE;

    public NETWORK_TYPE getPreferableNetworkType() {
        return preferableNetworkType;
    }

    public final static long MIN_PREFERABLE_NETWORK_TYPE_SWITCH_TIME = 600000;
    public final static long DEFAULT_PREFERABLE_NETWORK_TYPE_SWITCH_TIME = MIN_PREFERABLE_NETWORK_TYPE_SWITCH_TIME;
    private long preferableNetworkTypeSwitchTime = DEFAULT_PREFERABLE_NETWORK_TYPE_SWITCH_TIME;

    public long getPreferableNetworkTypeSwitchTime() {
        return preferableNetworkTypeSwitchTime;
    }

    public void setPreferableNetworkTypeAndSwitchTime(NETWORK_TYPE preferableNetworkType, long preferableNetworkTypeSwitchTime) {
        logger.debug("setPreferableNetworkTypeAndSwitchTime(), preferableNetworkType=" + preferableNetworkType
                + ", preferableNetworkTypeSwitchTime=" + preferableNetworkTypeSwitchTime);

        if (preferableNetworkType != null && ConnectivityManager.isNetworkTypeValid(preferableNetworkType.getValue())
                || preferableNetworkType == NETWORK_TYPE.NONE) {
            this.preferableNetworkType = preferableNetworkType;
        } else {
            logger.error("incorrect preferableNetworkType: " + preferableNetworkType);
        }

        if (preferableNetworkTypeSwitchTime >= MIN_PREFERABLE_NETWORK_TYPE_SWITCH_TIME) {
            this.preferableNetworkTypeSwitchTime = preferableNetworkTypeSwitchTime;
        } else {
            logger.error("incorrect preferableNetworkTypeSwitchTime: " + preferableNetworkTypeSwitchTime);
        }
    }

    private boolean enableToggleAirplaneMode = false;

    public boolean isToggleAirplaneModeEnabled() {
        return enableToggleAirplaneMode;
    }

    public final static int DEFAULT_TOGGLE_AIRPLANE_MODE_ATTEMPTS_LIMIT = 3;
    private int toggleAirplaneModeAttemptsLimit = DEFAULT_TOGGLE_AIRPLANE_MODE_ATTEMPTS_LIMIT;

    public int getToggleAirplaneModeAttemptsLimit() {
        return toggleAirplaneModeAttemptsLimit;
    }

    public final static int DEFAULT_AIRPLANE_MODE_TIME = 15000;
    private int airplaneModeTime = DEFAULT_AIRPLANE_MODE_TIME;

    public int getAirplaneModeTime() {
        return airplaneModeTime;
    }

    public final static int DEFAULT_AIRPLANE_MODE_POST_WAIT = 60000;
    private int airplaneModePostWait = DEFAULT_AIRPLANE_MODE_POST_WAIT;

    public int getAirplaneModePostWait() {
        return airplaneModePostWait;
    }

    public void enableToggleAirplaneMode(boolean enable, int attemptsLimit, int airplaneModeTime, int airplaneModePostWait) {
        logger.debug("enableToggleAirplaneMode(), enable=" + enable + ", attemptsLimit=" + attemptsLimit + ", airplaneModeTime="
                + airplaneModeTime + ", airplaneModePostWait=" + airplaneModePostWait);

        enableToggleAirplaneMode = enable;
        if (!enableToggleAirplaneMode) {
            enableRebootingPhone = false;
        }

        if (attemptsLimit <= 0) {
            attemptsLimit = DEFAULT_TOGGLE_AIRPLANE_MODE_ATTEMPTS_LIMIT;
        }
        toggleAirplaneModeAttemptsLimit = attemptsLimit;

        if (airplaneModeTime < 0) {
            airplaneModeTime = DEFAULT_AIRPLANE_MODE_TIME;
        }
        this.airplaneModeTime = airplaneModeTime;

        if (airplaneModePostWait < 0) {
            airplaneModePostWait = DEFAULT_AIRPLANE_MODE_POST_WAIT;
        }
        this.airplaneModePostWait = airplaneModePostWait;
    }

    private boolean enableRebootingPhone = false;

    public boolean isRebootingPhoneEnabled() {
        return enableRebootingPhone;
    }

    public void enableRebootingPhone(boolean enable) {
        logger.debug("enableRebootingPhone(), enable=" + enable);

        if (enable) {
            if (!enableToggleAirplaneMode) {
                enableToggleAirplaneMode = true;
            }
        }

        enableRebootingPhone = enable;
    }

    private boolean enableSwitchNetworkInterface = false;

    public boolean isSwitchingNetworkInterfaceEnabled() {
        return enableSwitchNetworkInterface;
    }

    public void enableSwitchNetworkInterface(boolean enable) {
        enableSwitchNetworkInterface = enable;
    }

    @SuppressWarnings("deprecation")
    public static void toggleAirplaneMode(boolean enable, Context ctx) {
        logger.debug("toggleAirplaneMode(), enable=" + enable);

        if (ctx == null) {
            return;
        }

        // boolean isEnabled = Settings.System.getInt(getContentResolver(), Settings.System.AIRPLANE_MODE_ON, 0) == 1;

        Settings.System.putInt(ctx.getContentResolver(), Settings.System.AIRPLANE_MODE_ON, enable ? 1 : 0); // isEnabled

        // send an intent to reload
        Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intent.putExtra("state", enable); // !isEnabled
        ctx.sendBroadcast(intent);
    }

    public static boolean rebootPhoneByShell() {
        logger.debug("rebootPhoneByShell()");
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{"su", "-c", "reboot"});
            return proc.waitFor() == 0;
        } catch (Exception e) {
            logger.error("Could not reboot", e);
            return false;
        }
    }

    /**
     * requires reboot permission that granted only to system apps
     */
    public static boolean rebootPhoneByPowerManager(Context ctx) {
        logger.debug("rebootPhoneByPowerManager()");
        try {
            ((PowerManager) ctx.getSystemService(Context.POWER_SERVICE)).reboot(null);
            return true;
        } catch (Exception e) {
            logger.error("an Exception occurred during reboot(): " + e.getMessage());
            return false;
        }
    }

    public final static long DEFAULT_HOST_PING_PERIOD = 10000;

    private final ScheduledThreadPoolExecutorManager hostPingExecutor = new ScheduledThreadPoolExecutorManager("HostPingTask");
    private HostPingTask hostPingTask;

    public boolean isHostPingTaskRunning() {
        return hostPingExecutor.isRunning();
    }

    public synchronized void startHostPingTask(final long period, final String pingIpAddress, final int pingCount, final long timeout) {
        logger.debug("startHostPingTask(), period=" + period + ", pingIpAddress=" + pingIpAddress + ", pingCount=" + pingCount
                + ", timeout=" + timeout);

        new Thread(new Runnable() {

            @Override
            public void run() {

                InetAddress pingInetAddress = NetworkHelper.getInetAddressByIp(pingIpAddress);

                if (pingInetAddress == null) {
                    logger.error("incorrect ping ip address: " + pingIpAddress);
                    return;
                }

                stopRestoreNetworkThread();

                hostPingExecutor.addRunnableTask(hostPingTask = new HostPingTask(pingInetAddress, pingCount, timeout >= 0 ? timeout
                        : HostPingTask.DEFAULT_TIMEOUT));
                hostPingExecutor.restart(period > 0 ? period : DEFAULT_HOST_PING_PERIOD);
            }

        }).start();
    }

    public synchronized void stopHostPingTask() {
        logger.debug("stopHostPingTask()");

        if (!isHostPingTaskRunning()) {
            logger.debug("host ping task is not running");
            return;
        }

        hostPingExecutor.removeRunnableTask(hostPingTask);
        hostPingTask = null;
        hostPingExecutor.stop(false, 0);

        stopRestoreNetworkThread();
    }

    public interface HostPingListener {
        void onHostPingStateChanged(PING_STATE state);

        void onHostPingTimeChanged(double pingTime);
    }

    private final LinkedList<HostPingListener> hostPingListeners = new LinkedList<HostPingListener>();

    public void addHostPingListener(HostPingListener listener) throws NullPointerException {

        if (listener == null)
            throw new NullPointerException();

        synchronized (hostPingListeners) {
            if (!hostPingListeners.contains(listener)) {
                hostPingListeners.add(listener);
            }
        }
    }

    public void removeHostPingListener(HostPingListener listener) {
        synchronized (hostPingListeners) {
            if (hostPingListeners.contains(listener)) {
                hostPingListeners.remove(listener);
            }
        }
    }

    public PING_STATE getLastHostPingState() {

        if (!isHostPingTaskRunning()) {
            logger.error("can't get last ping state: host ping task is not running");
            return PING_STATE.NONE;
        }

        return hostPingTask.getLastHostPingState();
    }

    public double getLastHostPingTime() {

        if (!isHostPingTaskRunning()) {
            logger.error("can't get last ping time: host ping task is not running");
            return NetworkHelper.PING_TIME_NONE;
        }

        return hostPingTask.getLastHostPingTime();
    }

    public class HostPingTask implements Runnable {

        private int toggleAirplaneModeAttempts = 0;

        private PING_STATE lastHostPingState = PING_STATE.NONE;

        public PING_STATE getLastHostPingState() {
            return lastHostPingState;
        }

        private void setLastPingState(PING_STATE state) {

            lastHostPingState = state;
            logger.debug("last host ping state: " + state);

            synchronized (hostPingListeners) {
                if (hostPingListeners.size() > 0) {
                    for (HostPingListener l : hostPingListeners) {
                        l.onHostPingStateChanged(lastHostPingState);
                    }
                }
            }
        }

        private double lastHostPingTime = NetworkHelper.PING_TIME_NONE;

        public double getLastHostPingTime() {
            return lastHostPingTime;
        }

        private void setLastPingTime(double time) {

            lastHostPingTime = time;
            logger.debug("last host ping time: " + time);

            synchronized (hostPingListeners) {
                if (hostPingListeners.size() > 0) {
                    for (HostPingListener l : hostPingListeners) {
                        l.onHostPingTimeChanged(time);
                    }
                }
            }
        }

        public final static String DEFAULT_PING_IP_ADDRESS = "8.8.8.8";
        private final InetAddress inetAddress;

        public final static long DEFAULT_TIMEOUT = 20000;
        private final long timeout;

        public final static int DEFAULT_PING_COUNT = 10;
        private final int pingCount;

        private int lastActiveNetworkType = NetworkHelper.NETWORK_TYPE_NONE;
        private long lastActiveNetworkTypeStartTime = 0;

        private HostPingTask(InetAddress inetAddress, int pingCount, long timeout) {
            logger.debug("HostPingTask()");

            this.inetAddress = inetAddress;
            this.pingCount = pingCount < 1 ? DEFAULT_PING_COUNT : pingCount;
            this.timeout = timeout < 0 ? DEFAULT_TIMEOUT : timeout;
        }

        @Override
        public void run() {
            doHostPing();
        }

        private void doHostPing() {
            logger.debug("doHostPing()");

            if (inetAddress == null) {
                logger.error("inet address is null");
                return;
            }

            if (lastActiveNetworkType != NetworkHelper.getActiveNetworkType(context)) {
                logger.info("active network type has been changed: " + NetworkHelper.getActiveNetworkType(context) + " / previous: "
                        + lastActiveNetworkType);
                lastActiveNetworkType = NetworkHelper.getActiveNetworkType(context);
                lastActiveNetworkTypeStartTime = System.currentTimeMillis();
            }

            setLastPingState(PING_STATE.PINGING);
            setLastPingTime(NetworkHelper.isReachable(inetAddress, pingCount, MathUtils.safeLongToInt(timeout / 1000)));

            if (lastHostPingTime != NetworkHelper.PING_TIME_NONE) {

                logger.info("remote host is reachable");
                setLastPingState(PING_STATE.REACHABLE);

                toggleAirplaneModeAttempts = 0;

                stopRestoreNetworkThread();

                logger.info("active network type: " + NetworkHelper.getActiveNetworkType(context) + " / preferable network type: "
                        + preferableNetworkType);

                final long lastActiveNetworkTypeTime = System.currentTimeMillis() - lastActiveNetworkTypeStartTime;

                logger.info("last active network type time: " + lastActiveNetworkTypeTime / 1000 + " s");

                if (preferableNetworkType.getValue() != NetworkHelper.getActiveNetworkType(context)
                        && preferableNetworkType.getValue() != NetworkHelper.NETWORK_TYPE_NONE) {

                    logger.info("active network type not equals preferable, switching (enabled: " + enableSwitchNetworkInterface + ")...");

                    if (preferableNetworkType.getValue() == ConnectivityManager.TYPE_MOBILE && !NetworkHelper.isSimCardInserted(context)) {
                        logger.warn("preferable network type is mobile but sim card is NOT inserted, NO need to switch");
                        return;
                    }

                    if (lastActiveNetworkTypeTime >= preferableNetworkTypeSwitchTime || preferableNetworkTypeSwitchTime == 0) {

                        if (enableSwitchNetworkInterface) {

                            restoreNetworkThread = new RestoreNetworkThread(RESTORE_METHOD.NONE, 0, 0, preferableNetworkType,
                                    DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT);
                            restoreNetworkThread.setName(RestoreNetworkThread.class.getSimpleName());
                            restoreNetworkThread.start();

                            return;

                        } else {
                            logger.warn("switching network interface is NOT enabled");
                        }

                    } else {
                        logger.warn("last active network type time is not enough (need " + preferableNetworkTypeSwitchTime / 1000
                                + " s), NO switching to preferable network type");
                    }

                }

                return;
            }

            // for (int i = 0; i < pingCount; i++) {
            // if (NetworkHelper.isReachable(inetAddress, timeout)) {
            //
            // }
            // }

            logger.error("remote host " + inetAddress.getHostAddress() + " is not reachable");
            setLastPingState(PING_STATE.NOT_REACHABLE);

            if (isRestoreNetworkThreadRunning()) {
                logger.warn("restoring network is already running!");
                return;
            }

            if (NetworkHelper.isWifiNetworkType(NetworkHelper.getActiveNetworkType(context)) || NetworkHelper.isWifiEnabled(context)) {

                logger.info("network type is wifi or wifi is enabled");

                if (!NetworkHelper.isSimCardInserted(context)) {

                    logger.info("sim card is NOT inserted");

                    if (enableSwitchNetworkInterface) {

                        logger.info("switching wifi network ON/OFF...");

                        restoreNetworkThread = new RestoreNetworkThread(RESTORE_METHOD.NONE, 0, 0, NETWORK_TYPE.WIFI,
                                DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT);
                        restoreNetworkThread.setName(RestoreNetworkThread.class.getSimpleName());
                        restoreNetworkThread.start();

                        return;

                    } else {
                        logger.warn("switching network interface is NOT enabled");
                    }

                } else {

                    logger.info("sim card is inserted");

                    if (enableSwitchNetworkInterface) {

                        logger.info("turning OFF wifi and turning ON mobile data connection...");

                        restoreNetworkThread = new RestoreNetworkThread(RESTORE_METHOD.NONE, 0, 0, NETWORK_TYPE.MOBILE,
                                DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT);
                        restoreNetworkThread.setName(RestoreNetworkThread.class.getSimpleName());
                        restoreNetworkThread.start();

                        return;

                    } else {
                        logger.warn("switching network interface is NOT enabled");
                    }

                }
            }

            if (NetworkHelper.isMobileNetworkType(NetworkHelper.getActiveNetworkSubtype(context)) || !NetworkHelper.isOnline(context)) {

                logger.info("network type is mobile or none");

                if (!NetworkHelper.isSimCardInserted(context)) {
                    logger.info("sim card is NOT inserted");

                    if (enableSwitchNetworkInterface) {

                        logger.info("turning ON wifi...");

                        restoreNetworkThread = new RestoreNetworkThread(RESTORE_METHOD.NONE, 0, 0, NETWORK_TYPE.WIFI,
                                DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT);
                        restoreNetworkThread.setName(RestoreNetworkThread.class.getSimpleName());
                        restoreNetworkThread.start();

                        return;

                    } else {
                        logger.warn("switching network interface is NOT enabled");
                    }

                } else {
                    logger.info("sim card is inserted");

                    if (enableToggleAirplaneMode) {

                        logger.info("toggle airplane mode is enabled, toggleAirplaneModeAttempts=" + toggleAirplaneModeAttempts + "/"
                                + toggleAirplaneModeAttemptsLimit);

                        if (toggleAirplaneModeAttempts < toggleAirplaneModeAttemptsLimit) {

                            toggleAirplaneModeAttempts++;

                            logger.info("turning ON airplane mode...");

                            restoreNetworkThread = new RestoreNetworkThread(RESTORE_METHOD.TOGGLE_AIRPLANE_MODE, airplaneModeTime,
                                    airplaneModePostWait, NETWORK_TYPE.NONE, 0);
                            restoreNetworkThread.setName(RestoreNetworkThread.class.getSimpleName());
                            restoreNetworkThread.start();

                            return;

                        } else {

                            logger.warn("toggle airplane mode attempts limit has been reached!");

                            if (enableRebootingPhone) {

                                logger.info("rebooting phone is enabled");

                                toggleAirplaneModeAttempts = 0;

                                logger.info("rebooting...");

                                restoreNetworkThread = new RestoreNetworkThread(RESTORE_METHOD.REBOOT_PHONE, 0, 0, NETWORK_TYPE.WIFI, 0);
                                restoreNetworkThread.setName(RestoreNetworkThread.class.getSimpleName());
                                restoreNetworkThread.start();

                                return;

                            } else {

                                logger.warn("rebooting phone is NOT enabled");

                                if (enableSwitchNetworkInterface) {

                                    logger.info("turning OFF mobile data connection and turning ON wifi...");

                                    restoreNetworkThread = new RestoreNetworkThread(RESTORE_METHOD.NONE, 0, 0, NETWORK_TYPE.WIFI,
                                            DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT);
                                    restoreNetworkThread.setName(RestoreNetworkThread.class.getSimpleName());
                                    restoreNetworkThread.start();

                                    return;

                                } else {
                                    logger.warn("switching network interface is NOT enabled");
                                }
                            }
                        }

                    } else {
                        logger.warn("toggle airplane mode is NOT enabled");
                    }

                }

            }
        }
    }

    private static enum RESTORE_METHOD {
        NONE, TOGGLE_AIRPLANE_MODE, REBOOT_PHONE
    }

    public final static long DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT = 60000;

    private RestoreNetworkThread restoreNetworkThread;

    private boolean isRestoreNetworkThreadRunning() {
        return (restoreNetworkThread != null && restoreNetworkThread.isAlive());
    }

    private void stopRestoreNetworkThread() {
        if (isRestoreNetworkThreadRunning()) {
            logger.debug("restoring network thread is running, interrupting...");
            restoreNetworkThread.interrupt();
            restoreNetworkThread = null;
        }
    }

    private class RestoreNetworkThread extends Thread {

        private final RESTORE_METHOD restoreMethod;

        private final long airplaneModeTime;
        private final long airplaneModePostWait;

        private final NETWORK_TYPE networkTypeToSwitch;
        private final long networkTypeSwitchPostWait;

        public RestoreNetworkThread(RESTORE_METHOD restoreMethod, long airplaneModeTime, long airplaneModePostWait,
                                    NETWORK_TYPE networkTypeToSwitch, long networkTypeSwitchPostWait) {

            if (restoreMethod != null) {
                this.restoreMethod = restoreMethod;
            } else {
                this.restoreMethod = RESTORE_METHOD.NONE;
            }

            if (airplaneModeTime < 0) {
                this.airplaneModeTime = DEFAULT_AIRPLANE_MODE_TIME;
            } else {
                this.airplaneModeTime = airplaneModeTime;
            }

            if (airplaneModePostWait < 0) {
                this.airplaneModePostWait = DEFAULT_AIRPLANE_MODE_POST_WAIT;
            } else {
                this.airplaneModePostWait = airplaneModePostWait;
            }

            if (networkTypeToSwitch != null) {
                switch (networkTypeToSwitch) {
                    case NONE:
                    case MOBILE:
                    case WIFI:
                        this.networkTypeToSwitch = networkTypeToSwitch;
                        break;
                    default:
                        this.networkTypeToSwitch = NETWORK_TYPE.fromNativeValue(NetworkHelper.NETWORK_TYPE_NONE);
                }
            } else {
                this.networkTypeToSwitch = NETWORK_TYPE.fromNativeValue(NetworkHelper.NETWORK_TYPE_NONE);
            }

            if (networkTypeSwitchPostWait < 0) {
                this.networkTypeSwitchPostWait = DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT;
            } else {
                this.networkTypeSwitchPostWait = networkTypeSwitchPostWait;
            }

        }

        @Override
        public void run() {

            logger.debug("network type to switch: " + networkTypeToSwitch);

            switch (networkTypeToSwitch) {
                case WIFI:

                    logger.debug("disabling mobile data connection...");
                    NetworkHelper.enableMobileDataConnection(context, false);

                    logger.debug("enabling wifi connection...");
                    if (NetworkHelper.isWifiEnabled(context)) {
                        logger.debug("wifi already enabled, disabling first...");
                        NetworkHelper.enableWifiConnection(context, false);
                    }
                    NetworkHelper.enableWifiConnection(context, true);

                    break;

                case MOBILE:

                    logger.debug("disabling wifi connection...");
                    NetworkHelper.enableWifiConnection(context, false);

                    logger.debug("enabling mobile connection...");
                    NetworkHelper.enableMobileDataConnection(context, true);
                    break;

                default:
                    break;
            }

            if (networkTypeToSwitch != NETWORK_TYPE.NONE) {

                try {
                    Thread.sleep(networkTypeSwitchPostWait);
                } catch (InterruptedException e) {
                    logger.error("an InterruptedException exception occurred during sleep(): " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
            }

            logger.debug("restore method: " + restoreMethod);

            switch (restoreMethod) {

                case NONE:
                    break;

                case TOGGLE_AIRPLANE_MODE:

                    logger.warn("turning ON airplane mode...");
                    toggleAirplaneMode(true, context);

                    if (airplaneModeTime > 0) {
                        logger.debug("sleeping " + airplaneModeTime + " ms...");
                        try {
                            Thread.sleep(airplaneModeTime);
                        } catch (InterruptedException e) {
                            logger.error("an InterruptedException occurred during sleep(): " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    }

                    logger.info("turning OFF airplane mode...");
                    toggleAirplaneMode(false, context);

                    if (airplaneModePostWait > 0) {
                        logger.debug("sleeping " + airplaneModePostWait + " ms...");
                        try {
                            Thread.sleep(airplaneModePostWait);
                        } catch (InterruptedException e) {
                            logger.error("an InterruptedException occurred during sleep(): " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    }

                    break;

                case REBOOT_PHONE:

                    synchronized (rebootListeners) {
                        if (rebootListeners.size() > 0) {
                            for (OnPhoneRebootListener l : rebootListeners) {
                                l.onPhoneReboot();
                            }
                        }
                    }

                    logger.warn("trying to reboot phone by shell...");
                    if (!rebootPhoneByShell()) {
                        logger.error("reboot failed");

                        synchronized (rebootListeners) {
                            if (rebootListeners.size() > 0) {
                                for (OnPhoneRebootListener l : rebootListeners) {
                                    l.onPhoneRebootFailed();
                                }
                            }
                        }
                    }

                    break;
            }

        }
    }

}

package net.maxsmr.networkutils.watcher;

import android.content.Context;
import android.net.ConnectivityManager;

import net.maxsmr.commonutils.android.hardware.DeviceUtils;
import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.networkutils.NetworkHelper;
import net.maxsmr.networkutils.NetworkType;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;
import net.maxsmr.tasksutils.runnable.RunnableInfoRunnable;
import net.maxsmr.tasksutils.storage.ids.IdHolder;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnableExecutor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

import static net.maxsmr.commonutils.shell.RootShellCommandsKt.reboot;
import static net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY;

public class NetworkWatcher {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(NetworkWatcher.class);

    private final Context context;

    private static NetworkWatcher sInstance;

    public static void initInstance(Context context) {
        synchronized (NetworkWatcher.class) {
            if (sInstance == null) {
                logger.d("initInstance()");
                sInstance = new NetworkWatcher(context);
            }
        }
    }

    public static NetworkWatcher getInstance() {
        synchronized (NetworkWatcher.class) {
            if (sInstance == null) {
                throw new IllegalStateException("initInstance() was not called");
            }
            return sInstance;
        }
    }

    public final static long MIN_PREFERABLE_NETWORK_TYPE_SWITCH_TIME = 600000;

    public final static long DEFAULT_PREFERABLE_NETWORK_TYPE_SWITCH_TIME = MIN_PREFERABLE_NETWORK_TYPE_SWITCH_TIME;

    public final static NetworkType DEFAULT_PREFERABLE_NETWORK_TYPE = NetworkType.WIFI;

    public final static long DEFAULT_HOST_PING_PERIOD = 10000;

    public final static long DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT = 60000;

    public final static int DEFAULT_TOGGLE_AIRPLANE_MODE_ATTEMPTS_LIMIT = 3;

    public final static int DEFAULT_AIRPLANE_MODE_TIME = 15000;

    public final static int DEFAULT_AIRPLANE_MODE_POST_WAIT = 60000;

    private final IdHolder taskIdHolder = new IdHolder(1);

    private final HostPingObserbable hostPingListeners = new HostPingObserbable();

    private final RebootObservable rebootListeners = new RebootObservable();

    private final TaskRunnableExecutor<RestoreNetworkRunnableInfo, Void, Void, RestoreNetworkRunnable> restoreExecutor = new TaskRunnableExecutor<>(TaskRunnableExecutor.TASKS_NO_LIMIT, 1,
            TaskRunnableExecutor.DEFAULT_KEEP_ALIVE_TIME, TimeUnit.SECONDS, "RestoreNetworkTask", null,
            null, null);

    private final ScheduledThreadPoolExecutorManager hostPingExecutor = new ScheduledThreadPoolExecutorManager("HostPingTask");

    private HostPingRunnable hostPingRunnable;

    private NetworkType preferableNetworkType = DEFAULT_PREFERABLE_NETWORK_TYPE;

    private long preferableNetworkTypeSwitchTime = DEFAULT_PREFERABLE_NETWORK_TYPE_SWITCH_TIME;

    private int toggleAirplaneModeAttemptsLimit = DEFAULT_TOGGLE_AIRPLANE_MODE_ATTEMPTS_LIMIT;

    private boolean enableToggleAirplaneMode = false;

    private boolean enableSwitchNetworkInterface = false;

    private int airplaneModeTime = DEFAULT_AIRPLANE_MODE_TIME;

    private int airplaneModePostWait = DEFAULT_AIRPLANE_MODE_POST_WAIT;

    private double lastHostPingTime = -1;

    private NetworkWatcher(Context context) {
        this.context = context;
    }

    public void addHostPingListener(@NotNull HostPingListener listener) throws NullPointerException {
        hostPingListeners.registerObserver(listener);
    }

    public void removeHostPingListener(@NotNull HostPingListener listener) {
        hostPingListeners.unregisterObserver(listener);
    }

    public void addOnPhoneRebootListener(@NotNull PhoneRebootListener listener) {
        rebootListeners.registerObserver(listener);
    }

    public void removeOnPhoneRebootListener(@NotNull PhoneRebootListener listener) {
        rebootListeners.unregisterObserver(listener);
    }

    public NetworkType getPreferableNetworkType() {
        return preferableNetworkType;
    }

    public long getPreferableNetworkTypeSwitchTime() {
        return preferableNetworkTypeSwitchTime;
    }

    public void setPreferableNetworkTypeAndSwitchTime(NetworkType preferableNetworkType, long preferableNetworkTypeSwitchTime) {
        logger.d("setPreferableNetworkTypeAndSwitchTime(), preferableNetworkType=" + preferableNetworkType
                + ", preferableNetworkTypeSwitchTime=" + preferableNetworkTypeSwitchTime);

        if (preferableNetworkType != null && ConnectivityManager.isNetworkTypeValid(preferableNetworkType.getValue())
                || preferableNetworkType == NetworkType.NONE) {
            this.preferableNetworkType = preferableNetworkType;
        } else {
            logger.e("incorrect preferableNetworkType: " + preferableNetworkType);
        }

        if (preferableNetworkTypeSwitchTime >= MIN_PREFERABLE_NETWORK_TYPE_SWITCH_TIME) {
            this.preferableNetworkTypeSwitchTime = preferableNetworkTypeSwitchTime;
        } else {
            logger.e("incorrect preferableNetworkTypeSwitchTime: " + preferableNetworkTypeSwitchTime);
        }
    }

    public boolean isToggleAirplaneModeEnabled() {
        return enableToggleAirplaneMode;
    }

    public int getToggleAirplaneModeAttemptsLimit() {
        return toggleAirplaneModeAttemptsLimit;
    }


    public int getAirplaneModeTime() {
        return airplaneModeTime;
    }

    public int getAirplaneModePostWait() {
        return airplaneModePostWait;
    }

    public void enableToggleAirplaneMode(boolean enable, int attemptsLimit, int airplaneModeTime, int airplaneModePostWait) {
        logger.d("enableToggleAirplaneMode(), enable=" + enable + ", attemptsLimit=" + attemptsLimit + ", airplaneModeTime="
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
        logger.d("enableRebootingPhone(), enable=" + enable);

        if (enable) {
            if (!enableToggleAirplaneMode) {
                enableToggleAirplaneMode = true;
            }
        }

        enableRebootingPhone = enable;
    }

    public boolean isSwitchingNetworkInterfaceEnabled() {
        return enableSwitchNetworkInterface;
    }

    public void enableSwitchNetworkInterface(boolean enable) {
        enableSwitchNetworkInterface = enable;
    }

    public boolean isHostPingTaskRunning() {
        return hostPingExecutor.isRunning();
    }

    public synchronized void startHostPingTask(final long period, final String pingAddress, final int pingCount, final long timeout) {
        logger.d("startHostPingTask(), period=" + period + ", pingAddress=" + pingAddress + ", pingCount=" + pingCount
                + ", timeout=" + timeout);

        stopRestoreNetworkRunnable();

        hostPingExecutor.addRunnableTask(
                hostPingRunnable = new HostPingRunnable(taskIdHolder.getAndIncrement(),
                        pingAddress,
                        pingCount,
                        timeout >= 0 ? timeout
                                : HostPingRunnable.DEFAULT_TIMEOUT),
                new ScheduledThreadPoolExecutorManager.RunOptions(0, period > 0 ? period : DEFAULT_HOST_PING_PERIOD, FIXED_DELAY)
        );
        hostPingExecutor.restart(1);
    }

    public synchronized void stopHostPingTask() {
        logger.d("stopHostPingTask()");

        if (!isHostPingTaskRunning()) {
            logger.d("host ping task is not running");
            return;
        }

        hostPingExecutor.removeRunnableTask(hostPingRunnable);
        hostPingRunnable = null;
        hostPingExecutor.stop();

        stopRestoreNetworkRunnable();
    }


    public PingState getLastHostPingState() {

        if (!isHostPingTaskRunning()) {
            logger.e("can't get last ping state: host ping task is not running");
            return PingState.NONE;
        }

        return hostPingRunnable.getLastHostPingState();
    }

    public double getLastHostPingTime() {

        if (!isHostPingTaskRunning()) {
            logger.e("can't get last ping time: host ping task is not running");
            return -1;
        }

        return hostPingRunnable.getLastHostPingTime();
    }

    private boolean isRestoreNetworkRunnableRunning() {
        return restoreExecutor.containsTask(RestoreNetworkRunnableInfo.RESTORE_RUNNABLE_ID, TaskRunnableExecutor.RunnableType.ACTIVE);
    }

    private void stopRestoreNetworkRunnable() {
        if (isRestoreNetworkRunnableRunning()) {
            restoreExecutor.cancelTask(RestoreNetworkRunnableInfo.RESTORE_RUNNABLE_ID);
        }
    }

    public interface HostPingListener {

        void onHostPingStateChanged(@NotNull PingState state);

        void onHostPingTimeChanged(double pingTime);
    }

    public interface PhoneRebootListener {

        void onPhoneRebootStarting();

        void onPhoneRebootFailed();
    }

    private enum RestoreMethod {
        NONE, TOGGLE_AIRPLANE_MODE, REBOOT_PHONE
    }

    public class HostPingRunnable extends RunnableInfoRunnable<RunnableInfo> {

        public final static String DEFAULT_PING_IP_ADDRESS = "8.8.8.8";

        public final static long DEFAULT_TIMEOUT = 20000;

        public final static int DEFAULT_PING_COUNT = 10;

        private final String pingAddress;

        private final int pingCount;

        private final long timeout;

        private int lastActiveNetworkType = NetworkHelper.NETWORK_TYPE_NONE;
        private long lastActiveNetworkTypeStartTime = 0;

        private int toggleAirplaneModeAttempts = 0;

        @NotNull
        private PingState lastHostPingState = PingState.NONE;

        HostPingRunnable(int id, String pingAddress, int pingCount, long timeout) {
            super(new RunnableInfo(id));
            this.pingAddress = pingAddress;
            this.pingCount = pingCount < 1 ? DEFAULT_PING_COUNT : pingCount;
            this.timeout = timeout < 0 ? DEFAULT_TIMEOUT : timeout;
        }

        @NotNull
        public PingState getLastHostPingState() {
            return lastHostPingState;
        }

        private void setLastPingState(PingState state) {
            if (lastHostPingState != state) {
                lastHostPingState = state;
                logger.d("last host ping state: " + state);
                hostPingListeners.notifyHostPingStateChanged(state);
            }
        }

        public double getLastHostPingTime() {
            return lastHostPingTime;
        }

        private void setLastPingTime(double time) {
            if (Double.compare(time, lastHostPingTime) != 0) {
                lastHostPingTime = time;
                logger.d("last host ping time: " + time);
                hostPingListeners.notifyHostPingTimeChanged(lastHostPingTime);
            }
        }


        @Override
        public void run() {
            doHostPing();
        }

        private void doHostPing() {
            logger.d("doHostPing()");

            InetAddress pingInetAddress = NetworkHelper.getInetAddressByIp(pingAddress);

            if (pingInetAddress == null) {
                pingInetAddress = NetworkHelper.getInetAddressByDomain(pingAddress);
                if (pingInetAddress == null) {
                    logger.e("incorrect ping ip or domain address: " + pingAddress);
                    return;
                }
            }

            if (lastActiveNetworkType != NetworkHelper.getActiveNetworkType(context)) {
                logger.i("active network type has been changed: " + NetworkHelper.getActiveNetworkType(context) + " / previous: "
                        + lastActiveNetworkType);
                lastActiveNetworkType = NetworkHelper.getActiveNetworkType(context);
                lastActiveNetworkTypeStartTime = System.currentTimeMillis();
            }

            setLastPingState(PingState.PINGING);
            setLastPingTime(NetworkHelper.pingHost(pingInetAddress, pingCount, timeout, TimeUnit.MILLISECONDS));

            if (lastHostPingTime > 0) {

                logger.i("remote host is reachable");
                setLastPingState(PingState.REACHABLE);

                toggleAirplaneModeAttempts = 0;

                stopRestoreNetworkRunnable();

                logger.i("active network type: " + NetworkHelper.getActiveNetworkType(context) + " / preferable network type: "
                        + preferableNetworkType);

                final long currentTime = System.currentTimeMillis();
                final long lastActiveNetworkTypeTime = currentTime > lastActiveNetworkTypeStartTime ? currentTime - lastActiveNetworkTypeStartTime : 0;

                logger.i("last active network type time: " + TimeUnit.MILLISECONDS.toSeconds(lastActiveNetworkTypeTime) + " s");

                if (preferableNetworkType.getValue() != NetworkHelper.getActiveNetworkType(context)
                        && preferableNetworkType.getValue() != NetworkHelper.NETWORK_TYPE_NONE) {

                    logger.i("active network type not equals preferable, switching (enabled: " + enableSwitchNetworkInterface + ")...");

                    if (preferableNetworkType.getValue() == ConnectivityManager.TYPE_MOBILE && !DeviceUtils.isSimCardInserted(context)) {
                        logger.w("preferable network type is mobile but sim card is NOT inserted, NO need to switch");
                        return;
                    }

                    if (lastActiveNetworkTypeTime >= preferableNetworkTypeSwitchTime || preferableNetworkTypeSwitchTime == 0) {

                        if (enableSwitchNetworkInterface) {

                            restoreExecutor.execute(new RestoreNetworkRunnable(RestoreMethod.NONE, 0, 0, preferableNetworkType,
                                    DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT));

                            return;

                        } else {
                            logger.w("switching network interface is NOT enabled");
                        }

                    } else {
                        logger.w("last active network type time is not enough (need " + preferableNetworkTypeSwitchTime / 1000
                                + " s), NO switching to preferable network type");
                    }

                }

                return;
            }

            logger.e("remote host " + pingInetAddress.getHostAddress() + " is not reachable");
            setLastPingState(PingState.NOT_REACHABLE);

            if (isRestoreNetworkRunnableRunning()) {
                logger.w("restoring network is already running!");
                return;
            }

            if (NetworkHelper.isWifiNetworkType(NetworkHelper.getActiveNetworkType(context)) || NetworkHelper.isWifiEnabled(context)) {

                logger.i("network type is wifi or wifi is enabled");

                if (!DeviceUtils.isSimCardInserted(context)) {

                    logger.i("sim card is NOT inserted");

                    if (enableSwitchNetworkInterface) {

                        logger.i("switching wifi network ON/OFF...");

                        restoreExecutor.execute(new RestoreNetworkRunnable(RestoreMethod.NONE, 0, 0, NetworkType.WIFI,
                                DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT));

                        return;

                    } else {
                        logger.w("switching network interface is NOT enabled");
                    }

                } else {

                    logger.i("sim card is inserted");

                    if (enableSwitchNetworkInterface) {

                        logger.i("turning OFF wifi and turning ON mobile data connection...");

                        restoreExecutor.execute(new RestoreNetworkRunnable(RestoreMethod.NONE, 0, 0, NetworkType.MOBILE,
                                DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT));

                        return;

                    } else {
                        logger.w("switching network interface is NOT enabled");
                    }

                }
            }

            if (NetworkHelper.isMobileNetworkType(NetworkHelper.getActiveNetworkSubtype(context)) || !NetworkHelper.isOnline(context)) {

                logger.i("network type is mobile or none");

                if (!DeviceUtils.isSimCardInserted(context)) {
                    logger.i("sim card is NOT inserted");

                    if (enableSwitchNetworkInterface) {

                        logger.i("turning ON wifi...");

                        restoreExecutor.execute(new RestoreNetworkRunnable(RestoreMethod.NONE, 0, 0, NetworkType.WIFI,
                                DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT));

                    } else {
                        logger.w("switching network interface is NOT enabled");
                    }

                } else {
                    logger.i("sim card is inserted");

                    if (enableToggleAirplaneMode) {

                        logger.i("toggle airplane mode is enabled, toggleAirplaneModeAttempts=" + toggleAirplaneModeAttempts + "/"
                                + toggleAirplaneModeAttemptsLimit);

                        if (toggleAirplaneModeAttempts < toggleAirplaneModeAttemptsLimit) {

                            logger.i("turning ON airplane mode...");

                            restoreExecutor.execute(new RestoreNetworkRunnable(RestoreMethod.TOGGLE_AIRPLANE_MODE, airplaneModeTime,
                                    airplaneModePostWait, NetworkType.NONE, 0));

                            toggleAirplaneModeAttempts++;

                        } else {

                            logger.w("toggle airplane mode attempts limit has been reached!");

                            if (enableRebootingPhone) {

                                logger.i("rebooting phone is enabled");

                                toggleAirplaneModeAttempts = 0;

                                logger.i("rebooting...");

                                restoreExecutor.execute(new RestoreNetworkRunnable(RestoreMethod.REBOOT_PHONE, 0, 0, NetworkType.WIFI, 0));


                            } else {

                                logger.w("rebooting phone is NOT enabled");

                                if (enableSwitchNetworkInterface) {

                                    logger.i("turning OFF mobile data connection and turning ON wifi...");

                                    restoreExecutor.execute(new RestoreNetworkRunnable(RestoreMethod.NONE, 0, 0, NetworkType.WIFI,
                                            DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT));

                                } else {
                                    logger.w("switching network interface is NOT enabled");
                                }
                            }
                        }

                    } else {
                        logger.w("toggle airplane mode is NOT enabled");
                    }

                }

            }
        }
    }


    private class RestoreNetworkRunnable extends TaskRunnable<RestoreNetworkRunnableInfo, Void, Void> {

        public RestoreNetworkRunnable(RestoreMethod restoreMethod, long airplaneModeTime, long airplaneModePostWait,
                                      NetworkType networkTypeToSwitch, long networkTypeSwitchPostWait) {
            super(new RestoreNetworkRunnableInfo(restoreMethod, airplaneModeTime, airplaneModePostWait, networkTypeToSwitch, networkTypeSwitchPostWait));
        }

        @Nullable
        @Override
        public Void doWork() throws Throwable {
            logger.d("network type to switch: " + rInfo.networkTypeToSwitch);

            switch (rInfo.networkTypeToSwitch) {
                case WIFI:

                    logger.d("disabling mobile data connection...");
                    NetworkHelper.enableMobileDataConnection(context, false);

                    logger.d("enabling wifi connection...");
                    if (NetworkHelper.isWifiEnabled(context)) {
                        logger.d("wifi already enabled, disabling first...");
                        NetworkHelper.enableWifiConnection(context, false);
                    }
                    NetworkHelper.enableWifiConnection(context, true);

                    break;

                case MOBILE:

                    logger.d("disabling wifi connection...");
                    NetworkHelper.enableWifiConnection(context, false);

                    logger.d("enabling mobile connection...");
                    NetworkHelper.enableMobileDataConnection(context, true);
                    break;

                default:
                    break;
            }

            if (rInfo.networkTypeToSwitch != NetworkType.NONE) {

                if (rInfo.networkTypeSwitchPostWait > 0) {
                    try {
                        Thread.sleep(rInfo.networkTypeSwitchPostWait);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.e("an InterruptedException exception occurred during sleep(): " + e.getMessage());
                    }
                }
            }

            logger.d("restore method: " + rInfo.restoreMethod);

            switch (rInfo.restoreMethod) {

                case NONE:
                    break;

                case TOGGLE_AIRPLANE_MODE:

                    logger.w("turning ON airplane mode...");
                    DeviceUtils.toggleAirplaneMode(true, context);

                    if (rInfo.airplaneModeTime > 0) {
                        logger.d("sleeping " + rInfo.airplaneModeTime + " ms...");
                        try {
                            Thread.sleep(rInfo.airplaneModeTime);
                        } catch (InterruptedException e) {
                            logger.e("an InterruptedException occurred during sleep(): " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    }

                    logger.i("turning OFF airplane mode...");
                    DeviceUtils.toggleAirplaneMode(false, context);

                    if (rInfo.airplaneModePostWait > 0) {
                        logger.d("sleeping " + rInfo.airplaneModePostWait + " ms...");
                        try {
                            Thread.sleep(rInfo.airplaneModePostWait);
                        } catch (InterruptedException e) {
                            logger.e("an InterruptedException occurred during sleep(): " + e.getMessage());
                            Thread.currentThread().interrupt();
                        }
                    }

                    break;

                case REBOOT_PHONE:

                    rebootListeners.notifyRebootStarting();

                    logger.w("trying to reboot phone by shell...");
                    if (!reboot()) {
                        logger.e("reboot failed");

                        rebootListeners.notifyRebootFailed();
                    }

                    break;
            }

            return null;
        }
    }

    private static class RestoreNetworkRunnableInfo extends RunnableInfo {

        final static int RESTORE_RUNNABLE_ID = 1;

        private final RestoreMethod restoreMethod;

        private final long airplaneModeTime;
        private final long airplaneModePostWait;

        private final NetworkType networkTypeToSwitch;
        private final long networkTypeSwitchPostWait;

        public RestoreNetworkRunnableInfo(RestoreMethod restoreMethod, long airplaneModeTime, long airplaneModePostWait,
                                          NetworkType networkTypeToSwitch, long networkTypeSwitchPostWait) {
            super(RESTORE_RUNNABLE_ID);

            if (restoreMethod != null) {
                this.restoreMethod = restoreMethod;
            } else {
                this.restoreMethod = RestoreMethod.NONE;
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
                        this.networkTypeToSwitch = NetworkType.fromValue(NetworkHelper.NETWORK_TYPE_NONE);
                }
            } else {
                this.networkTypeToSwitch = NetworkType.fromValue(NetworkHelper.NETWORK_TYPE_NONE);
            }

            if (networkTypeSwitchPostWait < 0) {
                this.networkTypeSwitchPostWait = DEFAULT_NETWORK_TYPE_SWITCH_POST_WAIT;
            } else {
                this.networkTypeSwitchPostWait = networkTypeSwitchPostWait;
            }
        }
    }

    private static class HostPingObserbable extends Observable<HostPingListener> {

        private void notifyHostPingTimeChanged(double pingTime) {
            synchronized (observers) {
                for (HostPingListener l : observers) {
                    l.onHostPingTimeChanged(pingTime);
                }
            }
        }

        private void notifyHostPingStateChanged(@NotNull PingState state) {
            synchronized (observers) {
                for (HostPingListener l : observers) {
                    l.onHostPingStateChanged(state);
                }
            }
        }

    }

    private static class RebootObservable extends Observable<PhoneRebootListener> {

        private void notifyRebootStarting() {
            synchronized (observers) {
                for (PhoneRebootListener l : observers) {
                    l.onPhoneRebootStarting();
                }
            }
        }

        private void notifyRebootFailed() {
            synchronized (observers) {
                for (PhoneRebootListener l : observers) {
                    l.onPhoneRebootFailed();
                }
            }
        }
    }

}

package net.maxsmr.devicewatchers.usb;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.data.StringUtils;
import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.maxsmr.commonutils.data.SymbolConstKt.EMPTY_STRING;
import static net.maxsmr.commonutils.shell.ShellUtilsKt.execProcess;
import static net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY;

public class UsbDeviceWatcher {

    private static final BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(UsbDeviceWatcher.class);

    public static final int DEFAULT_WATCH_INTERVAL = 2000;

    private static UsbDeviceWatcher sInstance;

    private final DeviceWatchObservable watchListeners = new DeviceWatchObservable();

    private final DevicesFlagsObservable flagsListeners = new DevicesFlagsObservable();

    private final ScheduledThreadPoolExecutorManager deviceListWatcher = new ScheduledThreadPoolExecutorManager("UsbDeviceWatcher");

    private final ScheduledThreadPoolExecutorManager deviceFinder = new ScheduledThreadPoolExecutorManager("UsbDeviceFinder");

    private DeviceWatcherRunnable deviceListRunnable;

    private DeviceFinderRunnable deviceFinderRunnable;

    public static void initInstance() {
        synchronized (UsbDeviceWatcher.class) {
            if (sInstance == null) {
                logger.d("initInstance()");
                sInstance = new UsbDeviceWatcher();
            }
        }
    }

    public static UsbDeviceWatcher getInstance() {
        synchronized (UsbDeviceWatcher.class) {
            initInstance();
            return sInstance;
        }
    }


    private UsbDeviceWatcher() {
        // do nothing
    }

    public void addWatchListener(@NotNull DeviceWatchListener l) {
        watchListeners.registerObserver(l);
    }

    public void removeWatchListener(@NotNull DeviceWatchListener l) {
        watchListeners.unregisterObserver(l);
    }

    public boolean isDeviceListWatcherRunning() {
        return deviceListWatcher.isRunning() && deviceListRunnable != null;
    }

    public void stopDeviceListWatcher() {
        logger.d("stopDeviceListWatcher()");
        if (isDeviceListWatcherRunning()) {
            deviceListWatcher.removeAllRunnableTasks();
            deviceListRunnable = null;
        }
    }

    public void restartDeviceListWatcher(long interval, DeviceInfo... devicesToWatch) {
        List<DeviceInfo> deviceInfos = devicesToWatch != null ? Arrays.asList(devicesToWatch) : null;
        logger.d("restartDeviceWatcher, interval=" + interval + ", devicesToWatch=" + deviceInfos);
        if (interval <= 0) {
            throw new IllegalArgumentException("incorrect interval: " + interval);
        }
        deviceListWatcher.removeAllRunnableTasks();
        deviceListWatcher.addRunnableTask(deviceListRunnable = new DeviceWatcherRunnable(deviceInfos), new ScheduledThreadPoolExecutorManager.RunOptions(0, interval, FIXED_DELAY));
        deviceListWatcher.restart(1);
    }

    public void startDeviceWatcher(long interval, DeviceInfo... devicesToWatch) {
        if (!isDeviceListWatcherRunning()) {
            restartDeviceListWatcher(interval, devicesToWatch);
        }
    }

    public void addDevicesFlagsListener(@NotNull DevicesFlagsListener l) {
        flagsListeners.registerObserver(l);
    }

    public void removeDevicesFlagsListener(@NotNull DevicesFlagsListener l) {
        flagsListeners.unregisterObserver(l);
    }

    public boolean isDeviceFinderRunning() {
        return deviceFinder.isRunning() && deviceFinderRunnable != null;
    }

    public void stopDeviceFinder() {
        logger.d("stopDeviceFinder()");
        if (isDeviceFinderRunning()) {
            deviceFinder.removeAllRunnableTasks();
            deviceFinderRunnable = null;
        }
    }

    public void restartDeviceFinderWatcher(int evFlagsMask, boolean match, long interval) {
        logger.d("restartDeviceFinder, evFlagsMask=" + evFlagsMask + ", match=" + match + ", interval=" + interval);
        if (interval <= 0) {
            throw new IllegalArgumentException("incorrect interval: " + interval);
        }
        deviceFinder.removeAllRunnableTasks();
        deviceFinder.addRunnableTask(deviceFinderRunnable = new DeviceFinderRunnable(evFlagsMask, match), new ScheduledThreadPoolExecutorManager.RunOptions(0, interval, FIXED_DELAY));
        deviceFinder.restart(1);
    }

    public void startDeviceFinder(int evFlagsMask, boolean match, long interval) {
        if (!isDeviceFinderRunning()) {
            restartDeviceFinderWatcher(evFlagsMask, match, interval);
        }
    }

    public boolean isDeviceAttached(@Nullable DeviceInfo info) {
        if (!isDeviceListWatcherRunning()) {
            throw new IllegalStateException(DeviceWatcherRunnable.class.getName() + " is not running");
        }
        return deviceListRunnable.contains(info);
    }

    public static class DeviceInfo {

        public final int bus, device, vendorID, productID;

        public DeviceInfo(int bus, int device, int vendorID, int productID) {
            this.bus = bus;
            this.device = device;
            this.vendorID = vendorID;
            this.productID = productID;
        }

        @Override
        public boolean equals(Object o) {

            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DeviceInfo that = (DeviceInfo) o;

            if (vendorID != that.vendorID) return false;
            if (productID != that.productID) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = bus;
            result = 31 * result + device;
            result = 31 * result + vendorID;
            result = 31 * result + productID;
            return result;
        }

        @Override
        public String toString() {
            return "DeviceInfo{" +
                    "bus=" + bus +
                    ", device=" + device +
                    ", vendorID=" + vendorID +
                    ", productID=" + productID +
                    '}';
        }
    }

    private class DeviceWatcherRunnable extends TaskRunnable<RunnableInfo, Void, CommandResult> {

        final List<DeviceInfo> currentDeviceInfos = new ArrayList<>();
        final List<DeviceInfo> devicesToWatch = new ArrayList<>();

        DeviceWatcherRunnable(@Nullable List<DeviceInfo> devicesToWatch) {
            super(new RunnableInfo(0, DeviceWatcherRunnable.class.getName()));
            logger.d("devicesToWatch=" + devicesToWatch);
            if (devicesToWatch != null) {
                this.devicesToWatch.addAll(devicesToWatch);
            }
        }

        boolean contains(@Nullable DeviceInfo info) {
            return currentDeviceInfos.contains(info);
        }

        @Nullable
        @Override
        public CommandResult doWork() throws Throwable {
            return execProcess(Arrays.asList("su", "-c", "lsusb"), EMPTY_STRING, null, null, null, null);
        }

        @Override
        public void onPostExecute(@Nullable CommandResult result) {
            super.onPostExecute(result);

            if (result != null && result.isSuccessful()) {
                List<DeviceInfo> infos = parseOutput(result.getStdOutLines());
//                        logger.d("parsed: " + infos + ", current: " + currentDeviceInfos);

                List<DeviceInfo> attached = new ArrayList<>();
                List<DeviceInfo> detached = new ArrayList<>();

                List<DeviceInfo> specifiedAttached = new ArrayList<>();
                List<DeviceInfo> specifiedDetached = new ArrayList<>();

                for (DeviceInfo i : infos) {
                    if (!currentDeviceInfos.contains(i)) {
                        attached.add(i);
                        if (devicesToWatch.contains(i)) {
                            specifiedAttached.add(i);
                        }
                    }
                }

                for (DeviceInfo i : currentDeviceInfos) {
                    if (!infos.contains(i)) {
                        detached.add(i);
                        if (devicesToWatch.contains(i)) {
                            specifiedDetached.add(i);
                        }
                    }
                }

                watchListeners.notifyDevicesChanged(attached, detached, specifiedAttached, specifiedDetached, currentDeviceInfos, infos);

                currentDeviceInfos.clear();
                currentDeviceInfos.addAll(infos);

            } else {

                watchListeners.notifyReadFailed(result != null ? result : new CommandResult());
            }
        }

        @NotNull
        private List<DeviceInfo> parseOutput(@NotNull List<String> output) {

            final List<DeviceInfo> infos = new ArrayList<>();

            if (!output.isEmpty()) {

                final int busLength = 3;
                final int deviceLength = 3;
                final int idLength = 9;

                for (String str : output) {
                    if (!StringUtils.isEmpty(str)) {

                        int busStartIndex = str.contains("Bus") ? +str.indexOf("Bus") + "Bus".length() + 1 : -1;
                        int busEndIndex = busStartIndex + busLength;

                        int bus = 0;
                        try {
                            bus = busStartIndex >= 0 && busStartIndex < busEndIndex && busStartIndex < str.length() && busEndIndex < str.length() ? Integer.parseInt(str.substring(busStartIndex, busEndIndex)) : 0;
                        } catch (NumberFormatException e) {
                            logger.e("a NumberFormatException occurred during parseInt()", e);
                        }

                        int deviceStartIndex = str.contains("Device") ? str.indexOf("Device") + "Device".length() + 1 : -1;
                        int deviceEndIndex = deviceStartIndex + deviceLength;

                        int device = 0;
                        try {
                            device = deviceStartIndex >= 0 && deviceStartIndex < deviceEndIndex && deviceStartIndex < str.length() && deviceEndIndex < str.length() ? Integer.parseInt(str.substring(deviceStartIndex, deviceEndIndex)) : 0;
                        } catch (NumberFormatException e) {
                            logger.e("a NumberFormatException occurred during parseInt()", e);
                        }

                        int idStartIndex = str.contains("ID") ? str.lastIndexOf("ID") + "ID".length() + 1 : -1;
                        int idEndIndex = idStartIndex + idLength;

                        String id = idStartIndex >= 0 && idStartIndex < idEndIndex && idStartIndex < str.length() && idEndIndex <= str.length() ? str.substring(idStartIndex, idEndIndex) : null;

                        int vendorID = 0;
                        int productID = 0;

                        if (!StringUtils.isEmpty(id)) {
                            String[] parts = id.split(":");
                            if (parts.length == 2) {
                                try {
                                    vendorID = Integer.parseInt(parts[0], 16);
                                    productID = Integer.parseInt(parts[1], 16);
                                } catch (NumberFormatException e) {
                                    logger.e("a NumberFormatException occurred during parseInt()", e);
                                }
                            }
                        }

                        infos.add(new DeviceInfo(bus, device, vendorID, productID));
                    }
                }

            }

            return infos;
        }

    }

    public enum EventTypeFlags {

        EV_SYN(0x00), EV_KEY(0x01), EV_MSC(0x04), EV_LED(0x11), EV_REP(0x14);

        public final int flags;

        EventTypeFlags(int flags) {
            this.flags = flags;
        }
    }

    public boolean containsEvFlags(int evFlags, boolean match) {

        if (!isDeviceFinderRunning()) {
            throw new IllegalStateException(DeviceFinderRunnable.class.getName() + " is not running");
        }

        return deviceFinderRunnable.containsEvFlags(evFlags, match);
    }

    private class DeviceFinderRunnable extends TaskRunnable<RunnableInfo, Void, CommandResult> {

        final List<Integer> lastEvFlags = new ArrayList<>();

        final int evFlagsMask;
        final boolean match;

        DeviceFinderRunnable(int evFlagsMask, boolean match) {
            super(new RunnableInfo(0, DeviceFinderRunnable.class.getName()));
            this.evFlagsMask = evFlagsMask;
            this.match = match;
        }

        boolean containsEvFlags(int evFlags, boolean match) {
            return !scanFlags(lastEvFlags, evFlags, match).isEmpty();
        }

        @Nullable
        @Override
        public CommandResult doWork() throws Throwable {
            return execProcess(Arrays.asList("su", "-c", "cat", "/proc/bus/input/devices"), EMPTY_STRING, null, null, null, null);
        }

        @Override
        public void onPostExecute(@Nullable CommandResult result) {
            super.onPostExecute(result);
            if (result != null && result.isSuccessful()) {

                lastEvFlags.clear();
                lastEvFlags.addAll(parseEventFlags(result.getStdOutLines()));
//               logger.d("parsed: " + lastEvFlags);

                flagsListeners.notifyFlagsChanged(scanFlags(lastEvFlags, evFlagsMask, match), evFlagsMask);

            } else {
                flagsListeners.notifyReadFailed(result != null ? result : new CommandResult());
            }
        }

        @NotNull
        private List<Integer> parseEventFlags(@NotNull List<String> output) {
            List<Integer> evFlagsList = new ArrayList<>();
            if (!output.isEmpty()) {
                for (String str : output) {
                    if (!StringUtils.isEmpty(str)) {
                        int evStartIndex = str.contains("EV=") ? str.indexOf("EV=") + "EV=".length() : -1;
                        int evEndIndex = str.length();
                        if (evStartIndex >= 0 && evStartIndex < evEndIndex) {
                            try {
                                int evFlags = Integer.parseInt(str.substring(evStartIndex, evEndIndex), 16);
                                evFlagsList.add(evFlags);
                            } catch (NumberFormatException e) {
                                logger.e("a NumberFormatException occurred", e);
                            }
                        }
                    }
                }
            }
            return evFlagsList;
        }

        @NotNull
        private List<Integer> scanFlags(@NotNull List<Integer> evFlagsList, int evFlags, boolean match) {
            List<Integer> found = new ArrayList<>();
            for (Integer f : evFlagsList) {
                if (f != null && (match ? ((f & evFlags) == evFlags) : evFlags == f)) {
                    found.add(f);
                }
            }
            return found;
        }
    }

    public interface DeviceWatchListener {

        void onDevicesStatusChanged(List<DeviceInfo> deviceInfos, boolean attach);

        void onSpecifiedDevicesStatusChanged(List<DeviceInfo> deviceInfos, boolean attach);

        void onDevicesListChanged(List<DeviceInfo> currentList, List<DeviceInfo> previousList);

        void onDevicesListReadFailed(int exitCode, @NotNull List<String> errorStream);
    }

    public interface DevicesFlagsListener {

        void onDevicesFlagsFound(List<Integer> devicesFlags, int mask);

        void onDevicesFlagsNotFound(int mask);

        void onDevicesFlagsReadFailed(int exitCode, @NotNull List<String> errorStream);
    }

    private static class DeviceWatchObservable extends Observable<DeviceWatchListener> {

        void notifyDevicesChanged(List<DeviceInfo> attached, List<DeviceInfo> detached,
                                  List<DeviceInfo> specifiedAttached, List<DeviceInfo> specifiedDetached,
                                  List<DeviceInfo> currentDeviceInfos, List<DeviceInfo> previousDeviceInfos) {
            synchronized (observers) {
                for (DeviceWatchListener l : observers) {
                    if (!attached.isEmpty())
                        l.onDevicesStatusChanged(new ArrayList<>(attached), true);
                    if (!detached.isEmpty())
                        l.onDevicesStatusChanged(new ArrayList<>(detached), false);
                    if (!specifiedAttached.isEmpty())
                        l.onSpecifiedDevicesStatusChanged(new ArrayList<>(specifiedAttached), true);
                    if (!specifiedDetached.isEmpty())
                        l.onSpecifiedDevicesStatusChanged(new ArrayList<>(specifiedDetached), false);
                    if (!currentDeviceInfos.equals(previousDeviceInfos))
                        l.onDevicesListChanged(new ArrayList<>(currentDeviceInfos), new ArrayList<>(previousDeviceInfos));
                }
            }
        }

        void notifyReadFailed(@NotNull CommandResult result) {
            synchronized (observers) {
                for (DeviceWatchListener l : observers) {
                    l.onDevicesListReadFailed(result.getExitCode(), result.getStdErrLines());
                }
            }
        }
    }

    private static class DevicesFlagsObservable extends Observable<DevicesFlagsListener> {

        void notifyFlagsChanged(@NotNull List<Integer> found, int evFlagsMask) {
            synchronized (observers) {
                for (DevicesFlagsListener l : observers) {
                    if (!found.isEmpty()) {
                        l.onDevicesFlagsFound(found, evFlagsMask);
                    } else {
                        l.onDevicesFlagsNotFound(evFlagsMask);
                    }
                }
            }
        }

        void notifyReadFailed(@NotNull CommandResult result) {
            synchronized (observers) {
                for (DevicesFlagsListener l : observers) {
                    l.onDevicesFlagsReadFailed(result.getExitCode(), result.getStdErrLines());
                }
            }
        }
    }
}

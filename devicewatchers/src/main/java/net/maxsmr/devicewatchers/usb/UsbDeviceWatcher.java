package net.maxsmr.devicewatchers.usb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.data.Observable;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.commonutils.shell.ShellUtils;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;
import net.maxsmr.tasksutils.taskexecutor.RunnableInfo;
import net.maxsmr.tasksutils.taskexecutor.TaskRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class UsbDeviceWatcher {

    private static final Logger logger = LoggerFactory.getLogger(UsbDeviceWatcher.class);

    public static final int DEFAULT_WATCH_INTERVAL = 2000;

    private static UsbDeviceWatcher sInstance;

    private final DeviceWatchObservable watchListeners = new DeviceWatchObservable();

    private final DevicesFlagsObservable flagsListeners = new DevicesFlagsObservable();

    private final ScheduledThreadPoolExecutorManager deviceListWatcher = new ScheduledThreadPoolExecutorManager(ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY, UsbDeviceWatcher.class.getSimpleName());

    private final ScheduledThreadPoolExecutorManager deviceFinder = new ScheduledThreadPoolExecutorManager(ScheduledThreadPoolExecutorManager.ScheduleMode.FIXED_DELAY, "UsbDeviceFinder");

    private DeviceWatcherRunnable deviceListRunnable;

    private DeviceFinderRunnable deviceFinderRunnable;

    public static void initInstance() {
        if (sInstance == null) {
            synchronized (UsbDeviceWatcher.class) {
                logger.debug("initInstance()");
                sInstance = new UsbDeviceWatcher();
            }
        }
    }

    public static UsbDeviceWatcher getInstance() {
        initInstance();
        return sInstance;
    }


    private UsbDeviceWatcher() {
    }

    public void addWatchListener(@NonNull DeviceWatchListener l) {
        watchListeners.registerObserver(l);
    }

    public void removeWatchListener(@NonNull DeviceWatchListener l) {
        watchListeners.unregisterObserver(l);
    }


    public boolean isDeviceListWatcherRunning() {
        return deviceListWatcher.isRunning() && deviceListRunnable != null;
    }

    public void stopDeviceListWatcher() {
        logger.debug("stopDeviceListWatcher()");
        if (isDeviceListWatcherRunning()) {
            deviceListWatcher.removeAllRunnableTasks();
            deviceListRunnable = null;
        }
    }

    public void restartDeviceListWatcher(int interval, DeviceInfo... devicesToWatch) {
        List<DeviceInfo> deviceInfos = devicesToWatch != null ? Arrays.asList(devicesToWatch) : null;
        logger.debug("restartDeviceWatcher(), interval=" + interval + ", devicesToWatch=" + deviceInfos);
        if (interval <= 0) {
            throw new IllegalArgumentException("incorrect interval: " + interval);
        }
        stopDeviceListWatcher();
        deviceListWatcher.addRunnableTask(deviceListRunnable = new DeviceWatcherRunnable(deviceInfos));
        deviceListWatcher.restart(interval);
    }

    public void startDeviceWatcher(int interval, DeviceInfo... devicesToWatch) {
        if (!isDeviceListWatcherRunning()) {
            restartDeviceListWatcher(interval, devicesToWatch);
        }
    }


    public void addDevicesFlagsListener(@NonNull DevicesFlagsListener l) {
        flagsListeners.registerObserver(l);
    }

    public void removeDevicesFlagsListener(@NonNull DevicesFlagsListener l) {
        flagsListeners.unregisterObserver(l);
    }

    public boolean isDeviceFinderRunning() {
        return deviceFinder.isRunning() && deviceFinderRunnable != null;
    }

    public void stopDeviceFinder() {
        logger.debug("stopDeviceFinder()");
        if (isDeviceFinderRunning()) {
            deviceFinder.removeAllRunnableTasks();
            deviceFinderRunnable = null;
        }
    }

    public void restartDeviceFinderWatcher(int evFlagsMask, boolean match) {
        stopDeviceFinder();
        deviceFinder.addRunnableTask(deviceFinderRunnable = new DeviceFinderRunnable(evFlagsMask, match));
        deviceFinder.restart(DEFAULT_WATCH_INTERVAL);
    }

    public void startDeviceFinder(int evFlagsMask, boolean match) {
        logger.debug("startDeviceFinder(), evFlagsMask=" + evFlagsMask + ", match=" + match);
        if (!isDeviceFinderRunning()) {
            restartDeviceFinderWatcher(evFlagsMask, match);
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

    private class DeviceWatcherRunnable extends TaskRunnable<RunnableInfo> {

        final List<DeviceInfo> currentDeviceInfos = new ArrayList<>();
        final List<DeviceInfo> devicesToWatch = new ArrayList<>();

        DeviceWatcherRunnable(@Nullable List<DeviceInfo> devicesToWatch) {
            super(new RunnableInfo(0, DeviceWatcherRunnable.class.getName()));
            logger.debug("devicesToWatch=" + devicesToWatch);
            if (devicesToWatch != null) {
                this.devicesToWatch.addAll(devicesToWatch);
            }
        }

        boolean contains(@Nullable DeviceInfo info) {
            return currentDeviceInfos.contains(info);
        }

        @Override
        public void run() {
            doDeviceWatch();
        }

        private void doDeviceWatch() {
            logger.debug("doDeviceWatch()");

            CommandResult result = ShellUtils.execProcess(Arrays.asList("su", "-c", "lsusb"), null, null, null);

            if (result.isSuccessful()) {
                List<DeviceInfo> infos = parseOutput(result.getStdOutLines());
//                        logger.debug("parsed: " + infos + ", current: " + currentDeviceInfos);

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

                watchListeners.notifyReadFailed(result);
            }
        }

        @NonNull
        private List<DeviceInfo> parseOutput(@NonNull List<String> output) {

            final List<DeviceInfo> infos = new ArrayList<>();

            if (!output.isEmpty()) {

                final int busLength = 3;
                final int deviceLength = 3;
                final int idLength = 9;

                for (String str : output) {
                    if (!TextUtils.isEmpty(str)) {

                        int busStartIndex = str.contains("Bus") ? +str.indexOf("Bus") + "Bus".length() + 1 : -1;
                        int busEndIndex = busStartIndex + busLength;

                        int bus = 0;
                        try {
                            bus = busStartIndex >= 0 && busStartIndex < busEndIndex && busStartIndex < str.length() && busEndIndex < str.length() ? Integer.parseInt(str.substring(busStartIndex, busEndIndex)) : 0;
                        } catch (NumberFormatException e) {
                            logger.error("a NumberFormatException occurred during parseInt()", e);
                            e.printStackTrace();
                        }

                        int deviceStartIndex = str.contains("Device") ? str.indexOf("Device") + "Device".length() + 1 : -1;
                        int deviceEndIndex = deviceStartIndex + deviceLength;

                        int device = 0;
                        try {
                            device = deviceStartIndex >= 0 && deviceStartIndex < deviceEndIndex && deviceStartIndex < str.length() && deviceEndIndex < str.length() ? Integer.parseInt(str.substring(deviceStartIndex, deviceEndIndex)) : 0;
                        } catch (NumberFormatException e) {
                            logger.error("a NumberFormatException occurred during parseInt()", e);
                            e.printStackTrace();
                        }

                        int idStartIndex = str.contains("ID") ? str.lastIndexOf("ID") + "ID".length() + 1 : -1;
                        int idEndIndex = idStartIndex + idLength;

                        String id = idStartIndex >= 0 && idStartIndex < idEndIndex && idStartIndex < str.length() && idEndIndex <= str.length() ? str.substring(idStartIndex, idEndIndex) : null;

                        int vendorID = 0;
                        int productID = 0;

                        if (!TextUtils.isEmpty(id)) {
                            String[] parts = id.split(":");
                            if (parts.length == 2) {
                                try {
                                    vendorID = Integer.parseInt(parts[0], 16);
                                    productID = Integer.parseInt(parts[1], 16);
                                } catch (NumberFormatException e) {
                                    logger.error("a NumberFormatException occurred during parseInt()", e);
                                    e.printStackTrace();
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

    private class DeviceFinderRunnable extends TaskRunnable<RunnableInfo> {

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

        @Override
        public void run() {

            CommandResult result = ShellUtils.execProcess(Arrays.asList("su", "-c", "cat", "/proc/bus/input/devices"), null, null, null);

            if (result.isSuccessful()) {

                lastEvFlags.clear();
                lastEvFlags.addAll(parseEventFlags(result.getStdOutLines()));
//                        logger.debug("parsed: " + lastEvFlags);

                flagsListeners.notifyFlagsChanged(scanFlags(lastEvFlags, evFlagsMask, match), evFlagsMask);


            } else {
                flagsListeners.notifyReadFailed(result);
            }
        }

        @NonNull
        private List<Integer> parseEventFlags(@NonNull List<String> output) {
            List<Integer> evFlagsList = new ArrayList<>();
            if (!output.isEmpty()) {
                for (String str : output) {
                    if (!TextUtils.isEmpty(str)) {
                        int evStartIndex = str.contains("EV=") ? str.indexOf("EV=") + "EV=".length() : -1;
                        int evEndIndex = str.length();
                        if (evStartIndex >= 0 && evStartIndex < evEndIndex) {
                            try {
                                int evFlags = Integer.parseInt(str.substring(evStartIndex, evEndIndex), 16);
                                evFlagsList.add(evFlags);
                            } catch (NumberFormatException e) {
                                logger.error("a NumberFormatException occurred", e);
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            return evFlagsList;
        }

        @NonNull
        private List<Integer> scanFlags(@NonNull List<Integer> evFlagsList, int evFlags, boolean match) {
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

        void onDevicesListReadFailed(int exitCode, @NonNull List<String> errorStream);
    }

    public interface DevicesFlagsListener {

        void onDevicesFlagsFound(List<Integer> devicesFlags, int mask);

        void onDevicesFlagsNotFound(int mask);

        void onDevicesFlagsReadFailed(int exitCode, @NonNull List<String> errorStream);
    }

    private static class DeviceWatchObservable extends Observable<DeviceWatchListener> {

        void notifyDevicesChanged(List<DeviceInfo> attached, List<DeviceInfo> detached,
                                  List<DeviceInfo> specifiedAttached, List<DeviceInfo> specifiedDetached,
                                  List<DeviceInfo> currentDeviceInfos, List<DeviceInfo> previousDeviceInfos) {
            synchronized (mObservers) {
                for (DeviceWatchListener l : getObservers()) {
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

        void notifyReadFailed(@NonNull CommandResult result) {
            synchronized (mObservers) {
                for (DeviceWatchListener l : mObservers) {
                    l.onDevicesListReadFailed(result.getExitCode(), result.getStdErrLines());
                }
            }
        }
    }

    private static class DevicesFlagsObservable extends Observable<DevicesFlagsListener> {

        void notifyFlagsChanged(@NonNull List<Integer> found, int evFlagsMask) {
            synchronized (mObservers) {
                for (DevicesFlagsListener l : mObservers) {
                    if (!found.isEmpty()) {
                        l.onDevicesFlagsFound(found, evFlagsMask);
                    } else {
                        l.onDevicesFlagsNotFound(evFlagsMask);
                    }
                }
            }
        }

        void notifyReadFailed(@NonNull CommandResult result) {
            synchronized (mObservers) {
                for (DevicesFlagsListener l : mObservers) {
                    l.onDevicesFlagsReadFailed(result.getExitCode(), result.getStdErrLines());
                }
            }
        }


    }
}

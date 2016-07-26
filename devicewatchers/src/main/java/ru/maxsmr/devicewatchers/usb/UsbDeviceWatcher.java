package ru.maxsmr.devicewatchers.usb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import net.maxsmr.commonutils.shell.ShellUtils;
import net.maxsmr.tasksutils.ScheduledThreadPoolExecutorManager;
import net.maxsmr.tasksutils.taskrunnable.RunnableInfo;
import net.maxsmr.tasksutils.taskrunnable.TaskRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class UsbDeviceWatcher {

    private static final Logger logger = LoggerFactory.getLogger(UsbDeviceWatcher.class);

    private static UsbDeviceWatcher sInstance;

    public static void initInstance() {
        if (sInstance == null) {
            synchronized (UsbDeviceWatcher.class) {
                logger.debug("initInstance()");
                sInstance = new UsbDeviceWatcher();
            }
        }
    }

    public static UsbDeviceWatcher getInstance() {
        if (sInstance == null) {
            throw new IllegalStateException("initInstance() was not called");
        }
        return sInstance;
    }


    private UsbDeviceWatcher() {
    }

    private static final int WATCH_INTERVAL = 2000;

    public interface WatchListener {
        void onDevicesStatusChanged(List<DeviceInfo> deviceInfos, boolean attach);

        void onSpecifiedDevicesStatusChanged(List<DeviceInfo> deviceInfos, boolean attach);

        void onDevicesListChanged(List<DeviceInfo> currentList, List<DeviceInfo> previousList);

        void onDevicesListReadFailed(int exitCode, @NonNull List<String> errorStream);
    }

    private final LinkedList<WatchListener> watchListeners = new LinkedList<>();

    public void addWatchListener(@NonNull WatchListener l) {
        if (!watchListeners.contains(l)) {
            watchListeners.add(l);
        }
    }

    public void removeWatchListener(@NonNull WatchListener l) {
        watchListeners.remove(l);
    }

    private final ScheduledThreadPoolExecutorManager deviceListWatcher = new ScheduledThreadPoolExecutorManager("DeviceWatcher");
    private DeviceWatcherRunnable deviceListRunnable;

    public boolean isDeviceListWatcherRunning() {
        return deviceListWatcher.isRunning() && deviceListRunnable != null;
    }

    public void stopDeviceListWatcher() {
        logger.debug("stopDeviceListWatcher()");
        if (isDeviceListWatcherRunning()) {
            deviceListWatcher.removeAllRunnableTasks();
            deviceListWatcher.stop(false, 0);
            deviceListRunnable = null;
        }
    }

    public void restartDeviceListWatcher(DeviceInfo... devicesToWatch) {
        stopDeviceListWatcher();
        deviceListWatcher.addRunnableTask(deviceListRunnable = new DeviceWatcherRunnable(devicesToWatch != null ? Arrays.asList(devicesToWatch) : null));
        deviceListWatcher.start(0, WATCH_INTERVAL);
    }

    public void startDeviceWatcher(DeviceInfo... devicesToWatch) {
        logger.debug("startDeviceWatcher(), devicesToWatch=" + Arrays.asList(devicesToWatch));
        if (!isDeviceListWatcherRunning()) {
            restartDeviceListWatcher(devicesToWatch);
        }
    }

    public interface DevicesFlagsListener {
        void onDevicesFlagsFound(List<Integer> devicesFlags, int mask);

        void onDevicesFlagsNotFound(int mask);

        void onDevicesFlagsReadFailed(int exitCode, @NonNull List<String> errorStream);
    }

    private final LinkedList<DevicesFlagsListener> flagsListeners = new LinkedList<>();

    public void addDevicesFlagsListener(@NonNull DevicesFlagsListener l) {
        if (!flagsListeners.contains(l)) {
            flagsListeners.add(l);
        }
    }

    public void removeDevicesFlagsListener(@NonNull DevicesFlagsListener l) {
        flagsListeners.remove(l);
    }

    private final ScheduledThreadPoolExecutorManager deviceFinder = new ScheduledThreadPoolExecutorManager("DeviceFinder");
    private DeviceFinderRunnable deviceFinderRunnable;

    public boolean isDeviceFinderRunning() {
        return deviceFinder.isRunning() && deviceFinderRunnable != null;
    }

    public void stopDeviceFinder() {
        logger.debug("stopDeviceFinder()");
        if (isDeviceFinderRunning()) {
            deviceFinder.removeAllRunnableTasks();
            deviceFinder.stop(false, 0);
            deviceFinderRunnable = null;
        }
    }

    public void restartDeviceFinderWatcher(int evFlagsMask, boolean match) {
        stopDeviceFinder();
        deviceFinder.addRunnableTask(deviceFinderRunnable = new DeviceFinderRunnable(evFlagsMask, match));
        deviceFinder.start(0, WATCH_INTERVAL);
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
        protected boolean checkArgs() {
            return true;
        }

        @Override
        public void run() {
            super.run();
            doDeviceWatch();
        }

        private void doDeviceWatch() {
            logger.debug("doDeviceWatch()");

            ShellUtils.execProcess(Arrays.asList("su", "-c", "lsusb"), "/", new ShellUtils.ShellCallback() {

                final List<String> output = new ArrayList<>();
                final List<String> error = new ArrayList<>();

                @Override
                public boolean needToLogCommands() {
                    return true;
                }

                @Override
                public void shellOut(@NonNull StreamType from, String shellLine) {
                    if (from == StreamType.OUT) {
                        output.add(shellLine);
                    } else if (from == StreamType.ERR) {
                        error.add(shellLine);
                    }
                }

                @Override
                public void processComplete(int exitValue) {
                    logger.debug("processComplete(), exitValue=" + exitValue);

                    if (exitValue == ShellUtils.PROCESS_EXIT_CODE_SUCCESS) {
                        List<DeviceInfo> infos = parseOutput(output);
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

                        synchronized (watchListeners) {
                            for (WatchListener l : watchListeners) {
                                if (!attached.isEmpty())
                                    l.onDevicesStatusChanged(attached, true);
                                if (!detached.isEmpty())
                                    l.onDevicesStatusChanged(detached, false);
                                if (!specifiedAttached.isEmpty())
                                    l.onSpecifiedDevicesStatusChanged(specifiedAttached, true);
                                if (!specifiedDetached.isEmpty())
                                    l.onSpecifiedDevicesStatusChanged(specifiedDetached, false);
                                if (!currentDeviceInfos.equals(infos))
                                    l.onDevicesListChanged(new ArrayList<>(infos), new ArrayList<>(currentDeviceInfos));
                            }
                        }

                        currentDeviceInfos.clear();
                        currentDeviceInfos.addAll(infos);

                    } else {
                        synchronized (watchListeners) {
                            for (WatchListener l : watchListeners) {
                                l.onDevicesListReadFailed(exitValue, error);
                            }
                        }
                    }
                }

            }, null, true);

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
        protected boolean checkArgs() {
            return true;
        }

        @Override
        public void run() {
            super.run();

            ShellUtils.execProcess(Arrays.asList("su", "-c", "cat", "/proc/bus/input/devices"), "/", new ShellUtils.ShellCallback() {

                final List<String> output = new ArrayList<>();
                final List<String> error = new ArrayList<>();

                @Override
                public boolean needToLogCommands() {
                    return true;
                }

                @Override
                public void shellOut(@NonNull StreamType from, String shellLine) {
                    if (from == StreamType.OUT) {
                        output.add(shellLine);
                    } else if (from == StreamType.ERR) {
                        error.add(shellLine);
                    }
                }

                @Override
                public void processComplete(int exitValue) {
                    logger.debug("processComplete(), exitValue=" + exitValue);
                    if (exitValue == ShellUtils.PROCESS_EXIT_CODE_SUCCESS) {

                        lastEvFlags.clear();
                        lastEvFlags.addAll(parseEventFlags(output));
//                        logger.debug("parsed: " + lastEvFlags);

                        List<Integer> found = scanFlags(lastEvFlags, evFlagsMask, match);
//                        logger.debug("found: " + found);
                        synchronized (flagsListeners) {
                            for (DevicesFlagsListener l : flagsListeners) {
                                if (!found.isEmpty()) {
                                    l.onDevicesFlagsFound(found, evFlagsMask);
                                } else {
                                    l.onDevicesFlagsNotFound(evFlagsMask);
                                }
                            }
                        }

                    } else {
                        synchronized (watchListeners) {
                            for (DevicesFlagsListener l : flagsListeners) {
                                l.onDevicesFlagsReadFailed(exitValue, error);
                            }
                        }
                    }
                }
            }, null, true);


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
}
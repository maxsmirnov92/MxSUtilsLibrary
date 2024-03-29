package net.maxsmr.networkutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Patterns;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.CommandResult;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static net.maxsmr.commonutils.ReflectionUtils.invokeMethodOrThrow;
import static net.maxsmr.commonutils.conversion.NumberConversionUtilsKt.toDoubleOrNull;
import static net.maxsmr.commonutils.logger.holder.BaseLoggerHolder.formatException;
import static net.maxsmr.commonutils.text.SymbolConstsKt.EMPTY_STRING;
import static net.maxsmr.commonutils.text.TextUtilsKt.isEmpty;
import static net.maxsmr.commonutils.shell.ShellUtilsKt.execProcess;

public final class NetworkHelper {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(NetworkHelper.class);

    private static Pattern IPV4_PATTERN = null;
    private static Pattern IPV6_PATTERN = null;

    static {
        try {
            IPV4_PATTERN = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])", Pattern.CASE_INSENSITIVE);
            IPV6_PATTERN = Pattern.compile("([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}", Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            logger.e("Unable to compile pattern", e);
        }
    }

    private NetworkHelper() {
        throw new AssertionError("no instances.");
    }

    public static boolean isOnline(@NotNull Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            throw new RuntimeException(ConnectivityManager.class.getSimpleName() + " is null");
        }
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetInfo != null && activeNetInfo.isConnected() && activeNetInfo.isAvailable();
    }

    @NotNull
    public static NetworkInfo.State getCurrentNetworkState(@NotNull Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            throw new RuntimeException(ConnectivityManager.class.getSimpleName() + " is null");
        }
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null) {
            NetworkInfo.State state = activeNetInfo.getState();

            if (state == null) {
                return NetworkInfo.State.UNKNOWN;
            }

            return state;

        }
        return NetworkInfo.State.DISCONNECTED;
    }

    @Nullable
    public static NetworkTypeInfo getActiveNetworkTypeInfo(@NotNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return new NetworkTypeInfo(
                    activeNetInfo.getType(),
                    activeNetInfo.getSubtype(),
                    activeNetInfo.getTypeName(),
                    activeNetInfo.getSubtypeName()
            );
        }
        return null;
    }


    public static boolean isIpAddress(String ipAddress) {
        return !isEmpty(ipAddress) && Patterns.IP_ADDRESS.matcher(ipAddress).matches();
    }

    public static boolean isIpv4Address(String ipAddress) {
        return !isEmpty(ipAddress) && IPV4_PATTERN.matcher(ipAddress).matches();
    }

    public static boolean isIpv6Address(String ipAddress) {
        return !isEmpty(ipAddress) && IPV6_PATTERN.matcher(ipAddress).matches();
    }

    public static boolean isDomain(String domain) {
        return !isEmpty(domain) && Patterns.DOMAIN_NAME.matcher(domain).matches();
    }

    @Nullable
    public static InetAddress getInetAddressByIp(String ipAddr) {

        if (!isIpAddress(ipAddr)) {
            logger.e("IP address " + ipAddr + " is not a valid IP address");
            return null;
        }

        return getInetAddressByNameSafe(ipAddr);
    }

    @Nullable
    public static InetAddress getInetAddressByDomain(String hostName) {

        if (!isDomain(hostName)) {
            logger.e("hostName " + hostName + " is not a valid host name");
            return null;
        }

        return getInetAddressByNameSafe(hostName);
    }

    private static InetAddress getInetAddressByNameSafe(String name) {
        try {
            return InetAddress.getByName(name);
        } catch (UnknownHostException e) {
            logger.e(formatException(e, "getByName"));
            return null;
        }
    }

    public static boolean checkAddr(String addr) {
        return (getInetAddressByDomain(addr) != null || getInetAddressByIp(addr) != null);
    }

    /**
     * Returns the IP address of the first configured interface of the device
     *
     * @param removeIPv6 If true, IPv6 addresses are ignored
     * @return the IP address of the first configured interface or null
     */
    public static String getLocalIpAddress(boolean removeIPv6) {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (inetAddress.isSiteLocalAddress() && !inetAddress.isAnyLocalAddress()
                            && (!removeIPv6 || isIpv4Address(inetAddress.getHostAddress()))) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            logger.e(formatException(e, "getNetworkInterfaces"));
        }
        return null;
    }

    /**
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.e(formatException(e));
        }
        return EMPTY_STRING;
    }

    /**
     * ping using {@linkplain InetAddress}
     */
    public static boolean isReachable(InetAddress inetAddress, int timeOut) {
        logger.d("isReachable(), inetAddress=" + inetAddress + ", timeOut=" + timeOut);

        if (inetAddress == null) {
            return false;
        }

        if (timeOut < 0 || timeOut > Integer.MAX_VALUE) {
            return false;
        }

        try {
            logger.d("pinging address " + inetAddress.getHostAddress() + "...");
            return inetAddress.isReachable(timeOut);
        } catch (Exception e) {
            logger.e(formatException(e, "isReachable"));
            return false;
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
            logger.e(formatException(e));
            timeClient.close();
        }
        return -1;
    }

    /**
     * ping using "ping" utility
     *
     * @param timeoutUnit null == ms
     * @return ping time in ms or -1 if error occurred
     */
    public static double pingHost(InetAddress inetAddress, int pingCount, long timeout, @Nullable TimeUnit timeoutUnit) {

        if (inetAddress == null) {
            return -1;
        }

        if (pingCount <= 0) {
            pingCount = 1;
        }

        if (timeout < 0) {
            timeout = 0;
        }

        logger.d("pinging address " + inetAddress.getHostAddress() + "...");
        CommandResult result = execProcess(Arrays.asList(
                "ping",
                "-c " + pingCount,
                "-W " + (timeoutUnit == null ? TimeUnit.MILLISECONDS.toSeconds(timeout) : timeoutUnit.toSeconds(timeout)),
                inetAddress.getHostAddress()
                ),
                EMPTY_STRING, null, null, null, null, 0, TimeUnit.SECONDS);

        String resultString = result.getStdOut();

        if (!result.isSuccessful() || isEmpty(resultString)) {
            logger.e("ping failed: " + result);
            return -1;
        }

        logger.d("ping result: " + resultString);

        int startIndex = resultString.indexOf("=", resultString.indexOf("=", resultString.indexOf("=") + 1) + 1) + 1;
        int endIndex = resultString.indexOf(" ms");

        if (startIndex < 0 || endIndex < 0 || startIndex > endIndex) {
            return -1;
        }

        final String pingTimeString = resultString.substring(startIndex, endIndex);
        logger.d("ping time: " + pingTimeString + " ms");

        final Double pingTime = toDoubleOrNull(pingTimeString);
        if (pingTime != null) {
            return pingTime;
        }
        return -1;
    }

    public static boolean isGprsNetworkType(int type) {
        return type == TelephonyManager.NETWORK_TYPE_GPRS;
    }

    public static boolean isEdgeNetworkType(int type) {
        return type == TelephonyManager.NETWORK_TYPE_EDGE;
    }

    public static boolean isHighSpeedNetworkType(int type) {
        return type == TelephonyManager.NETWORK_TYPE_HSDPA || type == TelephonyManager.NETWORK_TYPE_HSPA
                || type == TelephonyManager.NETWORK_TYPE_HSPAP || type == TelephonyManager.NETWORK_TYPE_HSUPA;
    }

    public static boolean isMobileNetworkType(int type) {
        return isGprsNetworkType(type) || isEdgeNetworkType(type) || isHighSpeedNetworkType(type);
    }

    public static boolean isWifiNetworkType(int type) {
        return type == ConnectivityManager.TYPE_WIFI;
    }

    public static boolean isWifiEnabled(@NotNull Context context) {
        final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            throw new RuntimeException("WifiManager is null");
        }
        return wifiManager.isWifiEnabled();
    }

    @SuppressWarnings("ResourceType")
    public static boolean enableWifiConnection(@NotNull Context context, boolean enable) {
        final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            throw new RuntimeException("WifiManager is null");
        }
        return wifiManager.setWifiEnabled(enable);
    }

    public static boolean enableMobileDataConnection(@NotNull Context context, boolean enable) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        try {
            invokeMethodOrThrow(ConnectivityManager.class, "setMobileDataEnabled", new Class<?>[]{boolean.class}, connectivityManager, enable);
            return true;
        } catch (RuntimeException e) {
            logger.e(e);
            return false;
        }
    }

    @Nullable
    public static URL parseURL(String url) {
        return parseURL(url, true);
    }

    @Nullable
    public static URL parseURL(String url, boolean encoded) {
        try {
            return new URL(!encoded ? URLEncoder.encode(url, "UTF-8") : url);
        } catch (Exception e) {
            logger.e(formatException(e));
            return null;
        }
    }
}

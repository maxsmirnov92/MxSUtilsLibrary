package net.maxsmr.networkutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Patterns;

import androidx.core.content.ContextCompat;

import net.maxsmr.commonutils.logger.BaseLogger;
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder;
import net.maxsmr.commonutils.shell.CommandResult;
import net.maxsmr.commonutils.shell.ShellUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
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

public final class NetworkHelper {

    private final static BaseLogger logger = BaseLoggerHolder.getInstance().getLogger(NetworkHelper.class);

    public final static int NETWORK_TYPE_NONE = -1;

    public final static String NETWORK_TYPE_NONE_HR = "none";

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

    public static int getActiveNetworkType(@NotNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return activeNetInfo.getType();
        }
        return NETWORK_TYPE_NONE;
    }

    public static int getActiveNetworkSubtype(@NotNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return activeNetInfo.getSubtype();
        }
        return NETWORK_TYPE_NONE;
    }

    public static String getActiveNetworkTypeHR(@NotNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return activeNetInfo.getTypeName();

        } else {
            return NETWORK_TYPE_NONE_HR;
        }
    }

    public static String getActiveNetworkSubtypeHR(@NotNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return activeNetInfo.getSubtypeName();
        }
        return NETWORK_TYPE_NONE_HR;
    }

    public static boolean isIpAddress(String ipAddress) {
        return !TextUtils.isEmpty(ipAddress) && Patterns.IP_ADDRESS.matcher(ipAddress).matches();
    }

    public static boolean isIpv4Address(String ipAddress) {
        return !TextUtils.isEmpty(ipAddress) && IPV4_PATTERN.matcher(ipAddress).matches();
    }

    public static boolean isIpv6Address(String ipAddress) {
        return !TextUtils.isEmpty(ipAddress) && IPV6_PATTERN.matcher(ipAddress).matches();
    }

    public static boolean isDomain(String domain) {
        return !TextUtils.isEmpty(domain) && Patterns.DOMAIN_NAME.matcher(domain).matches();
    }

    @Nullable
    public static InetAddress getInetAddressByIp(String ipAddr) {

        if (!isIpAddress(ipAddr)) {
            logger.e("IP address " + ipAddr + " is not a valid IP address");
            return null;
        }

        try {
            return InetAddress.getByName(ipAddr);
        } catch (UnknownHostException e) {
            logger.e("an UnknownHostException occurred during getByName(): " + e.getMessage());
            return null;
        }
    }

    @Nullable
    public static InetAddress getInetAddressByDomain(String hostName) {

        if (!isDomain(hostName)) {
            logger.e("hostName " + hostName + " is not a valid host name");
            return null;
        }

        try {
            return InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            logger.e("an UnknownHostException occurred during getByName(): " + e.getMessage());
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
            logger.e("a SocketException occurred during getNetworkInterfaces(): " + e.getMessage());
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
        } catch (Exception ex) {
            // for now eat exceptions
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * ping using {@linkplain InetAddress}
     * */
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
            logger.e("an Exception occurred during isReachable(): " + e.getMessage());
            return false;
        }
    }

    /**
     * ping using {@linkplain ShellUtils}
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
        CommandResult result = ShellUtils.execProcess(Arrays.asList("ping", "-c " + pingCount, "-W " +
                        (timeoutUnit == null? TimeUnit.MILLISECONDS.toSeconds(timeout) : timeoutUnit.toSeconds(timeout)), inetAddress.getHostAddress()),
                null, null,null);

        String resultString = result.getStdOut();

        if (!result.isSuccessful() || TextUtils.isEmpty(resultString)) {
            logger.e("ping failed: " + result);
            return -1;
        }

        logger.d("ping result: " + resultString);

        int startIndex = resultString.indexOf("=", resultString.indexOf("=", resultString.indexOf("=", 0) + 1) + 1) + 1;
        int endIndex = resultString.indexOf(" ms");

        if (startIndex < 0 || endIndex < 0 || startIndex > endIndex) {
            return -1;
        }

        logger.d("ping time: " + resultString.substring(startIndex, endIndex) + " ms");

        try {
            return Double.parseDouble(resultString.substring(startIndex, endIndex));
        } catch (NumberFormatException e) {
            logger.e("a NumberFormatException occurred during parseDouble(): " + e.getMessage());
        }

        return -1;
    }

    public static boolean isGprsNetworkType(int type) {
        return (type == TelephonyManager.NETWORK_TYPE_GPRS);
    }

    public static boolean isEdgeNetworkType(int type) {
        return (type == TelephonyManager.NETWORK_TYPE_EDGE);
    }

    public static boolean isHighSpeedNetworkType(int type) {
        return (type == TelephonyManager.NETWORK_TYPE_HSDPA || type == TelephonyManager.NETWORK_TYPE_HSPA
                || type == TelephonyManager.NETWORK_TYPE_HSPAP || type == TelephonyManager.NETWORK_TYPE_HSUPA);
    }

    public static boolean isMobileNetworkType(int type) {
        return (isGprsNetworkType(type) || isEdgeNetworkType(type) || isHighSpeedNetworkType(type));
    }

    public static boolean isWifiNetworkType(int type) {
        return (type == ConnectivityManager.TYPE_WIFI);
    }

    public static boolean isWifiEnabled(@NotNull Context context) {
        final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            throw new RuntimeException(WifiManager.class.getSimpleName() + " is null");
        }
        return wifiManager.isWifiEnabled();
    }

    @SuppressWarnings("ResourceType")
    public static boolean enableWifiConnection(@NotNull Context context, boolean enable) {
        final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            throw new RuntimeException(WifiManager.class.getSimpleName() + " is null");
        }
        return ContextCompat.checkSelfPermission(context, android.Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED && wifiManager.setWifiEnabled(enable);
    }

    public static boolean enableMobileDataConnection(@NotNull Context context, boolean enable) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        final Method setMobileDataEnabledMtd;

        try {
            setMobileDataEnabledMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            logger.e("a NoSuchMethodException occurred during getDeclaredMethod(): " + e.getMessage());
            return false;
        }

        setMobileDataEnabledMtd.setAccessible(true);

        try {
            setMobileDataEnabledMtd.invoke(connectivityManager, enable);
            return true;
        } catch (Exception e) {
            logger.e("an Exception occurred during invoke: " + e.getMessage());
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
            return null;
        }
    }

}

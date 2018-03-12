package net.maxsmr.networkutils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Patterns;

import net.maxsmr.commonutils.data.MathUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class NetworkHelper {

    private final static Logger logger = LoggerFactory.getLogger(NetworkHelper.class);

    public final static int PING_TIME_NONE = -1;

    public final static int NETWORK_TYPE_NONE = -1;

    public final static String NETWORK_TYPE_NONE_HR = "none";

    private static Pattern IPV4_PATTERN = null;
    private static Pattern IPV6_PATTERN = null;

    static {
        try {
            IPV4_PATTERN = Pattern.compile("(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])", Pattern.CASE_INSENSITIVE);
            IPV6_PATTERN = Pattern.compile("([0-9a-f]{1,4}:){7}([0-9a-f]){1,4}", Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            logger.error("Unable to compile pattern", e);
        }
    }

    public static boolean isOnline(@NonNull Context context) {
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetInfo != null && activeNetInfo.isConnected() && activeNetInfo.isAvailable();
    }

    @NonNull
    public static NetworkInfo.State getCurrentNetworkState(@NonNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null) {
            NetworkInfo.State state = activeNetInfo.getState();

            if (state == null) {
                logger.debug("network state: ", NetworkInfo.State.UNKNOWN);
                return NetworkInfo.State.UNKNOWN;
            }

            logger.debug("network state: ", state);
            return state;

        } else {
            logger.debug("network state: ", NetworkInfo.State.DISCONNECTED);
            return NetworkInfo.State.DISCONNECTED;
        }
    }

    public static int getActiveNetworkType(@NonNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return activeNetInfo.getType();
        }
        return NETWORK_TYPE_NONE;
    }

    public static int getActiveNetworkSubtype(@NonNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return activeNetInfo.getSubtype();
        }
        return NETWORK_TYPE_NONE;
    }

    public static String getActiveNetworkTypeHR(@NonNull Context context) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        @SuppressLint("MissingPermission") final NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();

        if (activeNetInfo != null && activeNetInfo.isConnected()) {
            return activeNetInfo.getTypeName();

        } else {
            return NETWORK_TYPE_NONE_HR;
        }
    }

    public static String getActiveNetworkSubtypeHR(@NonNull Context context) {

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
            logger.error("IP address " + ipAddr + " is not a valid IP address");
            return null;
        }

        try {
            return InetAddress.getByName(ipAddr);
        } catch (UnknownHostException e) {
            logger.error("an UnknownHostException occurred during getByName(): " + e.getMessage());
            return null;
        }
    }

    @Nullable
    public static InetAddress getInetAddressByDomain(String hostName) {

        if (!isDomain(hostName)) {
            logger.error("hostName " + hostName + " is not a valid host name");
            return null;
        }

        try {
            return InetAddress.getByName(hostName);
        } catch (UnknownHostException e) {
            logger.error("an UnknownHostException occurred during getByName(): " + e.getMessage());
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
            logger.error("a SocketException occurred during getNetworkInterfaces(): " + e.getMessage());
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

    public static boolean isReachable(InetAddress inetAddress, long timeOut) {
        logger.debug("isReachable(), inetAddress=" + inetAddress + ", timeOut=" + timeOut);

        if (inetAddress == null) {
            return false;
        }

        if (timeOut < 0 || timeOut > Integer.MAX_VALUE) {
            return false;
        }

        try {
            logger.debug("pinging address " + inetAddress.getHostAddress() + "...");
            return inetAddress.isReachable(MathUtils.safeLongToInt(timeOut));
        } catch (Exception e) {
            logger.error("an Exception occurred during isReachable(): " + e.getMessage());
            return false;
        }
    }

    /**
     * @return ping time in ms or -1 if error occurred
     */
    public static double isReachable(InetAddress inetAddress, int pingCount, int timeoutSec) {
        logger.debug("isReachable(), inetAddress=" + inetAddress + ", pingCount=" + pingCount + ", timeoutSec=" + timeoutSec);

        if (inetAddress == null) {
            return PING_TIME_NONE;
        }

        if (pingCount <= 0) {
            pingCount = 1;
        }

        if (timeoutSec < 0) {
            timeoutSec = 0;
        }

        Runtime runtime = Runtime.getRuntime();

        Process proc = null;
        try {
            logger.debug("pinging address " + inetAddress.getHostAddress() + "...");
            proc = runtime.exec(new String[]{"ping", "-c " + pingCount, "-W " + timeoutSec, inetAddress.getHostAddress()});
        } catch (IOException e) {
            logger.error("an IOException occurred during exec()", e);
            return PING_TIME_NONE;
        }

        BufferedReader procReader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String inputLine = null;
        StringBuilder resultLine = new StringBuilder();

        try {
            while ((inputLine = procReader.readLine()) != null) {
                resultLine.append(inputLine);
            }
        } catch (IOException e) {
            logger.error("an IOException occurred during readLine()", e);
        } finally {
            try {
                procReader.close();
            } catch (IOException e) {
                logger.error("an IOException occurred close()", e);
            }
        }

        String resultString = resultLine.toString();
        logger.debug("ping result: " + resultString);

        // Pattern pattern = Pattern.compile("=(.*?)ms");
        // Matcher matcher = pattern.matcher(resultString);
        // if (matcher.find()) {
        // logger.debug("find by matcher: " + matcher.group(1));
        // }

        int startIndex = resultString.indexOf("=", resultString.indexOf("=", resultString.indexOf("=", 0) + 1) + 1) + 1;
        int endIndex = resultString.indexOf(" ms");

        if (startIndex < 0 || endIndex < 0 || startIndex > endIndex) {
            return PING_TIME_NONE;
        }

        logger.debug("ping time: " + resultString.substring(startIndex, endIndex) + " ms");

        try {
            return Double.parseDouble(resultString.substring(startIndex, endIndex));
        } catch (NumberFormatException e) {
            logger.error("a NumberFormatException occurred during parseDouble(): " + e.getMessage());
        }

        return PING_TIME_NONE;
    }

    public static boolean isSimCardInserted(@NonNull Context context) {

        final TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (telephonyManager.getSimState() != TelephonyManager.SIM_STATE_ABSENT) {
            return true;
        } else {
            return false;
        }
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

    public static boolean isWifiEnabled(@NonNull Context context) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    @SuppressWarnings("ResourceType")
    public static boolean enableWifiConnection(@NonNull Context context, boolean enable) {
        final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED) {
            return wifiManager.setWifiEnabled(enable);
        }
        return false;
    }

    public static boolean enableMobileDataConnection(@NonNull Context context, boolean enable) {

        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        final Method setMobileDataEnabledMtd;

        try {
            setMobileDataEnabledMtd = ConnectivityManager.class.getDeclaredMethod("setMobileDataEnabled", boolean.class);
        } catch (NoSuchMethodException e) {
            logger.error("a NoSuchMethodException occurred during getDeclaredMethod(): " + e.getMessage());
            return false;
        }

        setMobileDataEnabledMtd.setAccessible(true);

        try {
            setMobileDataEnabledMtd.invoke(connectivityManager, enable);
            return true;
        } catch (Exception e) {
            logger.error("an Exception occurred during invoke: " + e.getMessage());
            return false;
        }
    }

    @Nullable
    public static URL parseURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    @Nullable
    public static URL parseEncodedURL(String url) {
        try {
            return new URL(URLEncoder.encode(url, "UTF-8"));
        } catch (Exception e) {
            return null;
        }
    }

}

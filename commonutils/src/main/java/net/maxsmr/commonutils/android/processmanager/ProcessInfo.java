package net.maxsmr.commonutils.android.processmanager;

import org.jetbrains.annotations.NotNull;

public class ProcessInfo {
    
    public final String packageName;
    
    public final String applicationName;
    
    public final int pid;
    
    public final String user;
    
    public final int userId;
    
    public final int rss;
    
    public final int vSize;
    
    public final boolean isForeground;
    
    public final boolean isSystemApp;

    public ProcessInfo(String packageName, CharSequence applicationName,
                       int pid, String user, int userId, int rss, int vSize,
                       boolean isForeground, boolean isSystemApp) {
        this.packageName = packageName;
        this.applicationName = applicationName != null? applicationName.toString() : null;
        this.pid = pid;
        this.user = user;
        this.userId = userId;
        this.rss = rss;
        this.vSize = vSize;
        this.isForeground = isForeground;
        this.isSystemApp = isSystemApp;
    }

    @NotNull
    @Override
    public String toString() {
        return "ProcessInfo{" +
                "packageName='" + packageName + '\'' +
                ", applicationName='" + applicationName + '\'' +
                ", pid=" + pid +
                ", user='" + user + '\'' +
                ", userId=" + userId +
                ", rss=" + rss +
                ", vSize=" + vSize +
                ", isForeground=" + isForeground +
                ", isSystemApp=" + isSystemApp +
                '}';
    }
}

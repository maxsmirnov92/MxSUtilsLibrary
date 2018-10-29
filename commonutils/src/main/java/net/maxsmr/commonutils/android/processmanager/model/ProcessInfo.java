package net.maxsmr.commonutils.android.processmanager.model;


import net.maxsmr.commonutils.data.CompareUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProcessInfo {
    
    public final String packageName;
    
    public final String applicationName;
    
    public final int pid;

    public final int pPid;

    public final String user;
    
    public final int userId;
    
    public final int rss;
    
    public final int vSize;

    @Nullable
    public final ProcessState processState;

    public final Boolean isForeground;
    
    public final boolean isSystemApp;

    public ProcessInfo(String packageName, CharSequence applicationName,
                       int pid, int pPid, String user, int userId, int rss, int vSize,
                       @Nullable ProcessState processState,
                       Boolean isForeground, boolean isSystemApp) {
        this.packageName = packageName;
        this.applicationName = applicationName != null? applicationName.toString() : null;
        this.pid = pid;
        this.pPid = pPid;
        this.user = user;
        this.userId = userId;
        this.rss = rss;
        this.vSize = vSize;
        this.processState = processState;
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
                ", pPid=" + pPid +
                ", user='" + user + '\'' +
                ", userId=" + userId +
                ", rss=" + rss +
                ", vSize=" + vSize +
                ", processState=" + processState +
                ", isForeground=" + isForeground +
                ", isSystemApp=" + isSystemApp +
                '}';
    }

    public enum ProcessState {

        /** uninterruptible sleep (usually IO) */
        D,
        /** Idle kernel thread */
        I,
        /** running or runnable (on run queue) */
        R,
        /** interruptible sleep (waiting for an event to complete) */
        S,
        /** stopped by job control signal */
        T,
        /** stopped by debugger during the tracing */
        t,
        /** paging (not valid since the 2.6.xx kernel) */
        W,
        /** dead (should never be seen) */
        X,
        /** defunct ("zombie") process, terminated but not reaped by */
        Y;

        @Nullable
        public static ProcessState fromName(String name) {
            for (ProcessState s : ProcessState.values()) {
                if (CompareUtils.stringsEqual(name, s.name(), false)) {
                    return s;
                }
            }
            return null;
        }
    }

    public enum PCY {

        fg, bg;

        @Nullable
        public static PCY fromName(String name) {
            for (PCY p : PCY.values()) {
                if (CompareUtils.stringsEqual(name, p.name(), false)) {
                    return p;
                }
            }
            return null;
        }
    }
}

package net.maxsmr.commonutils.android.processmanager;

import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;

import net.maxsmr.commonutils.android.processmanager.model.ProcessInfo;
import net.maxsmr.commonutils.android.processmanager.shell.BusyboxPsProcessManager;
import net.maxsmr.commonutils.android.processmanager.shell.BusyboxTopProcessManager;
import net.maxsmr.commonutils.android.processmanager.shell.PsProcessManager;
import net.maxsmr.commonutils.android.processmanager.shell.ToolboxPsProcessManager;
import net.maxsmr.commonutils.android.processmanager.shell.TopProcessManager;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ProcessManagerWrapper {

    private static volatile ProcessManagerWrapper instance;

    @NotNull
    private final AbstractProcessManager processManager;

    @Nullable
    private List<ProcessInfo> cachedProcessList;

    private boolean isFirstLaunch = true;

    private ProcessManagerWrapper(@NotNull Context context) {
        processManager = createProcessManager(context);
    }

    @NonNull
    public static ProcessManagerWrapper getInstance(@NotNull Context context) {
        ProcessManagerWrapper localInstance = instance;

        if (localInstance == null) {
            synchronized (ProcessManagerWrapper.class) {
                localInstance = instance;

                if (localInstance == null) {
                    instance = localInstance = new ProcessManagerWrapper(context);
                }
            }
        }

        return localInstance;
    }

    @NonNull
    public List<ProcessInfo> getCachedProcessList() {
        if (cachedProcessList == null) {
            cachedProcessList = new ArrayList<>();
        }
        return new ArrayList<>(cachedProcessList);
    }

    @NotNull
    public AbstractProcessManager getProcessManager() {
        return processManager;
    }

    @NotNull
    public List<ProcessInfo> getProcesses(boolean includeSystemPackages) {
        try {
            if (isFirstLaunch && cachedProcessList != null && !cachedProcessList.isEmpty()) {
                return getCachedProcessList();
            }
            cachedProcessList = processManager.getProcesses(includeSystemPackages);
            return getCachedProcessList();
        } finally {
            isFirstLaunch = false;
        }
    }

    @NotNull
    private AbstractProcessManager createProcessManager(@NotNull Context context) {

        final DefaultProcessManager defaultProcessManager = new DefaultProcessManager(context);

        final Set<AbstractProcessManager> priorityManagers = new LinkedHashSet<>();
        final boolean isKitKat = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT;
        if (isKitKat) {
            priorityManagers.add(defaultProcessManager);
        }
        priorityManagers.add(new ToolboxPsProcessManager(context));
        priorityManagers.add(new TopProcessManager(context));
        priorityManagers.add(new BusyboxTopProcessManager(context));
        priorityManagers.add(new PsProcessManager(context));
        priorityManagers.add(new BusyboxPsProcessManager(context));
        priorityManagers.add(defaultProcessManager);
        if (!isKitKat) {
            priorityManagers.add(defaultProcessManager);
        }

        final Iterator<AbstractProcessManager> it = priorityManagers.iterator();

        AbstractProcessManager targetManager = null;

        while (it.hasNext()) {
            targetManager = it.next();
            final List<ProcessInfo> processes = targetManager.getProcesses(true);
            if (!processes.isEmpty()) {
                cachedProcessList = processes;
                break;
            }
        }

        if (targetManager == null) {
            targetManager = defaultProcessManager;
        }

        return targetManager;
    }


}

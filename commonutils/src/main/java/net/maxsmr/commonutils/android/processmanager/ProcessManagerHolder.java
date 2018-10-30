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

public class ProcessManagerHolder {

    private static volatile ProcessManagerHolder instance;

    @NotNull
    private Context context;

    @Nullable
    private AbstractProcessManager processManager;

    @Nullable
    private List<ProcessInfo> cachedProcessList;

    private boolean isFirstLaunch = true;

    public static void initInstance(@NonNull Context context, @NotNull IProcessManagerHolderProvider<?> provider) {
        synchronized (ProcessManagerHolder.class) {
            if (instance == null) {
                instance = provider.provideProcessManagerHolder(context);
            }
        }
    }

    @NotNull
    public static ProcessManagerHolder getInstance() {
        synchronized (ProcessManagerHolder.class) {
            if (instance == null) {
                throw new IllegalStateException(ProcessManagerHolder.class.getSimpleName() + " is not initialized");
            }
            return instance;
        }
    }

    protected ProcessManagerHolder(@NotNull Context context) {
        this.context = context;
    }

    @NotNull
    public List<ProcessInfo> getCachedProcessList() {
        if (cachedProcessList == null) {
            cachedProcessList = new ArrayList<>();
        }
        return new ArrayList<>(cachedProcessList);
    }

    @NotNull
    public AbstractProcessManager getProcessManager() {
        if (processManager == null) {
            processManager = createProcessManager(context);
        }
        return processManager;
    }

    @NotNull
    public List<ProcessInfo> getProcesses(boolean includeSystemPackages) {
        try {
            if (isFirstLaunch && cachedProcessList != null && !cachedProcessList.isEmpty()) {
                return getCachedProcessList();
            }
            cachedProcessList = getProcessManager().getProcesses(includeSystemPackages);
            return getCachedProcessList();
        } finally {
            isFirstLaunch = false;
        }
    }

    @NotNull
    protected Set<AbstractProcessManager> getManagersByPriority(@NotNull Context context) {
        @NotNull Set<AbstractProcessManager> managers = new LinkedHashSet<>();
        final DefaultProcessManager defaultProcessManager = new DefaultProcessManager(context);
        final boolean isKitKat = Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT;
        if (isKitKat) {
            managers.add(defaultProcessManager);
        }
        managers.add(new ToolboxPsProcessManager(context));
        managers.add(new TopProcessManager(context));
        managers.add(new BusyboxTopProcessManager(context));
        managers.add(new PsProcessManager(context));
        managers.add(new BusyboxPsProcessManager(context));
        if (!isKitKat) {
            managers.add(defaultProcessManager);
        }
        return managers;
    }

    @NotNull
    protected AbstractProcessManager createProcessManager(@NotNull Context context) {

        final Set<AbstractProcessManager> managers = getManagersByPriority(context);

        final Iterator<AbstractProcessManager> it = managers.iterator();

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
            targetManager = new DefaultProcessManager(context);
        }

        return targetManager;
    }

    public interface IProcessManagerHolderProvider<P extends ProcessManagerHolder> {

        @NotNull P provideProcessManagerHolder(@NotNull Context context);

        final class Default implements IProcessManagerHolderProvider<ProcessManagerHolder> {

            @NotNull
            @Override
            public ProcessManagerHolder provideProcessManagerHolder(@NotNull Context context) {
                return new ProcessManagerHolder(context);
            }
        }
    }

}

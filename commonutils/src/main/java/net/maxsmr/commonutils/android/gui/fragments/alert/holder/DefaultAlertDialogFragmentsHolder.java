package net.maxsmr.commonutils.android.gui.fragments.alert.holder;

import androidx.fragment.app.FragmentManager;

import net.maxsmr.commonutils.android.gui.fragments.alert.AlertDialogFragment;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class DefaultAlertDialogFragmentsHolder extends BaseAlertDialogFragmentsHolder<AlertDialogFragment.EventListener,
        BaseAlertDialogFragmentsHolder.AlertEventsObservable<AlertDialogFragment.EventListener>> {

    public DefaultAlertDialogFragmentsHolder(@NotNull Collection<String> tags) {
        super(tags);
    }

    public DefaultAlertDialogFragmentsHolder(@Nullable FragmentManager fragmentManager, @NotNull Collection<String> tags) {
        super(fragmentManager, tags);
    }

    @NotNull
    @Override
    protected AlertEventsObservable<AlertDialogFragment.EventListener> newAlertEventsObservable() {
        return new AlertEventsObservable<>();
    }
}

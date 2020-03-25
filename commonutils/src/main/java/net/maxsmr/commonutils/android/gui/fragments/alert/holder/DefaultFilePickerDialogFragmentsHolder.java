package net.maxsmr.commonutils.android.gui.fragments.alert.holder;


import net.maxsmr.commonutils.android.gui.fragments.alert.AlertDialogFragment;

import org.jetbrains.annotations.NotNull;

public class DefaultFilePickerDialogFragmentsHolder extends BaseFilePickerDialogFragmentsHolder<AlertDialogFragment.EventListener, BaseAlertDialogFragmentsHolder.AlertEventsObservable<AlertDialogFragment.EventListener>> {

    @NotNull
    @Override
    protected AlertEventsObservable<AlertDialogFragment.EventListener> newAlertEventsObservable() {
        return new AlertEventsObservable<>();
    }
}

package net.maxsmr.networkutils;

import android.support.annotation.NonNull;

public interface IBuilder<O> {

    @NonNull
    O build();
}

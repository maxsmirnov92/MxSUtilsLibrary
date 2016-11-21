package net.maxsmr.commonutils.data.model;

import android.support.annotation.NonNull;

public interface IBuilder<O> {

    @NonNull
    O build();
}

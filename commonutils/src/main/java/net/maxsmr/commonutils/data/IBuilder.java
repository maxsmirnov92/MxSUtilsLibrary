package net.maxsmr.commonutils.data;

import android.support.annotation.NonNull;

public interface IBuilder<O> {

    @NonNull
    O build();
}

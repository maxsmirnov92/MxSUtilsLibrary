package net.maxsmr.commonutils.data.model;

import org.jetbrains.annotations.NotNull;

public interface IBuilder<O> {

    @NotNull
    O build();
}

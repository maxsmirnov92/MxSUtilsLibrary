package net.maxsmr.commonutils.model;

import org.jetbrains.annotations.NotNull;

public interface IBuilder<O> {

    @NotNull
    O build();
}

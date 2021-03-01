package net.maxsmr.commonutils.logger.holder;

import org.jetbrains.annotations.NotNull;

public interface ILoggerHolderProvider<H extends BaseLoggerHolder> {

    @NotNull H provideHolder();
}

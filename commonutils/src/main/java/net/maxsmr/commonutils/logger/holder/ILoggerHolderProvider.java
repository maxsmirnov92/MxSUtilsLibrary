package net.maxsmr.commonutils.logger.holder;

import android.support.annotation.NonNull;

public interface ILoggerHolderProvider<H extends BaseLoggerHolder> {

    @NonNull H provideHolder();
}

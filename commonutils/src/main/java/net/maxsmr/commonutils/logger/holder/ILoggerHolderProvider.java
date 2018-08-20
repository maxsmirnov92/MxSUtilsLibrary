package net.maxsmr.commonutils.logger.holder;

public interface ILoggerHolderProvider<H extends BaseLoggerHolder> {

    H provideHolder();
}

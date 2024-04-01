package net.maxsmr.rx_extensions.actions

abstract class BaseTaggedViewModelAction<Actor>: BaseViewModelAction<Actor>() {

    abstract val tag: String
}
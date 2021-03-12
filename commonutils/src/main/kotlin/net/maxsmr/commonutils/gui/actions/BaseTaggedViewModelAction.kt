package net.maxsmr.commonutils.gui.actions

abstract class BaseTaggedViewModelAction<Actor>: BaseViewModelAction<Actor>() {

    abstract val tag: String
}
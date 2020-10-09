package net.maxsmr.commonutils.android.gui.actions

abstract class BaseTaggedViewModelAction<Actor>: BaseViewModelAction<Actor>() {

    abstract val tag: String
}
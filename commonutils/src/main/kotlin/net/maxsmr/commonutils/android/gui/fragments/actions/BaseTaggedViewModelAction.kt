package net.maxsmr.commonutils.android.gui.fragments.actions

abstract class BaseTaggedViewModelAction<Actor>: BaseViewModelAction<Actor>() {

    abstract val tag: String
}
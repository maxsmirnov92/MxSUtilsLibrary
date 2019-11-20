package net.maxsmr.commonutils.android.gui

import androidx.viewpager.widget.ViewPager

open class DefaultOnPageChangedListener : ViewPager.OnPageChangeListener {

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        //do nothing
    }

    override fun onPageSelected(position: Int) {
        //do nothing
    }

    override fun onPageScrollStateChanged(state: Int) {
        //do nothing
    }
}

class OnPageSelectedListener(
        private val listener: (position: Int) -> Unit
) : ViewPager.OnPageChangeListener {

    override fun onPageSelected(position: Int) {
        listener(position)
    }

    override fun onPageScrollStateChanged(state: Int) {
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
    }
}

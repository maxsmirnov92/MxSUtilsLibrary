package net.maxsmr.testapp.activity

import androidx.appcompat.app.AppCompatActivity
import net.maxsmr.commonutils.logger.BaseLogger
import net.maxsmr.commonutils.logger.holder.BaseLoggerHolder

class TestAnyActivity : AppCompatActivity() {

    private val logger = BaseLoggerHolder.instance.getLogger<BaseLogger>(TestAnyActivity::class.java)

    override fun onResume() {
        super.onResume()
        logger.d("onResume")
    }
}
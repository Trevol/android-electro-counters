package com.tavrida.testsondevice

import android.app.Instrumentation
import android.os.Bundle
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.InputStreamReader

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("aa.example.testsondevice", appContext.packageName)
        appContext.assets.open("asset_123.txt").use { is_ ->
            val s = InputStreamReader(is_).use { r -> r.readText() }
            Log.d("TEST-TEST", s)
        }
    }

    @Test
    fun simpleTest() {
        Log.d("TEST-TEST", "HELLO Log.d!!!!")

        System.out.println("HELLLLLLLLLO!!!!")
    }

    private fun out(str: String) {
        val b = Bundle()
        b.putString(Instrumentation.REPORT_KEY_STREAMRESULT, "\n$str")
        InstrumentationRegistry.getInstrumentation().sendStatus(0, b)
    }

}
package com.tavrida.testsondevice

import android.content.Context
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CoroutineTest {
    companion object {
        init {
            // OpenCVLoader.initDebug()
        }

        const val TAG = "COROUTINE_TESTS"

        fun appContext(): Context {
            return InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    @Test
    fun coroutineTest() {
        val appContext = appContext()
        Log.d(TAG, "START ${System.currentTimeMillis()}")
        runBlocking {
            repeat(3) { workerId ->
                launch(Dispatchers.Default) {
                    Log.d(TAG, "workerId($workerId) ${Thread.currentThread().id}")
                    delay(3000)
                }
            }
        }
        Log.d(TAG, "${Thread.currentThread().id} THE END!")
    }
}
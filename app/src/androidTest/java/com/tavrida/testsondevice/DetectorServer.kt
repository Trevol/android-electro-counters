package com.tavrida.testsondevice

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tavrida.utils.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.title
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import java.io.*
import java.nio.ByteBuffer
import kotlin.system.measureTimeMillis
import kotlin.text.get


@RunWith(AndroidJUnit4::class)
class DetectorServer {
    companion object {
        const val TAG = "DetectorServer_TAg"

        fun appContext(): Context {
            return InstrumentationRegistry.getInstrumentation().targetContext
        }

        fun String.log() = Log.d(TAG, this)
        fun Any.log() = this.toString().log()

    }

    @Test
    fun runServer() {
        embeddedServer(CIO, port = 8080, module = { serverModule() })
            .start(wait = true)
    }

    private fun Application.serverModule() {
        install(ContentNegotiation) { json() }

        routing {
            route("/") {
                get {
                    call.respondHtml {
                        head() {
                            title() {
                                +"Server on Android!!!"
                            }
                        }
                        body {
                            div { +"Content from Android Server!!!!!" }
                        }
                    }
                }
            }

            route("/api") {
                route("/customerReading") {
                    get {
//                            call.respond(db.allReadings())
                    }
                    post {
                        /*val reading = call.receive<CustomerReading>()
                        val id = db.save(reading)
                        call.respond(id)*/
                    }
                }
            }
        }

    }

}
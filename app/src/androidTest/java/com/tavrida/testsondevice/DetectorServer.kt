package com.tavrida.testsondevice

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tavrida.electro_counters.detection.tflite.new_detector.TfliteDetector
import com.tavrida.utils.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.head
import kotlinx.html.title
import kotlinx.serialization.Serializable
import org.junit.Test
import org.junit.runner.RunWith
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel


@RunWith(AndroidJUnit4::class)
class DetectorServer {
    companion object {
        fun appContext(): Context {
            return InstrumentationRegistry.getInstrumentation().targetContext
        }
    }

    private object detectorFactory {
        private const val MODEL_FILE = "screen_digits_320x128_351_noblur.tflite"
        private val inputSize = Size(320, 128)

        private fun mapAssetFile(context: Context, fileName: String): ByteBuffer {
            val assetFd = context.assets.openFd(fileName)
            val start = assetFd.startOffset
            val length = assetFd.declaredLength
            return FileInputStream(assetFd.fileDescriptor).channel
                .map(FileChannel.MapMode.READ_ONLY, start, length)
        }

        fun create(context: Context, warmup: Boolean = true): TfliteDetector {
            val instance = mapAssetFile(context, MODEL_FILE)
                .let {
                    TfliteDetector(it, inputSize.height, inputSize.width, context.filesDir)
                }
            if (warmup) {
                instance.detect(
                    Bitmap.createBitmap(inputSize.width, inputSize.height, Bitmap.Config.ARGB_8888),
                    .2f
                )
            }
            return instance
        }
    }

    @Test
    fun runServer() {
        // tavrida-electro-counters/data/files_2/00000.pixel_data
        val dataDir =
            File(Environment.getExternalStorageDirectory(), "tavrida-electro-counters/data")
        val detector = detectorFactory.create(appContext(), true)
        embeddedServer(CIO, port = 8080, module = { serverModule(detector, dataDir) })
            .start(wait = true)
    }

    private fun Application.serverModule(detector: TfliteDetector, dataDir: File) {
        install(ContentNegotiation) { json() }



        fun detect(request: DetectRequest): DetectResponse {
            val imageFile = File(request.image_path).let {
                val imageFileName = it.name
                val imageDir = it.parentFile.name
                //remap to dataDir
                File(dataDir, imageDir, imageFileName)
            }

            "Server: api/detect: ${request.image_path}".log()
            "Server: api/detect: ${imageFile.absolutePath}".log()
            "Server: api/detect: **************************".log()

            val image = BitmapFactoryEx.decodeFile(imageFile)

            val detections = detector.detect(image, .2f)

            val boxes = mutableListOf<List<Float>>()
            val classes = mutableListOf<Float>()
            val scores = mutableListOf<Float>()

            for (d in detections) {
                //yxyx
                boxes.add(
                    listOf(
                        d.location.top / image.height,
                        d.location.left / image.width,
                        d.location.bottom / image.height,
                        d.location.right / image.width
                    )
                )
                classes.add(d.classId.toFloat())
                scores.add(d.score)
            }

            val response = DetectResponse(boxes, classes, scores)
            return response
        }

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
                route("/detect") {
                    get {
//                            call.respond(db.allReadings())
                    }
                    post {
                        val request = call.receive<DetectRequest>()
                        val response = detect(request)
                        call.respond(response)

                    }
                }
            }
        }

    }

    @Serializable
    data class DetectRequest(val image_path: String)

    @Serializable
    data class DetectResponse(
        val boxes: List<List<Float>>,
        val classes: List<Float>,
        val scores: List<Float>
    )
}
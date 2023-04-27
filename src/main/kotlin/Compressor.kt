import kotlinx.coroutines.*
import org.imgscalr.Scalr
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.FileUtils
import ws.schild.jave.Encoder
import ws.schild.jave.EncoderException
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import ws.schild.jave.encode.VideoAttributes
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.extension
import kotlin.io.path.name
import kotlin.math.ceil

object Compressor {
    var MAX_DIMENSION_IMG = 1000
    var BIT_RATE_MP3 = 128 * 1000
        set(value) {
            field = value * 1000
        }
    var CHANNELS_MP3 = 2
    var SAMPLING_RATE = 44100
    var VIDEO_BIT_RATE = 300 * 1024
        set(value) {
            field = value * 1024
        }
    var CPU_CORES = Runtime.getRuntime().availableProcessors()
    var VIDEO_FORMAT = "libx264"
        set(value) {
            CPU_CORES = if (value != "libx264") {
                1
            } else {
                Runtime.getRuntime().availableProcessors()
            }
            field = value
        }

    val VIDEO_CODECS = listOf("libx264", "h264_nvenc", "h264_amf")
    val VIDEO_BITRATES = (20..900 step 80).map { it.toString() }.toList()
    val AUDIO_BITRATES = (20..320 step 20).map { it.toString() }.toList()

    suspend fun compress(
        sigameFile: File?,
        statusSet: (Float, String) -> Unit
    ): String? = withContext(Dispatchers.IO) {
        var progr = 0f
        sigameFile ?: return@withContext null
        progr += 0.1f
        statusSet(progr, "распаковка началась")
        val tmpFolder = createTempDirectory(prefix = "temp").toFile()
        tmpFolder.deleteOnExit()
        unZipPack(sigameFile, tmpFolder)
        progr += 0.1f
        statusSet(progr, "распаковка завершена")

        val filesCount = tmpFolder.listFiles()?.flatMap { it.listFiles()?.toList()?:listOf() }?.count()?:1
        var processedCount = 0
        val startStatus = progr
        val funStatuses = arrayOf(
            "идёт обработка", "шакализируем ваши мемы", "ищу аниме.....",
            "опять в финале кринжуха, может просто удалить?",
            "спасибо Владимир Хиль", "спасибо Владимир Хиль",
            "как сжать кота в мешке?"
        )
        funStatuses.shuffle()
        fun progress() {
            processedCount += 1
            progr = startStatus + (processedCount.toFloat() / filesCount) * (0.95f - startStatus)
            val curStatus = funStatuses[ceil((processedCount.toFloat() / filesCount) * funStatuses.lastIndex).toInt()]
            statusSet(progr, curStatus)
        }

        val jobs = tmpFolder.listFiles()?.map {
            when (it.name) {
                "Video" -> async {
                    videoFiles(it!!.listFiles()!!, ::progress)
                }
                "Audio" -> async {
                    audioFiles(it!!.listFiles()!!, ::progress)
                }
                "Images" -> async {
                    imageFiles(it!!.listFiles()!!, ::progress)
                }
                else -> { async {}}
            }
        }?:listOf()

        awaitAll(*jobs.toTypedArray())

        statusSet(0.98f, "пакуем пак")

        val newSIGameFile = File(sigameFile.parent + "/compressed" + sigameFile.name)
        ZipUtil.pack(tmpFolder, newSIGameFile)
        statusSet(1f, newSIGameFile.absolutePath)
        tmpFolder.deleteRecursively()
        return@withContext newSIGameFile.absolutePath
    }

    private fun fixURLName(contentXML: StringBuilder, zipEntry: ZipEntry): String {
        var newName = zipEntry.name
        if (zipEntry.name.contains('%')) {
            val unURLPath = Path(URLDecoder.decode(zipEntry.name, StandardCharsets.UTF_8.name()))
            newName = "fixName${System.nanoTime()}.${unURLPath.extension}"
            var curInd = contentXML.indexOf(unURLPath.name)
            while (curInd in contentXML.indices) {
                contentXML.replace(curInd, curInd + unURLPath.name.length, newName)
                curInd = contentXML.indexOf(unURLPath.name)
            }
            newName = "${unURLPath.parent}/$newName"
        }
        return newName
    }

    private fun unZipPack(sigameFile: File, targetFolder: File) {
        try {
            ZipUtil.unpack(sigameFile, targetFolder)
        } catch (e: org.zeroturnaround.zip.ZipException) {
            FileUtils.deleteDirectory(targetFolder)
            val contentXML = StringBuilder(String(ZipUtil.unpackEntry(sigameFile, "content.xml")))
            val zipStream = ZipInputStream(sigameFile.inputStream())
            val buffer = ByteArray(4 * 1024)
            zipStream.use { stream ->
                var zipEntry = stream.nextEntry

                while (zipEntry != null) {
                    val newName = fixURLName(contentXML, zipEntry)
                    val entryFile = File(targetFolder, newName)

                    if (zipEntry.isDirectory) {
                        entryFile.mkdirs()
                    } else {
                        entryFile.parentFile?.mkdirs()

                        FileOutputStream(entryFile).use { outputStream ->
                            var len: Int
                            while (stream.read(buffer).also { len = it } > 0) {
                                outputStream.write(buffer, 0, len)
                            }
                        }
                    }

                    zipEntry = stream.nextEntry
                }
            }
            val contXMLFile = File(targetFolder.absolutePath + "/content.xml")
            contXMLFile.writeText(contentXML.toString())
        }
    }

    private fun videoFiles(files: Array<File>, progress: () -> Unit) {
        val video = VideoAttributes()
        video.setCodec(VIDEO_FORMAT)

        val attrs = EncodingAttributes()
        attrs.setInputFormat("mp4")
        attrs.setOutputFormat("mp4")
        attrs.setEncodingThreads(CPU_CORES)
        attrs.setDecodingThreads(CPU_CORES)

        val encoder = Encoder()

        for (file in files) {
            val typeCheck = file.extension
            progress()
            if (typeCheck != "mp4")
                continue
            val tmpFile = File(file.parent + "/tmp" + file.name)
            try {
                val mFile = MultimediaObject(file)
                val videoCurAttr = mFile.info.video
                if (videoCurAttr.bitRate <= VIDEO_BIT_RATE)
                    continue

                video.setSize(videoCurAttr.size)
                video.setBitRate(VIDEO_BIT_RATE)
                video.setFrameRate(videoCurAttr.frameRate.toInt())

                val audioCurAttr = mFile.info.audio
                val audio = AudioAttributes()
                audio.setCodec("libmp3lame")
                audio.setBitRate(minOf(BIT_RATE_MP3, audioCurAttr.bitRate))
                audio.setChannels(CHANNELS_MP3)
                audio.setSamplingRate(audioCurAttr.samplingRate)

                attrs.setVideoAttributes(video)
                attrs.setAudioAttributes(audio)

                encoder.encode(mFile, tmpFile, attrs)
                file.delete()
                tmpFile.renameTo(file)
            } catch (e: EncoderException) {
                tmpFile.delete()
            }
        }
    }

    private fun audioFiles(files: Array<File>, progress: () -> Unit) {
        val audio = AudioAttributes()
        audio.setCodec("libmp3lame")
        audio.setBitRate(BIT_RATE_MP3)
        audio.setChannels(CHANNELS_MP3)
        audio.setSamplingRate(SAMPLING_RATE)

        val attrs = EncodingAttributes()
        attrs.setInputFormat("mp3")
        attrs.setOutputFormat("mp3")
        attrs.setAudioAttributes(audio)

        val encoder = Encoder()

        for (file in files) {
            val typeCheck = file.extension
            progress()
            if (typeCheck == "mp3") {
                val tmpFile = File(file.parent + "/tmp" + file.name)
                try {
                    val mFile = MultimediaObject(file)
                    encoder.encode(mFile, tmpFile, attrs)
                    file.delete()
                    tmpFile.renameTo(file)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun imageFiles(files: Array<File>, progress: () -> Unit) {
        for (file in files) {
            progress()
            if (file.extension == "gif") {
                continue
            }
            try {
                val originalImage = ImageIO.read(ByteArrayInputStream(file.readBytes())) ?: continue
                val currentDimension = minOf(MAX_DIMENSION_IMG, maxOf(originalImage.height, originalImage.width))
                val resizedImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, currentDimension)
                ImageIO.write(resizedImage, "jpg", file)
            } catch (e: Exception) {
                continue
            }
        }
    }
}


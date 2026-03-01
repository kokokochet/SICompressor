package compressor

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import org.imgscalr.Scalr
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.FileUtils
import ws.schild.jave.Encoder
import ws.schild.jave.MultimediaObject
import ws.schild.jave.encode.AudioAttributes
import ws.schild.jave.encode.EncodingAttributes
import ws.schild.jave.encode.VideoAttributes
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.extension
import kotlin.io.path.name

/**
 * Snapshot of settings for thread-safe access during compression.
 * Created once at the start of compression to prevent issues with mutable SettingsState.
 */
data class SettingsSnapshot(
    val videoCodec: String,
    val videoBitrate: Int,
    val audioBitrate: Int,
    val cpuCores: Int,
    val imageResolution: Int,
    val audioChannels: Int,
    val samplingRate: Int
) {
    companion object {
        fun fromSettings(settings: SettingsState): SettingsSnapshot = SettingsSnapshot(
            videoCodec = settings.videoCodec.currentValue,
            videoBitrate = settings.videoBitrate.currentValue,
            audioBitrate = settings.audioBitrate.currentValue,
            cpuCores = settings.cpuCores.currentValue,
            imageResolution = settings.imageResolution.currentValue,
            audioChannels = settings.audioChannels,
            samplingRate = settings.samplingRate
        )
    }
}

suspend fun compress(
    set: SettingsState,
    siGameFile: File,
    initialState: CompressState,
    onProgress: (CompressState) -> CompressState
) = withContext(Dispatchers.IO) {
    // Create immutable snapshot of settings to prevent race conditions
    val settings = SettingsSnapshot.fromSettings(set)
    
    // Use AtomicInteger for thread-safe counter, and lock for state updates
    val processedCount = AtomicInteger(0)
    val lock = Any()
    var curState = initialState
    
    // Thread-safe progress update
    fun reportProgress() {
        synchronized(lock) {
            val count = processedCount.incrementAndGet()
            val newProgress = CompressState.START_PROGRESS +
                    (count.toFloat() / curState.filesCount) * (CompressState.COMPRESS_DONE_PROGRESS - CompressState.START_PROGRESS)
            val statusIndex = minOf(
                ((count.toFloat() / curState.filesCount) * curState.funStatuses.lastIndex).toInt(),
                curState.funStatuses.lastIndex
            )
            curState = onProgress(curState.copy(
                processedCount = count,
                progress = newProgress,
                stringStatus = curState.funStatuses[statusIndex]
            ))
        }
    }
    
    curState = onProgress(curState.unpackingStart())
    val tmpFolder = createTempDirectory(prefix = "temp").toFile()
    
    try {
        tmpFolder.deleteOnExit()
        unZipPack(siGameFile, tmpFolder)
        
        val filesCount = tmpFolder.listFiles()?.sumOf { it.listFiles()?.size ?: 0 } ?: 1
        synchronized(lock) {
            curState = onProgress(curState.unpackingDone(filesCount))
        }

        // Use coroutineScope instead of runBlocking for structured concurrency
        coroutineScope {
            tmpFolder.listFiles()?.forEach { folder ->
                when (folder.name) {
                    "Video" -> launch {
                        videoFiles(settings, folder.listFiles() ?: emptyArray()) { 
                            reportProgress()
                        }
                    }

                    "Audio" -> launch {
                        audioFiles(settings, folder.listFiles() ?: emptyArray()) {
                            reportProgress()
                        }
                    }

                    "Images" -> launch {
                        imageFiles(this, settings, folder.listFiles() ?: emptyArray()) {
                            reportProgress()
                        }
                    }
                }
            }
        }

        curState = onProgress(curState.startPacking())

        val newSIGameFile = File(siGameFile.parent + "/compressed" + siGameFile.name)
        ZipUtil.pack(tmpFolder, newSIGameFile)
        curState = onProgress(curState.compressDone(newSIGameFile.absolutePath))
    } finally {
        // Always clean up temporary files, even on error
        tmpFolder.deleteRecursively()
    }
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

private fun unZipPack(siGameFile: File, targetFolder: File) {
    try {
        ZipUtil.unpack(siGameFile, targetFolder)
    } catch (e: org.zeroturnaround.zip.ZipException) {
        FileUtils.deleteDirectory(targetFolder)
        val contentXML = StringBuilder(String(ZipUtil.unpackEntry(siGameFile, "content.xml")))
        val zipStream = ZipInputStream(siGameFile.inputStream())
        // Increased buffer size from 4KB to 64KB for better performance
        val buffer = ByteArray(64 * 1024)
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

private fun videoFiles(set: SettingsSnapshot, files: Array<File>, progress: () -> Unit) {
    // Create encoder attributes once, reuse in loop
    val video = VideoAttributes()
    video.setCodec(set.videoCodec)

    val attrs = EncodingAttributes()
    attrs.setInputFormat("mp4")
    attrs.setOutputFormat("mp4")
    attrs.setEncodingThreads(set.cpuCores)
    attrs.setDecodingThreads(set.cpuCores)
    
    // Each coroutine has its own encoder instance for thread-safety
    val encoder = Encoder()

    for (file in files) {
        val typeCheck = file.extension
        if (typeCheck != "mp4") {
            progress()
            continue
        }
        val tmpFile = File(file.parent + "/tmp" + file.name)
        try {
            val mFile = MultimediaObject(file)
            val videoCurAttr = mFile.info.video
            if (videoCurAttr.bitRate <= set.videoBitrate) {
                progress()
                continue
            }

            video.setSize(videoCurAttr.size)
            video.setBitRate(set.videoBitrate)
            video.setFrameRate(videoCurAttr.frameRate.toInt())

            val audioCurAttr = mFile.info.audio
            val audio = AudioAttributes()
            audio.setCodec("libmp3lame")
            audio.setBitRate(minOf(set.videoBitrate, audioCurAttr.bitRate))
            audio.setChannels(set.audioChannels)
            audio.setSamplingRate(audioCurAttr.samplingRate)

            attrs.setVideoAttributes(video)
            attrs.setAudioAttributes(audio)

            encoder.encode(mFile, tmpFile, attrs)
            file.delete()
            tmpFile.renameTo(file)
        } catch (_: Exception) {
            tmpFile.delete()
        } finally {
            progress()
        }
    }
}

private fun audioFiles(set: SettingsSnapshot, files: Array<File>, progress: () -> Unit) {
    // Create encoder attributes once, reuse in loop
    val audio = AudioAttributes()
    audio.setCodec("libmp3lame")
    audio.setBitRate(set.audioBitrate)
    audio.setChannels(set.audioChannels)
    audio.setSamplingRate(set.samplingRate)

    val attrs = EncodingAttributes()
    attrs.setInputFormat("mp3")
    attrs.setOutputFormat("mp3")
    attrs.setAudioAttributes(audio)

    // Each coroutine has its own encoder instance for thread-safety
    val encoder = Encoder()

    for (file in files) {
        val typeCheck = file.extension

        if (typeCheck == "mp3") {
            val tmpFile = File(file.parent + "/tmp" + file.name)
            try {
                val mFile = MultimediaObject(file)
                encoder.encode(mFile, tmpFile, attrs)
                file.delete()
                tmpFile.renameTo(file)
            } catch (_: Exception) {
                tmpFile.delete()
            } finally {
                progress()
            }
        } else {
            // Log unsupported audio formats but still count them
            progress()
        }
    }
}

private fun imageFiles(corScope: CoroutineScope, set: SettingsSnapshot, files: Array<File>, progress: () -> Unit) {
    // Limit parallelism to available processors to prevent OOM and CPU contention
    val parallelism = Runtime.getRuntime().availableProcessors()
    val semaphore = Semaphore(parallelism)

    for (file in files) {
        // Skip GIF files - don't count them in progress
        if (file.extension == "gif") {
            continue
        }
        
        corScope.launch {
            semaphore.acquire()
            try {
                // Read image directly from file instead of loading entire file into memory first
                val originalImage = withContext(Dispatchers.Default) {
                    ImageIO.read(file)
                } ?: return@launch
                
                val currentDimension = minOf(set.imageResolution, maxOf(originalImage.height, originalImage.width))
                val resizedImage = withContext(Dispatchers.Default) {
                    Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, currentDimension)
                }

                withContext(Dispatchers.IO) {
                    ImageIO.write(resizedImage, "jpg", file)
                }
            } catch (_: Exception) {
                return@launch
            } finally {
                semaphore.release()
                progress()
            }
        }
    }
}
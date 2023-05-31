package compressor

import kotlinx.coroutines.*
import org.imgscalr.Scalr
import org.zeroturnaround.zip.ZipUtil
import org.zeroturnaround.zip.commons.FileUtils
import ws.schild.jave.Encoder
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

suspend fun compress(
    set: SettingsState,
    siGameFile: File,
    initialState: CompressState,
    onProgress: (CompressState) -> CompressState // consumes state and returns it
) = withContext(Dispatchers.IO) {
    var curState = initialState
    curState = onProgress(curState.unpackingStart())
    val tmpFolder = createTempDirectory(prefix = "temp").toFile()
    tmpFolder.deleteOnExit()
    unZipPack(siGameFile, tmpFolder)
    curState = onProgress(
        curState.unpackingDone(
            tmpFolder.listFiles()?.flatMap { it.listFiles()?.toList() ?: listOf() }?.count() ?: 1
        )
    )

    runBlocking(Dispatchers.IO) {
        tmpFolder.listFiles()?.forEach {
            when (it.name) {
                "Video" -> launch {
                    videoFiles(set, it!!.listFiles()!!) { curState = onProgress(curState.oneFileDone()) }
                }

                "Audio" -> launch {
                    audioFiles(set, it!!.listFiles()!!) { curState = onProgress(curState.oneFileDone()) }
                }

                "Images" -> launch {
                    imageFiles(this, set, it!!.listFiles()!!) { curState = onProgress(curState.oneFileDone()) }
                }
            }
        }
    }

    curState = onProgress(curState.startPacking())

    val newSIGameFile = File(siGameFile.parent + "/compressed" + siGameFile.name)
    ZipUtil.pack(tmpFolder, newSIGameFile)
    curState = onProgress(curState.compressDone(newSIGameFile.absolutePath))
    tmpFolder.deleteRecursively()
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

private fun videoFiles(set: SettingsState, files: Array<File>, progress: () -> Unit) {
    val video = VideoAttributes()
    video.setCodec(set.videoCodec.currentValue)

    val attrs = EncodingAttributes()
    attrs.setInputFormat("mp4")
    attrs.setOutputFormat("mp4")
    attrs.setEncodingThreads(set.cpuCores.currentValue)
    attrs.setDecodingThreads(set.cpuCores.currentValue)

    val encoder = Encoder()

    for (file in files) {
        val typeCheck = file.extension
        if (typeCheck != "mp4")
            continue
        val tmpFile = File(file.parent + "/tmp" + file.name)
        try {
            val mFile = MultimediaObject(file)
            val videoCurAttr = mFile.info.video
            if (videoCurAttr.bitRate <= set.videoBitrate.currentValue)
                continue

            video.setSize(videoCurAttr.size)
            video.setBitRate(set.videoBitrate.currentValue)
            video.setFrameRate(videoCurAttr.frameRate.toInt())

            val audioCurAttr = mFile.info.audio
            val audio = AudioAttributes()
            audio.setCodec("libmp3lame")
            audio.setBitRate(minOf(set.videoBitrate.currentValue, audioCurAttr.bitRate))
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

private fun audioFiles(set: SettingsState, files: Array<File>, progress: () -> Unit) {
    val audio = AudioAttributes()
    audio.setCodec("libmp3lame")
    audio.setBitRate(set.audioBitrate.currentValue)
    audio.setChannels(set.audioChannels)
    audio.setSamplingRate(set.samplingRate)

    val attrs = EncodingAttributes()
    attrs.setInputFormat("mp3")
    attrs.setOutputFormat("mp3")
    attrs.setAudioAttributes(audio)

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
            } finally {
                progress()
            }
        }
    }
}

private fun imageFiles(corScope: CoroutineScope, set: SettingsState, files: Array<File>, progress: () -> Unit) {
    for (file in files) {
        if (file.extension == "gif") {
            continue
        }
        corScope.launch {
            try {
                val originalImage = withContext(Dispatchers.IO) {
                    ImageIO.read(ByteArrayInputStream(file.readBytes()))
                } ?: return@launch
                val currentDimension =
                    minOf(set.imageResolution.currentValue, maxOf(originalImage.height, originalImage.width))
                val resizedImage = Scalr.resize(originalImage, Scalr.Method.ULTRA_QUALITY, currentDimension)

                withContext(Dispatchers.IO) {
                    ImageIO.write(resizedImage, "jpg", file)
                }
            } catch (e: Exception) {
                return@launch
            } finally {
                progress()
            }
        }
    }
}
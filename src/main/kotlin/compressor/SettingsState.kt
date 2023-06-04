package compressor

class SettingsState {
    class SettingProperty<T>(
        var currentValue: T,
        val values: List<String>,
        val toStringCurrentValue: (T) -> String = { it.toString() },
        val fromStringCurrentValue: (T) -> T = { it },
        val explanations: String = "",
        val toT: (String) -> T
    ) {
        fun setCurrentValue(value: String) {
            currentValue = fromStringCurrentValue(toT(value))
        }
        fun currentValueString() = toStringCurrentValue(currentValue)
    }

    val videoCodec = SettingProperty(
        "libx264", listOf("libx264", "h264_nvenc", "h264_amf", "h264_qsv"),
        explanations = "позволяет включить аппаратное ускорение для обработки видео на видеокарте\n" +
                "h264_nvenc - для nvidia\n" +
                "h264_amf - для AMD\n" +
                "h264_qsv - для Intel\n" +
                "libx264 - если не уверены, будет работать на процессоре",
        toT = { it }
    )
    val videoBitrate = SettingProperty(
        300 * 1024,
        (20..900 step 80).map { it.toString() }.toList(),
        { (it / 1024).toString() },
        { it * 1024 },
        "задает максимальный битрейт видео в килобитах",
        { it.toInt() }
    )
    val audioBitrate = SettingProperty(
        128 * 1000,
        (20..320 step 20).map { it.toString() }.toList(),
        { (it / 1000).toString() },
        { it * 1000 },
        "задает максимальный битрейт аудио в килобитах, так же используется для звука в видео",
        { it.toInt() }
    )
    val cpuCores = SettingProperty(
        Runtime.getRuntime().availableProcessors(),
        (1..Runtime.getRuntime().availableProcessors()).map { it.toString() }.toList(),
        explanations = "задает число ядер, которое будет использовать FFmpeg",
        toT = { it.toInt() }
    )
    val imageResolution = SettingProperty(
        1000,
        (300..2000 step 100).map { it.toString() }.toList(),
        explanations = "задает максимальное разрешение изображения в пикселях",
        toT = { it.toInt() }
    )

    val audioChannels = 1
    val samplingRate = 44100
}
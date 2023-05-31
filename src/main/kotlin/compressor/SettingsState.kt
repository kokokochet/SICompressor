package compressor

class SettingsState {
    class SettingProperty<T>(
        var currentValue: T,
        val values: List<String>,
        val toStringCurrentValue: (T) -> String = { it.toString() },
        val fromStringCurrentValue: (T) -> T = { it },
        val toT: (String) -> T
    ) {
        fun setCurrentValue(value: String) {
            currentValue = fromStringCurrentValue(toT(value))
        }
        fun currentValueString() = toStringCurrentValue(currentValue)
    }

    val videoCodec = SettingProperty("libx264", listOf("libx264", "h264_nvenc", "h264_amf"), toT = { it })
    val videoBitrate = SettingProperty(
        300 * 1024,
        (20..900 step 80).map { it.toString() }.toList(),
        { (it / 1024).toString() },
        { it * 1024 },
        { it.toInt() }
    )
    val audioBitrate = SettingProperty(
        128 * 1000,
        (20..320 step 20).map { it.toString() }.toList(),
        { (it / 1000).toString() },
        { it * 1000 },
        { it.toInt() }
    )
    val cpuCores = SettingProperty(
        Runtime.getRuntime().availableProcessors(),
        (1..Runtime.getRuntime().availableProcessors()).map { it.toString() }.toList(),
        toT = { it.toInt() }
    )
    val imageResolution = SettingProperty(
        1000,
        (300..2000 step 100).map { it.toString() }.toList(),
        toT = { it.toInt() }
    )

    val audioChannels = 1
    val samplingRate = 44100
}
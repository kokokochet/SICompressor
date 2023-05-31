package compressor

import kotlin.math.ceil

data class CompressState(
    val compressInProgress: Boolean = false,
    val progress: Float = 0f,
    val stringStatus: String = "",
    val previousFilePath: String? = null,
    val settings: SettingsState = SettingsState(),
    val processedCount: Int = 0,
    val filesCount: Int = 1,
    val funStatuses: Array<String> = STATUSES.shuffled().toTypedArray()
) {
    fun hasPreviousPath() = previousFilePath != null

    companion object {
        val STATUSES: List<String> = listOf(
            "идёт обработка", "шакализируем ваши мемы", "ищу аниме.....",
            "опять в финале кринжуха, может просто удалить?",
            "спасибо Владимир Хиль", "спасибо Владимир Хиль",
            "как сжать кота в мешке?"
        )
        private const val UNP_START = 0.1f
        private const val UNP_DONE = 0.1f
        const val START_PROGRESS = UNP_START + UNP_DONE
        const val COMPRESS_DONE_PROGRESS = 0.97f
    }

    internal fun unpackingStart() = this.copy(
        compressInProgress = true,
        stringStatus = "распаковка началась",
        progress = 0.1f
    )

    internal fun unpackingDone(filesCount: Int): CompressState {
        return this.copy(
            stringStatus = "распаковка началась",
            filesCount = filesCount
        )
    }

    internal fun startPacking() = this.copy(
        stringStatus = "пакуем пак",
        progress = COMPRESS_DONE_PROGRESS
    )

    internal fun compressDone(previousFilePath: String) = this.copy(
        stringStatus = "готово, удаляем временные файлы",
        progress = 1f,
        compressInProgress = false,
        previousFilePath = previousFilePath
    )

    internal fun oneFileDone(): CompressState {
        val newProcessedCount = processedCount + 1
        val newProgress = START_PROGRESS +
                (newProcessedCount.toFloat() / filesCount) * (COMPRESS_DONE_PROGRESS - START_PROGRESS)
        val newStringStatus = funStatuses[
            minOf(
                ceil((newProcessedCount.toFloat() / filesCount) * funStatuses.lastIndex).toInt(),
                funStatuses.lastIndex
            )
        ]
        return this.copy(
            processedCount = newProcessedCount,
            progress = newProgress,
            stringStatus = newStringStatus
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CompressState

        if (compressInProgress != other.compressInProgress) return false
        if (progress != other.progress) return false
        if (stringStatus != other.stringStatus) return false
        if (previousFilePath != other.previousFilePath) return false
        if (settings != other.settings) return false
        if (processedCount != other.processedCount) return false
        if (filesCount != other.filesCount) return false

        return true
    }

    override fun hashCode(): Int {
        var result = compressInProgress.hashCode()
        result = 31 * result + progress.hashCode()
        result = 31 * result + stringStatus.hashCode()
        result = 31 * result + (previousFilePath?.hashCode() ?: 0)
        result = 31 * result + settings.hashCode()
        result = 31 * result + processedCount
        result = 31 * result + filesCount
        return result
    }

}
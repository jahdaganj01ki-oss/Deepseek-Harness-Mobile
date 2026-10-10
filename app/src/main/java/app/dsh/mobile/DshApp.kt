package app.dsh.mobile

import android.app.Application
import app.dsh.mobile.engine.EngineSupervisor
import app.dsh.mobile.engine.ExtensionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class DshApp : Application() {

    /** 全局协程域：监督器生命周期独立于任何 Activity/Service */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 全局唯一监督器单例，Activity 与 Service 共享状态 */
    lateinit var supervisor: EngineSupervisor
        private set

    /** 全局唯一扩展管理器单例：扩展中心 UI 与 AI 通道（AgentBridge）共享下载任务状态 */
    val extensionManager: ExtensionManager by lazy { ExtensionManager(this) }

    override fun onCreate() {
        super.onCreate()
        // ⚠️ v1.2.110：全局崩溃捕获 —— 用户反馈「进程异常退出」（1.2.97 升级后 ×2 人，
        // Android 16 未复现，疑为 Android 17 兼容问题）。没有现场日志无法定位，
        // 现在任何 Java 崩溃都会落盘 `filesDir/crash-last.txt`（时间、线程、堆栈），
        // 用户或支持把该文件发回来即可定位。若该文件为空，说明是 native 崩溃
        // （tombstone 在系统日志里，需 logcat 提供）。
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                val f = java.io.File(filesDir, "crash-last.txt")
                f.writeText(
                    buildString {
                        appendLine("time: ${System.currentTimeMillis()}")
                        appendLine("thread: ${thread.name}")
                        appendLine(android.util.Log.getStackTraceString(throwable))
                    },
                )
                android.util.Log.e("DshApp", "crash captured -> ${f.absolutePath}")
            }
            prev?.uncaughtException(thread, throwable)
        }
        supervisor = EngineSupervisor(this)
    }

    companion object {
        /** 设置页 UI 偏好（页面缩放/横竖屏/试验性功能），与 SettingsActivity 共用 */
        const val PREFS_UI = "dsh_ui"

        /** 试验性功能开关（v1.2.95）：默认关闭，关闭时悬浮窗与相关设置入口都不出现 */
        const val KEY_EXPERIMENTAL = "experimental"

        /** 横竖屏偏好（字符串："portrait"/"landscape"/"auto"） */
        const val KEY_ORIENTATION = "orientation"

        /** 旧版横屏偏好（布尔）：用于迁阶 */
        const val KEY_LANDSCAPE_LEGACY = "landscape"

        const val ORIENTATION_PORTRAIT = "portrait"
        const val ORIENTATION_LANDSCAPE = "landscape"
        const val ORIENTATION_AUTO = "auto"

        const val DEFAULT_ORIENTATION = ORIENTATION_PORTRAIT

        val ORIENTATIONS = listOf(ORIENTATION_PORTRAIT, ORIENTATION_LANDSCAPE, ORIENTATION_AUTO)

        /** 读取横竖屏偏好（自动迁移旧版布尔键） */
        fun readOrientation(context: android.content.Context): String {
            val prefs = context.getSharedPreferences(PREFS_UI, android.content.Context.MODE_PRIVATE)
            val stored = prefs.getString(KEY_ORIENTATION, null)
            if (stored != null) return stored
            val oldLandscape = prefs.getBoolean(KEY_LANDSCAPE_LEGACY, false)
            val migrated = if (oldLandscape) ORIENTATION_LANDSCAPE else ORIENTATION_PORTRAIT
            prefs.edit().putString(KEY_ORIENTATION, migrated).apply()
            return migrated
        }

        /** 写入横竖屏偏好 */
        fun writeOrientation(context: android.content.Context, value: String) {
            context.getSharedPreferences(PREFS_UI, android.content.Context.MODE_PRIVATE)
                .edit().putString(KEY_ORIENTATION, value).apply()
        }

        /** 读取试验性开关（Activity/Service 通用；无 Context 依赖的静态读法） */
        fun experimentalOn(context: android.content.Context): Boolean =
            context.getSharedPreferences(PREFS_UI, android.content.Context.MODE_PRIVATE)
                .getBoolean(KEY_EXPERIMENTAL, false)
    }
}

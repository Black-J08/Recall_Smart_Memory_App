package ai.mlc.mlcllm

import org.apache.tvm.Device
import org.apache.tvm.Function
import org.apache.tvm.Module
import org.apache.tvm.TVMValue
import android.util.Log

class JSONFFIEngine {
    private val jsonFFIEngine: Module
    private val initBackgroundEngineFunc: Function
    private val reloadFunc: Function
    private val unloadFunc: Function
    private val resetFunc: Function
    private val chatCompletionFunc: Function
    private val abortFunc: Function
    private val getLastErrorFunc: Function
    private val runBackgroundLoopFunc: Function
    private val runBackgroundStreamBackLoopFunc: Function
    private val exitBackgroundLoopFunc: Function
    private var requestStreamCallback: Function? = null

    init {
        val createFunc = Function.getFunction("mlc.json_ffi.CreateJSONFFIEngine")
        checkNotNull(createFunc) { "Cannot find mlc.json_ffi.CreateJSONFFIEngine" }
        jsonFFIEngine = createFunc.invoke().asModule()
        initBackgroundEngineFunc = jsonFFIEngine.getFunction("init_background_engine")
        reloadFunc = jsonFFIEngine.getFunction("reload")
        unloadFunc = jsonFFIEngine.getFunction("unload")
        resetFunc = jsonFFIEngine.getFunction("reset")
        chatCompletionFunc = jsonFFIEngine.getFunction("chat_completion")
        abortFunc = jsonFFIEngine.getFunction("abort")
        getLastErrorFunc = jsonFFIEngine.getFunction("get_last_error")
        runBackgroundLoopFunc = jsonFFIEngine.getFunction("run_background_loop")
        runBackgroundStreamBackLoopFunc = jsonFFIEngine.getFunction("run_background_stream_back_loop")
        exitBackgroundLoopFunc = jsonFFIEngine.getFunction("exit_background_loop")
    }

    fun initBackgroundEngine(callback: KotlinFunction) {
        val device = Device.opencl()

        requestStreamCallback = Function.convertFunc { args ->
            val chatCompletionStreamResponsesJSONStr = args[0].asString()
            callback.invoke(chatCompletionStreamResponsesJSONStr)
            1 // Return value for TVM function
        }

        initBackgroundEngineFunc.pushArg(device.deviceType).pushArg(device.deviceId).pushArg(requestStreamCallback)
            .invoke()
    }

    fun reload(engineConfigJSONStr: String) {
        reloadFunc.pushArg(engineConfigJSONStr).invoke()
    }

    fun chatCompletion(requestJSONStr: String, requestId: String) {
        chatCompletionFunc.pushArg(requestJSONStr).pushArg(requestId).invoke()
    }

    fun runBackgroundLoop() {
        runBackgroundLoopFunc.invoke()
    }

    fun runBackgroundStreamBackLoop() {
        runBackgroundStreamBackLoopFunc.invoke()
    }

    fun exitBackgroundLoop() {
        exitBackgroundLoopFunc.invoke()
    }

    fun unload() {
        unloadFunc.invoke()
    }

    fun interface KotlinFunction {
        fun invoke(arg: String)
    }

    fun reset() {
        resetFunc.invoke()
    }
}

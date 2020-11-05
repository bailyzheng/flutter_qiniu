package com.weijietech.flutter.qiniu

import android.content.Context
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel

class FlutterQiniuPlugin: FlutterPlugin {
    val TAG = FlutterQiniuPlugin::class.java.simpleName

    private var channel : MethodChannel? = null
    private var eventChannel: EventChannel? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        setupChannel(binding.binaryMessenger, binding.applicationContext)
    }

    override fun onDetachedFromEngine(p0: FlutterPlugin.FlutterPluginBinding) {
        teardownChannel()
    }

    fun setupChannel(messenger: BinaryMessenger, context: Context) {
        channel = MethodChannel(messenger, "flutter_qiniu")
        eventChannel = EventChannel(messenger, "flutter_qiniu_event")
        val handler = MethodCallHandlerImpl(context)
        channel?.setMethodCallHandler(handler)
        eventChannel?.setStreamHandler(handler)
    }

    private fun teardownChannel() {
        channel?.setMethodCallHandler(null)
        channel = null
        eventChannel?.setStreamHandler(null)
        eventChannel = null
    }

}
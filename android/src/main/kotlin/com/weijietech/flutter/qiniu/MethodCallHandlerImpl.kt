package com.weijietech.flutter.qiniu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.qiniu.android.common.FixedZone
import com.qiniu.android.storage.Configuration
import com.qiniu.android.storage.UpCancellationSignal
import com.qiniu.android.storage.UpCompletionHandler
import com.qiniu.android.storage.UploadManager
import com.qiniu.android.storage.UploadOptions
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import java.lang.Exception

internal class MethodCallHandlerImpl(var context: Context) : MethodCallHandler, EventChannel.StreamHandler {
    val TAG = "QiniuImpl"
    private var cancelled = false
    private var receiver: BroadcastReceiver? = null

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "upload" -> upload(call, result)
            "cancelUpload" -> cancelUpload(result)
            else -> result.notImplemented()
        }
    }

    private fun upload(call: MethodCall, result: MethodChannel.Result) {
        cancelled = false
        val filepath = call.argument<String>("filepath")
        val key = call.argument<String>("key")
        val token = call.argument<String>("token")
//        Log.e(TAG, filepath)
        val config = Configuration.Builder()
                .chunkSize(512 * 1024) // 分片上传时，每片的大小。 默认256K
                .connectTimeout(10) // 链接超时。默认10秒
                .useHttps(true) // 是否使用https上传域名
                .responseTimeout(60) // 服务器响应超时。默认60秒
                .zone(FixedZone.zone0) // 设置区域，指定不同区域的上传域名、备用域名、备用IP。
                .build()
        // 重用uploadManager。一般地，只需要创建一个uploadManager对象
        val uploadManager = UploadManager(config)
        val upCompletionHandler = UpCompletionHandler { key, info, res -> //res包含hash、key等信息，具体字段取决于上传策略的设置
            if (info.isOK) {
                Log.i("qiniu", "Upload Success")
            } else {
                Log.i("qiniu", "Upload Fail: " + info.error)
                //如果失败，这里可以把info信息上报自己的服务器，便于后面分析上传错误原因
            }
            if (info.isOK) {
                try {
                    result.success(res["key"].toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                    result.success(null)
                }
            } else {
                result.success(null)
            }
            Log.i("qiniu", "$key,\r\n $info,\r\n $res")
        }
        val options = UploadOptions(null, null, false, { key, percent ->
            Log.i("qiniu", "$key: $percent")
            val i = Intent()
            i.action = UploadProgressFilter
            i.putExtra("percent", percent)
            context.sendBroadcast(i)
        }) { cancelled }
        uploadManager.put(filepath, key, token, upCompletionHandler, options)
    }

    private fun cancelUpload(result: MethodChannel.Result) {
        cancelled = true
        result.success(null)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        Log.v(TAG, "onListen")
        cancelled = false
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                Log.e(TAG, "rec")
                val percent = intent.getDoubleExtra("percent", 0.0)
                events?.success(percent)
            }
        }
        context.registerReceiver(receiver, IntentFilter(UploadProgressFilter))
    }

    override fun onCancel(arguments: Any?) {
        Log.e(TAG, "onCancel")
        cancelled = true
        context.unregisterReceiver(receiver)
    }

    companion object {
        private const val UploadProgressFilter = "UploadProgressFilter"
    }
}
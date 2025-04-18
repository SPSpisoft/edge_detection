package com.sample.edgedetection

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.sample.edgedetection.scan.ScanActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry

class EdgeDetectionPlugin : FlutterPlugin, ActivityAware {
    private var handler: EdgeDetectionHandler? = null
    private var channel: MethodChannel? = null

    override fun onAttachedToEngine(binding: FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "edge_detection")
        handler = EdgeDetectionHandler()
        channel?.setMethodCallHandler(handler)
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        channel?.setMethodCallHandler(null)
        channel = null
        handler = null
    }

    override fun onAttachedToActivity(activityPluginBinding: ActivityPluginBinding) {
        handler?.setActivityPluginBinding(activityPluginBinding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        handler?.setActivityPluginBinding(null)
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        handler?.setActivityPluginBinding(binding)
    }

    override fun onDetachedFromActivity() {
        handler?.setActivityPluginBinding(null)
    }
}

class EdgeDetectionHandler : MethodCallHandler, PluginRegistry.ActivityResultListener {
    private var activityPluginBinding: ActivityPluginBinding? = null
    private var result: Result? = null
    private var methodCall: MethodCall? = null

    companion object {
        const val INITIAL_BUNDLE = "initial_bundle"
        const val FROM_GALLERY = "from_gallery"
        const val SAVE_TO = "save_to"
        const val CAN_USE_GALLERY = "can_use_gallery"
        const val SCAN_TITLE = "scan_title"
        const val CROP_TITLE = "crop_title"
        const val CROP_BLACK_WHITE_TITLE = "crop_black_white_title"
        const val CROP_RESET_TITLE = "crop_reset_title"
        const val REQUEST_CODE = 100
        const val ERROR_CODE = 101
    }

    fun setActivityPluginBinding(activityPluginBinding: ActivityPluginBinding?) {
        this.activityPluginBinding?.removeActivityResultListener(this)
        activityPluginBinding?.addActivityResultListener(this)
        this.activityPluginBinding = activityPluginBinding
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        this.result = result
        this.methodCall = call

        when {
            getActivity() == null -> {
                result.error(
                    "no_activity",
                    "edge_detection plugin requires a foreground activity.",
                    null
                )
                return
            }
            call.method.equals("edge_detect") -> {
                openCameraActivity(call, result)
            }
            call.method.equals("edge_detect_gallery") -> {
                openGalleryActivity(call, result)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun getActivity(): Activity? {
        return activityPluginBinding?.activity
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    finishWithSuccess(true)
                }
                Activity.RESULT_CANCELED -> {
                    finishWithSuccess(false)
                }
                ERROR_CODE -> {
                    finishWithError(ERROR_CODE.toString(), data?.getStringExtra("RESULT") ?: "ERROR")
                }
            }
            return true
        }
        return false
    }

    private fun openCameraActivity(call: MethodCall, result: Result) {
        val activity = getActivity() ?: return
        val bundle = Bundle()
        bundle.putBoolean(FROM_GALLERY, false)
        bundle.putString(SAVE_TO, call.argument<String>(SAVE_TO))
        bundle.putBoolean(CAN_USE_GALLERY, call.argument<Boolean>(CAN_USE_GALLERY) ?: false)
        bundle.putString(SCAN_TITLE, call.argument<String>(SCAN_TITLE))
        bundle.putString(CROP_TITLE, call.argument<String>(CROP_TITLE))
        bundle.putString(CROP_BLACK_WHITE_TITLE, call.argument<String>(CROP_BLACK_WHITE_TITLE))
        bundle.putString(CROP_RESET_TITLE, call.argument<String>(CROP_RESET_TITLE))

        val intent = Intent(activity, ScanActivity::class.java)
        intent.putExtra(INITIAL_BUNDLE, bundle)
        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    private fun openGalleryActivity(call: MethodCall, result: Result) {
        val activity = getActivity() ?: return
        val bundle = Bundle()
        bundle.putBoolean(FROM_GALLERY, true)
        bundle.putString(SAVE_TO, call.argument<String>(SAVE_TO))
        bundle.putBoolean(CAN_USE_GALLERY, call.argument<Boolean>(CAN_USE_GALLERY) ?: false)
        bundle.putString(SCAN_TITLE, call.argument<String>(SCAN_TITLE))
        bundle.putString(CROP_TITLE, call.argument<String>(CROP_TITLE))
        bundle.putString(CROP_BLACK_WHITE_TITLE, call.argument<String>(CROP_BLACK_WHITE_TITLE))
        bundle.putString(CROP_RESET_TITLE, call.argument<String>(CROP_RESET_TITLE))

        val intent = Intent(activity, ScanActivity::class.java)
        intent.putExtra(INITIAL_BUNDLE, bundle)
        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    private fun finishWithSuccess(success: Boolean) {
        result?.success(success)
        result = null
        methodCall = null
    }

    private fun finishWithError(errorCode: String, errorMessage: String) {
        result?.error(errorCode, errorMessage, null)
        result = null
        methodCall = null
    }
}

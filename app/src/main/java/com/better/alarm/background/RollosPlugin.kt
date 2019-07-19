package com.better.alarm.background

import com.better.alarm.logger.Logger
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.disposables.Disposables
import okhttp3.*
import java.io.IOException


class RollosPlugin(val logger: Logger) : AlertPlugin {
    override fun go(alarm: PluginAlarmData, prealarm: Boolean, targetVolume: Observable<TargetVolume>): Disposable {

        val value = if (prealarm) "80" else "40"

        val request = Request.Builder()
                .url("http://192.168.0.200/deviceajax.do?cid=9&did=1010000&goto=$value&command=0")
                .build()

        OkHttpClient().newCall(request).enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        logger.e("Response: $e", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        logger.d("Response: $response")
                    }
                }
        )
        return Disposables.empty()
    }
}

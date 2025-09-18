package com.t1h2h0.videos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.t1h2h0.videos.utils.PipeDownloader
import org.schabi.newpipe.extractor.NewPipe
import javax.inject.Inject


@HiltAndroidApp
class MyApplication : Application() {

    @Inject
    lateinit var pipeDownloader: PipeDownloader

    override fun onCreate() {
        super.onCreate()


        NewPipe.init(pipeDownloader)
    }
}
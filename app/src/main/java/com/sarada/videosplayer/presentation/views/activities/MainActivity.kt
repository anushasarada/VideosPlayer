package com.sarada.videosplayer.presentation.views.activities

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.analytics.AnalyticsListener
import com.google.android.exoplayer2.analytics.PlaybackStatsListener
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.DefaultTimeBar
import com.google.android.exoplayer2.ui.TimeBar
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.util.Util
import com.sarada.videosplayer.databinding.ActivityMainBinding
import com.sarada.videosplayer.models.Videos
import com.sarada.videosplayer.presentation.adapters.VideosAdapter
import com.sarada.videosplayer.presentation.viewmodels.MainViewModel
import java.util.concurrent.TimeUnit


private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }

    private var videoPlayer: ExoPlayer? = null

    private var videoUrl = "https://player.vimeo.com/external/269971860.hd.mp4?s=eae965838585cc8342bb5d5253d06a52b2415570&profile_id=174&oauth2_token_id=57447761"

    private var playWhenReady: Boolean = true
    private var currentWindow: Int = 0
    private var playbackPosition: Long = 0

    private val playbackStateListener: Player.Listener = playbackStateListener()
    //pexels api key
    //563492ad6f917000010000017a2121d95b75469aaaffa12cf80039a2

    private var videosAdapter: VideosAdapter? = null
    private var isInFullScreen: Boolean = false

    private val POPULAR_VIDEOS: Int = 0
    private val MOST_VIEWED: Int = 1
    private val LAST_VIEWED: Int = 2

    private var spinnerList = arrayListOf("Popular videos", "Most Viewed", "Last Viewed")

    private val updateProgressAction = Runnable { updateProgress() }
    private var mHandler: Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //For full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        //To display video around the notch as well
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        //setTheme(R.style.mainActivityTheme)
        setContentView(binding.root)

        //For Immersive mode
        //Hide status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        //Bottom buttons
        WindowInsetsControllerCompat(window, binding.root).let{ controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            //Show bottom buttons upon swipe
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        //Building here is taking more than 3s to load video
        /*videoPlayer = ExoPlayer.Builder(this).build()
        binding.pvVideo.player = videoPlayer*/

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerList)
        binding.spSortingOrder.adapter = spinnerAdapter

        binding.spSortingOrder.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                updateVideosList(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        
        videosAdapter = VideosAdapter(this, onVideoItemClick)

        binding.rvVideos.apply {
            itemAnimator = DefaultItemAnimator()
            adapter = videosAdapter
        }

        viewModel.videosResponse.observe(this) { videoResponse ->

            if (videoResponse != null && videoResponse.videos.isNotEmpty()) {

                updateRecyclerView(videoResponse.videos)
            }
        }
    }

    private fun updateRecyclerView(videos: List<Videos>) {
        videosAdapter?.submitList(videos)
        initializePlayer()
        binding.rvVideos.apply {
            smoothScrollToPosition(0)
        }
    }

    private fun updateVideosList(position: Int) {
        when(position){
            POPULAR_VIDEOS -> {
                viewModel.videosResponse.value?.videos?.let { updateRecyclerView(it) }
            }
            MOST_VIEWED -> {
                viewModel.initHashMapWithMissingVideoIds()
                viewModel.updateMostViewedVideos()
                viewModel.getMostViewedVideos()?.let { updateRecyclerView(it) }
            }
            LAST_VIEWED -> {

            }
        }
    }

    private fun changeVideo(isNext: Boolean = true){
        if(isNext) viewModel.setPosition()
        else viewModel.setPosition(false)
        binding.rvVideos.smoothScrollToPosition(viewModel.getCurrentVideoIndex())
        initializePlayer()
    }

    private var onVideoItemClick = { position: Int ->
        viewModel.setCurrentVideoIndex(position)
        viewModel.updateHashMap()
        initializePlayer()
    }

    private fun playInFullScreen(enable: Boolean) {
        if (enable) {
            binding.pvVideo.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
            videoPlayer?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
            isInFullScreen = true
            binding.pvVideo.findViewById<ImageButton>(R.id.exo_fullscreen).setImageResource(R.drawable.exo_ic_fullscreen_exit)
        } else {
            binding.pvVideo.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            videoPlayer?.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            isInFullScreen = false
            binding.pvVideo.findViewById<ImageButton>(R.id.exo_fullscreen).setImageResource(R.drawable.exo_styled_controls_fullscreen_enter)
        }
    }

    private fun initializePlayer() {

        /*binding.pvVideo.player = ExoPlayer.Builder(this)
            .setTrackSelector(trackSelector)
            .build()
            .also { exoPlayer ->
                binding.pvVideo.player = exoPlayer
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(sampleUrl))
                    .setMimeType(MimeTypes.APPLICATION_SS)
                    .build()
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(currentWindow, playbackPosition)
                exoPlayer.addListener(playbackStateListener)
                exoPlayer.prepare()
            }*/

        initializeVideoUrl()

        videoPlayer = ExoPlayer.Builder(this).build()
        binding.pvVideo.player = videoPlayer

        resetVideoStats()

        buildMediaSource().let {
            videoPlayer?.setMediaSource(it)
            videoPlayer?.prepare()
            videoPlayer?.playWhenReady = true
        }
        videoPlayer?.addAnalyticsListener(playbackStatsListener)


        mHandler = Handler()
        mHandler?.post(updateProgressAction)

        setUiListeners()
    }

    private fun initializeVideoUrl() {
        viewModel.videosResponse.value?.let { it ->
            videoUrl = it.videos[viewModel.getCurrentVideoIndex()].videoFiles[0].link.toString()
            Log.d("VideoUrl", videoUrl)
        }
        viewModel.updateHashMap()
    }

    private fun resetVideoStats() {

        viewModel.resetVideoStats()
        binding.tvVideoProgressPercentage.text = "0%"
    }

    private val analyticsListener: AnalyticsListener = object : AnalyticsListener {
        private var initTime = 0L

        override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {

            if(isPlaying) {
                if(initTime != 0L) viewModel.pauseTime += System.currentTimeMillis() - initTime
                initTime = System.currentTimeMillis()
            } else {
                if(initTime != 0L) viewModel.playTime += System.currentTimeMillis() - initTime
                initTime = System.currentTimeMillis()
                viewModel.pressedPaused++
            }

            val totalVideoDuration = viewModel.calculateVideoStats(isPlaying)
            //binding.tvVideoProgressPercentage.text = totalVideoDuration.toString()

            super.onIsPlayingChanged(eventTime, isPlaying)
        }
    }

    private val playbackStatsListener = PlaybackStatsListener(true ) { eventTime, playbackStats -> }

    private fun updateProgress(){

        var totalTime = viewModel.videosResponse.value?.videos?.get(viewModel.getCurrentVideoIndex())?.duration?.times(1000)?.toFloat()
        var playTime = playbackStatsListener.playbackStats?.totalPlayTimeMs?.toFloat()
        var per = totalTime?.let { playTime?.div(it) }
        per = per?.times(100)
        var str = "%.0f".format(per)
        binding.tvVideoProgressPercentage.text = str+"%"

        val delayMs: Long = TimeUnit.SECONDS.toMillis(0)
        mHandler!!.postDelayed(updateProgressAction, delayMs)
    }

    private fun setUiListeners(){

        binding.pvVideo.findViewById<DefaultTimeBar>(R.id.exo_progress)
            .addListener(object : TimeBar.OnScrubListener {
                override fun onScrubStart(timeBar: TimeBar, position: Long) {

                }

                override fun onScrubMove(timeBar: TimeBar, position: Long) {
                    videoPlayer?.seekTo(position)
                }

                override fun onScrubStop(timeBar: TimeBar, position: Long, canceled: Boolean) {

                }

            })

        binding.pvVideo.findViewById<ImageButton>(R.id.exo_fullscreen).apply {
            visibility = View.VISIBLE
            setOnClickListener {
                playInFullScreen(!isInFullScreen)
            }
        }

        binding.pvVideo.findViewById<ImageButton>(R.id.exo_prev).apply {
            setOnClickListener {
                changeVideo(false)
            }
        }

        binding.pvVideo.findViewById<ImageButton>(R.id.exo_next).apply {
            setOnClickListener {
                changeVideo()
            }
        }

    }

    private fun buildMediaSource(): MediaSource {
        val dataSourceFactory = DefaultDataSource.Factory(this)
        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(Uri.parse(videoUrl)))
    }

    private fun playbackStateListener() = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            val stateString: String = when (playbackState) {
                ExoPlayer.STATE_IDLE -> "ExoPlayer.STATE_IDLE      -"
                ExoPlayer.STATE_BUFFERING -> "ExoPlayer.STATE_BUFFERING -"
                ExoPlayer.STATE_READY -> "ExoPlayer.STATE_READY     -"
                ExoPlayer.STATE_ENDED -> "ExoPlayer.STATE_ENDED     -"
                else -> "UNKNOWN_STATE             -"
            }
            Log.d(TAG, "changed state to $stateString")
            if(playbackState == ExoPlayer.STATE_READY){
                binding.tvVideoProgressPercentage.text = playbackStatsListener.playbackStats?.totalPlayTimeMs.toString()
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        if (Util.SDK_INT > 23) {
            initializePlayer()
        }
    }

    public override fun onResume() {
        super.onResume()
        hideSystemUi()
        if ((Util.SDK_INT <= 23 || videoPlayer == null)) {
            initializePlayer()
        }
    }

    public override fun onPause() {
        super.onPause()
        if (Util.SDK_INT <= 23) {
            releasePlayer()
        }
    }

    public override fun onStop() {
        super.onStop()
        if (Util.SDK_INT > 23) {
            releasePlayer()
        }
    }

    private fun releasePlayer() {
        videoPlayer?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            currentWindow = exoPlayer.currentMediaItemIndex
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.removeListener(playbackStateListener)
            exoPlayer.removeAnalyticsListener(playbackStatsListener)
            exoPlayer.release()
        }
        videoPlayer = null
    }

    @SuppressLint("InlinedApi")
    private fun hideSystemUi() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.pvVideo).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
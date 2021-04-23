package com.shankware.annoisli

import android.animation.FloatEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlin.math.pow

class GridItem(
    private val mContext: Context,
    val drawableId: Int,
    private val mSoundId: Int
) : View.OnClickListener, OnSeekBarChangeListener {
    private var mLastVolume = 0.5f
    private var mVolume = 0.5f
    private var mIsEnabled = false
    private var mIsMuted = false
    private var mImageView: ImageView? = null
    private var mSeekBar: SeekBar? = null

    private enum class MediaPlayerState {
        IDLE, PREPARED, STARTED, STOPPED, END
    }

    private var mMediaPlayerState = MediaPlayerState.IDLE
    private lateinit var mMediaPlayer: MediaPlayer

    var view: View? = null
        set(convertView) {
            if (view != null) {
                return
            }
            field = convertView
            mImageView = convertView?.findViewById<View>(R.id.grid_item_image) as ImageView
            mImageView!!.setImageResource(drawableId)
            mImageView!!.setOnClickListener(this)
            mSeekBar = convertView.findViewById<View>(R.id.grid_item_volume) as SeekBar
            mSeekBar!!.setOnSeekBarChangeListener(this)
            animate()
        }


    private fun animate() {
        if (mImageView == null) {
            return
        }
        val iconAnimator = ValueAnimator.ofObject(
            FloatEvaluator(),
            mImageView!!.alpha, if (mIsEnabled) 1.0f else 0.5f
        )
        iconAnimator.duration = 200
        iconAnimator.addUpdateListener { animator ->
            mImageView!!.alpha = animator.animatedValue as Float
        }
        iconAnimator.start()
        val volumeAnimator = ValueAnimator.ofObject(
            FloatEvaluator(),
            mSeekBar!!.alpha, if (mIsEnabled) 1.0f else 0f
        )
        volumeAnimator.duration = 200
        volumeAnimator.addUpdateListener { animator ->
            val newAlpha = animator.animatedValue as Float
            mSeekBar!!.alpha = newAlpha
            if (newAlpha < 0.2f && mSeekBar!!.isEnabled != mIsEnabled) {
                mSeekBar!!.isEnabled = mIsEnabled
            }
        }
        volumeAnimator.start()
        Borker.bork()
    }

    private fun toggleSound() {
        var progress = 50
        if (mSeekBar != null) {
            progress = mSeekBar!!.progress
        }
        if (mIsEnabled) {
            mVolume = scaleVolume(progress)
            mMediaPlayer = MediaPlayer.create(mContext, mSoundId)
            mMediaPlayerState = MediaPlayerState.PREPARED
            mMediaPlayer.setVolume(mVolume, mVolume)
            mMediaPlayer.setLooping(true)
            mMediaPlayer.start()
            mMediaPlayerState = MediaPlayerState.STARTED
        } else if (mMediaPlayerState == MediaPlayerState.STARTED && mMediaPlayer.isPlaying) {
            mMediaPlayer.stop()
            mMediaPlayerState = MediaPlayerState.STOPPED
            mMediaPlayer.release()
            mMediaPlayerState = MediaPlayerState.END
        } else {
            Log.e(TAG + name, "WTF: " + mMediaPlayerState.name)
            FirebaseCrashlytics.getInstance()
                .log("Weird MediaPlayerState: ${mMediaPlayerState.name}")
        }
        Borker.bork()
    }

    fun setEnabled(enabled: Boolean) {
        if (mIsEnabled != enabled) {
            mIsEnabled = enabled
            mIsMuted = !enabled
            animate()
            toggleSound()
        }
        FirebaseCrashlytics.getInstance().log("GridItem $name enabled: $enabled")
        Borker.bork()
    }

    fun setMuted(muted: Boolean) {
        if (mIsMuted == muted) {
            return
        }
        mIsMuted = muted
        FirebaseCrashlytics.getInstance().log("Setting mute state for GridItem $name: $muted")
        if (muted) {
            mLastVolume = mVolume
            mVolume = 0f
        } else {
            mVolume = mLastVolume
        }
        try {
            if (mMediaPlayerState == MediaPlayerState.STARTED && mMediaPlayer.isPlaying) {
                mMediaPlayer.setVolume(mVolume, mVolume)
            }
        } catch (ex: IllegalStateException) {
            Log.e("GridItem $name", ex.message!!)
            FirebaseCrashlytics.getInstance().recordException(ex)
        }
        if (mSeekBar != null) {
            mSeekBar!!.progress = progressFromCurrentVolume
        }
        Borker.bork()
    }

    fun setVolume(volume: Float) {
        mVolume = volume
        FirebaseCrashlytics.getInstance().log("Setting volume for GridItem $name: $volume")
        if (mSeekBar != null) {
            mSeekBar!!.progress = progressFromCurrentVolume
        }
        Borker.bork()
    }

    fun toggle() {
        mIsEnabled = !mIsEnabled
        mIsMuted = !mIsEnabled
        FirebaseCrashlytics.getInstance().log("Toggling GridItem $name: $mIsEnabled")
        animate()
        toggleSound()
        Borker.bork()
    }

    private fun scaleVolume(volume: Int): Float = (volume.toDouble() / 100.0).pow(1.2).toFloat()

    private val progressFromCurrentVolume: Int
        get() = (100 * mVolume.toDouble().pow(10.0 / 12.0)).toInt()

    private val name: String
        get() {
            val value = TypedValue()
            mContext.resources.getValue(mSoundId, value, true)
            return value.string.toString()
        }

    override fun onClick(view: View) {
        FirebaseCrashlytics.getInstance().log("Clicked on GridItem $name")
        setEnabled(mImageView!!.alpha <= 0.5f)
        Borker.bork()
    }

    override fun onProgressChanged(seekBar: SeekBar, volume: Int, b: Boolean) {
        FirebaseCrashlytics.getInstance().log("Volume for GridItem $name changed: $volume")
        mVolume = scaleVolume(volume)
        try {
            if (mMediaPlayerState == MediaPlayerState.STARTED && mMediaPlayer.isPlaying) {
                mMediaPlayer.setVolume(mVolume, mVolume)
            }
        } catch (ex: IllegalStateException) {
            Log.e("GridItem", name)
            FirebaseCrashlytics.getInstance().recordException(ex)
        }
        Borker.bork()
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {}

    override fun onStopTrackingTouch(seekBar: SeekBar) {}

    companion object {
        private const val TAG = "GridItem"
    }
}
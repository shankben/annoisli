package com.shankware.annoisli

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.ToggleButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.DrawableCompat
import com.amplifyframework.analytics.pinpoint.AWSPinpointAnalyticsPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase


class MainActivity : AppCompatActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var mHeader: View
    private lateinit var mHeaderGridView: HeaderGridView
    private lateinit var mGridItems: Array<GridItem>

    private fun getBackgroundColor(view: View): Int {
        var color = Color.TRANSPARENT
        val background = view.background
        if (background is ColorDrawable) {
            color = background.color
        }
        return color
    }

    private val mRotateHueHandler = Handler(Looper.getMainLooper())
    private val mRotateHueRunnable: Runnable = object : Runnable {
        override fun run() {
            val rootView = findViewById<View>(R.id.grid_view).rootView
            val currentHsl = FloatArray(3)
            val colorFrom = getBackgroundColor(rootView)
            ColorUtils.colorToHSL(colorFrom, currentHsl)
            var hue = currentHsl[0]
            hue += 37f
            if (hue > 360f) {
                hue = 0f
            }
            val colorTo = ColorUtils.HSLToColor(floatArrayOf(hue, 0.5f, 0.5f))
            val colorAnimation = ValueAnimator.ofObject(ArgbEvaluator(), colorFrom, colorTo)

            FirebaseCrashlytics.getInstance().log("Rotating colors: $colorFrom -> $colorTo")

            colorAnimation.duration = 5000
            colorAnimation.addUpdateListener { animator ->
                FirebaseCrashlytics.getInstance()
                    .log("Animating background color: ${animator.animatedValue}")
                rootView.setBackgroundColor(animator.animatedValue as Int)
            }
            colorAnimation.start()
            mRotateHueHandler.postDelayed(this, HUE_ROTATION_DELAY_MS.toLong())

            Borker.bork()
        }
    }

    private fun setupButtons() {
        val muteButton = mHeader.findViewById<View>(R.id.mute_button) as ToggleButton
        DrawableCompat.setTint(DrawableCompat.wrap(muteButton.background),
            Color.parseColor("#6AFFFFFF"))
        val randomButton = mHeader.findViewById<View>(R.id.button_left) as Button
        val counterProdButton = mHeader.findViewById<View>(R.id.button_middle) as ToggleButton
        val misophoniaButton = mHeader.findViewById<View>(R.id.button_right) as ToggleButton

        muteButton.setOnCheckedChangeListener { _, isChecked ->
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, R.id.mute_button.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, "mute_button")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "button")
                param(FirebaseAnalytics.Param.VALUE, isChecked.toString())
            }
            if (isChecked) {
                muteButton.setBackgroundResource(R.drawable.ic_volume_mute_black_24dp)
                DrawableCompat.setTint(DrawableCompat.wrap(muteButton.background),
                    Color.parseColor("#EFFFFFFF"))
            } else {
                muteButton.setBackgroundResource(R.drawable.ic_volume_up_black_24dp)
                DrawableCompat.setTint(DrawableCompat.wrap(muteButton.background),
                    Color.parseColor("#6AFFFFFF"))
            }
            mGridItems.forEach { it.setMuted(isChecked) }
        }

        randomButton.setOnClickListener {
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, R.id.button_left.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, "random_button")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "button")
            }
            counterProdButton.isChecked = false
            misophoniaButton.isChecked = false

            mGridItems.filter { Math.random() < 0.5 }.forEach {
                it.toggle()
                it.setVolume(Math.random().toFloat())
            }
        }

        counterProdButton.setOnCheckedChangeListener { _, isChecked ->
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, R.id.button_middle.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, "counter_prod_button")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "button")
            }
            misophoniaButton.isChecked = false
            counterProdButton.isChecked = isChecked
            for (gridItem in mGridItems) {
                when (gridItem.drawableId) {
                    R.drawable.baby -> {
                        gridItem.setEnabled(isChecked)
                        gridItem.setVolume(1.0f)
                    }
                    R.drawable.mosquito -> {
                        gridItem.setEnabled(isChecked)
                        gridItem.setVolume(0.3f)
                    }
                    R.drawable.tinnitus -> {
                        gridItem.setEnabled(isChecked)
                        gridItem.setVolume(0.75f)
                    }
                    R.drawable.light -> {
                        gridItem.setEnabled(isChecked)
                        gridItem.setVolume(0.3f)
                    }
                    else -> gridItem.setEnabled(false)
                }
            }
        }

        misophoniaButton.setOnCheckedChangeListener { _, isChecked ->
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, R.id.button_right.toString())
                param(FirebaseAnalytics.Param.ITEM_NAME, "misophonia_button")
                param(FirebaseAnalytics.Param.CONTENT_TYPE, "button")
            }
            counterProdButton.isChecked = false
            misophoniaButton.isChecked = isChecked
            for (gridItem in mGridItems) {
                when (gridItem.drawableId) {
                    R.drawable.nose -> {
                        gridItem.setEnabled(isChecked)
                        gridItem.setVolume(0.7f)
                    }
                    R.drawable.chalk -> {
                        gridItem.setEnabled(isChecked)
                        gridItem.setVolume(0.2f)
                    }
                    R.drawable.spaghetti -> {
                        gridItem.setEnabled(isChecked)
                        gridItem.setVolume(1.0f)
                    }
                    R.drawable.chips -> {
                        gridItem.setEnabled(isChecked)
                        gridItem.setVolume(0.5f)
                    }
                    else -> gridItem.setEnabled(false)
                }
            }
        }
    }

    private fun setFont() {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, R.id.button_middle.toString())
            param(FirebaseAnalytics.Param.ITEM_NAME, "counter_prod_button")
            param(FirebaseAnalytics.Param.CONTENT_TYPE, "button")
        }
        val fontPath = "fonts/Raleway-Regular.ttf"
        FirebaseCrashlytics.getInstance().log("Loading custom font: $fontPath")
        val raleway = Typeface.createFromAsset(assets, fontPath)
        val uiElementResourceIds = intArrayOf(
            R.id.header_title,
            R.id.header_subtitle,
            R.id.button_left,
            R.id.button_middle,
            R.id.button_right
        )
        for (buttonId in uiElementResourceIds) {
            FirebaseCrashlytics.getInstance().log("Setting custom font on View: $buttonId")
            (mHeader.findViewById<View>(buttonId) as TextView).typeface = raleway
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        firebaseAnalytics = Firebase.analytics

        Amplify.addPlugin(AWSCognitoAuthPlugin())
        Amplify.addPlugin(AWSPinpointAnalyticsPlugin(this.application))
        Amplify.configure(applicationContext)

        setContentView(R.layout.activity_main)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        }
//        else {
//            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
//            actionBar?.hide()
//        }

        mHeaderGridView = findViewById<View>(R.id.grid_view) as HeaderGridView
        mHeader = LayoutInflater.from(this).inflate(R.layout.header, mHeaderGridView, false)
        mHeaderGridView.addHeaderView(mHeader)
        setupButtons()

        mGridItems = arrayOf(
            GridItem(this, R.drawable.baby, R.raw.baby),
            GridItem(this, R.drawable.car, R.raw.car),
            GridItem(this, R.drawable.chalk, R.raw.chalk),
            GridItem(this, R.drawable.chips, R.raw.chips),
            GridItem(this, R.drawable.fan, R.raw.fan),
            GridItem(this, R.drawable.light, R.raw.light),
            GridItem(this, R.drawable.mosquito, R.raw.mosquito),
            GridItem(this, R.drawable.nose, R.raw.nose),
            GridItem(this, R.drawable.spaghetti, R.raw.spaghetti),
            GridItem(this, R.drawable.tinnitus, R.raw.tinnitus)
        )

        mHeaderGridView.adapter = GridAdapter(this, mGridItems)

        setFont()

        val initialColor = ColorUtils.HSLToColor(floatArrayOf(0f, 0.5f, 0.5f))
        mHeaderGridView.rootView?.setBackgroundColor(initialColor)
        mRotateHueHandler.postDelayed(mRotateHueRunnable, HUE_ROTATION_DELAY_MS.toLong())
    }

    public override fun onPause() {
        mGridItems.forEach { it.setMuted(true) }
        super.onPause()
    }

    public override fun onResume() {
        super.onResume()
        val muteButton = mHeader.findViewById<View>(R.id.mute_button) as ToggleButton
        if (!muteButton.isChecked) {
            mGridItems.forEach { it.setMuted(false) }
        }
    }

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private const val HUE_ROTATION_DELAY_MS = 30000
    }
}
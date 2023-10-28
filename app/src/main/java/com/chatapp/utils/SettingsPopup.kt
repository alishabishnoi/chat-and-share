package com.chatapp.utils

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.ColorInt
import com.amulyakhare.textdrawable.TextDrawable
import com.chatapp.R
import java.util.Locale

class SettingsPopup(context: Context) : PopupWindow() {

    enum class Option {
        PROFILE,
        IMAGES,
        SETTINGS,
        ABOUT;
    }

    private val appearingAnimationDuration = 200L

    @ColorInt
    private var color = Color.GRAY
    private var userName = ""
    private var clickListener: ((Option) -> (Unit))? = null

    private var rootView: View
    private var container: View
    private var avatar: ImageView
    private var userNameLabel: TextView

    private var isDismissing: Boolean = false

    fun populateData(userName: String, @ColorInt color: Int) {
        this.userName = userName
        this.color = color
    }

    fun setOnOptionClickListener(clickListener: (Option) -> (Unit)) {
        this.clickListener = clickListener
    }

    init {

        @SuppressLint("InflateParams")
        val inflater=context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        rootView = inflater.inflate(R.layout.popup_settings, null)
        container = rootView.findViewById(R.id.fl_container)
        avatar = rootView.findViewById(R.id.iv_avatar)
        userNameLabel = rootView.findViewById(R.id.tv_username)

        rootView.findViewById<View>(R.id.ll_user_profile_container).setOnClickListener {
            dismiss()
            clickListener?.invoke(Option.PROFILE)
        }

        rootView.findViewById<View>(R.id.ll_images_button).setOnClickListener {
            dismiss()
            clickListener?.invoke(Option.IMAGES)
        }

        rootView.findViewById<View>(R.id.ll_settings_button).setOnClickListener {
            dismiss()
            clickListener?.invoke(Option.SETTINGS)
        }

        rootView.findViewById<View>(R.id.ll_about_button).setOnClickListener {
            dismiss()
            clickListener?.invoke(Option.ABOUT)
        }

        contentView = rootView
    }

    fun show(anchor: View) {

        prepare()

        populateUi()

        val xPosition: Int
        val yPosition: Int

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)

        val anchorRect = Rect(location[0], location[1],
            location[0] + anchor.width, location[1] + anchor.height)

        xPosition = anchorRect.right + rootView.measuredWidth
        yPosition = anchorRect.top

        showAtLocation(anchor, Gravity.NO_GRAVITY, xPosition, yPosition)

        container.post {
            if (container.isAttachedToWindow) {
                val animator = ViewAnimationUtils.createCircularReveal(container,
                    container.width, 0, 0f, container.measuredWidth.toFloat())
                container.visibility = View.VISIBLE
                animator.duration = appearingAnimationDuration
                animator.start()
            }
        }
    }

    override fun dismiss() {

        if (true && !isDismissing) {
            val animator = ViewAnimationUtils.createCircularReveal(container,
                container.width, 0, container.measuredWidth.toFloat(), 0f)
            container.visibility = View.VISIBLE
            animator.addListener(object : EmptyAnimatorListener() {

                override fun onAnimationStart(p0: Animator) {
                    isDismissing = true
                }

                override fun onAnimationEnd(p0: Animator) {
                    actualDismiss()
                }
            })
            animator.duration = appearingAnimationDuration
            animator.start()
        } else {
            actualDismiss()
        }
    }

    private fun actualDismiss() {
        isDismissing = false
        super.dismiss()
    }

    private fun prepare() {
        setBackgroundDrawable(ColorDrawable())
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        isTouchable = true
        isFocusable = true
        isOutsideTouchable = true
    }

    private fun populateUi() {
        val drawable = TextDrawable.builder().buildRound(userName.getFirstLetter(), color)
        avatar.setImageDrawable(drawable)
        userNameLabel.text = userName
    }

    private fun String.getFirstLetter() = if (this.isEmpty()) "?" else this[0].toString().toUpperCase(
        Locale.ROOT)
}
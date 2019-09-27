package io.legado.app.ui.book.read

import android.content.Context
import android.util.AttributeSet
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.SeekBar
import androidx.core.view.isVisible
import io.legado.app.App
import io.legado.app.R
import io.legado.app.constant.Bus
import io.legado.app.lib.theme.accentColor
import io.legado.app.lib.theme.buttonDisabledColor
import io.legado.app.utils.*
import kotlinx.android.synthetic.main.view_read_menu.view.*
import org.jetbrains.anko.sdk27.listeners.onClick
import org.jetbrains.anko.sdk27.listeners.onLongClick

class ReadMenu : FrameLayout {
    var cnaShowMenu: Boolean = false
    private var callBack: CallBack? = null
    private lateinit var menuTopIn: Animation
    private lateinit var menuTopOut: Animation
    private lateinit var menuBottomIn: Animation
    private lateinit var menuBottomOut: Animation
    private var onMenuOutEnd: (() -> Unit)? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        callBack = activity as? CallBack
        inflate(context, R.layout.view_read_menu, this)
        if (context.isNightTheme) {
            fabNightTheme.setImageResource(R.drawable.ic_daytime)
        } else {
            fabNightTheme.setImageResource(R.drawable.ic_brightness)
        }
        initAnimation()
        vw_bg.onClick { }
        vwNavigationBar.onClick { }
        seek_brightness.progress = context.getPrefInt("brightness", 100)
        upBrightnessState()
        bindEvent()
    }

    private fun upBrightnessState() {
        if (brightnessAuto()) {
            iv_brightness_auto.setColorFilter(context.accentColor)
            seek_brightness.isEnabled = false
        } else {
            iv_brightness_auto.setColorFilter(context.buttonDisabledColor)
            seek_brightness.isEnabled = true
        }
        setScreenBrightness(context.getPrefInt("brightness", 100))
    }

    /**
     * 设置屏幕亮度
     */
    private fun setScreenBrightness(value: Int) {
        var brightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        if (!brightnessAuto()) {
            brightness = value.toFloat()
            if (brightness < 1f) brightness = 1f
            brightness /= 255f
        }
        val params = activity?.window?.attributes
        params?.screenBrightness = brightness
        activity?.window?.attributes = params
    }

    fun runMenuIn() {
        this.visible()
        title_bar.visible()
        bottom_menu.visible()
        title_bar.startAnimation(menuTopIn)
        bottom_menu.startAnimation(menuBottomIn)
    }

    fun runMenuOut(onMenuOutEnd: (() -> Unit)? = null) {
        this.onMenuOutEnd = onMenuOutEnd
        if (this.isVisible) {
            title_bar.startAnimation(menuTopOut)
            bottom_menu.startAnimation(menuBottomOut)
        }
    }

    private fun brightnessAuto(): Boolean {
        return context.getPrefBoolean("brightnessAuto", true)
    }

    private fun bindEvent() {
        iv_brightness_auto.onClick {
            context.putPrefBoolean("brightnessAuto", !brightnessAuto())
            upBrightnessState()
        }
        //亮度调节
        seek_brightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                setScreenBrightness(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                context.putPrefInt("brightness", seek_brightness.progress)
            }

        })

        //阅读进度
        seek_read_page.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {

            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                callBack?.skipToPage(seekBar.progress)
            }
        })

        //自动翻页
        fabAutoPage.onClick { callBack?.autoPage() }

        //替换
        fabReplaceRule.onClick { callBack?.openReplaceRule() }

        //夜间模式
        fabNightTheme.onClick {
            context.putPrefBoolean("isNightTheme", !context.isNightTheme)
            App.INSTANCE.applyDayNight()
        }

        //上一章
        tv_pre.onClick { callBack?.moveToPrevChapter(upContent = true, last = false) }

        //下一章
        tv_next.onClick { callBack?.moveToNextChapter(true) }

        //目录
        ll_catalog.onClick {
            runMenuOut {
                callBack?.openChapterList()
            }
        }

        //朗读
        ll_read_aloud.onClick {
            runMenuOut {
                postEvent(Bus.READ_ALOUD_BUTTON, true)
            }
        }
        ll_read_aloud.onLongClick {
            runMenuOut { callBack?.showReadAloudDialog() }
            true
        }
        //界面
        ll_font.onClick {
            runMenuOut {
                callBack?.showReadStyle()
            }
        }

        //设置
        ll_setting.onClick {
            runMenuOut {
                callBack?.showMoreSetting()
            }
        }
    }

    private fun initAnimation() {
        menuTopIn = AnimationUtils.loadAnimation(context, R.anim.anim_readbook_top_in)
        menuBottomIn = AnimationUtils.loadAnimation(context, R.anim.anim_readbook_bottom_in)
        menuTopIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                callBack?.upSystemUiVisibility()
            }

            override fun onAnimationEnd(animation: Animation) {
                vw_menu_bg.onClick { runMenuOut() }
                vwNavigationBar.layoutParams = vwNavigationBar.layoutParams.apply {
                    height =
                        if (context.getPrefBoolean("hideNavigationBar")
                            && Help.isNavigationBarExist(activity)
                        ) context.getNavigationBarHeight()
                        else 0
                }
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })

        //隐藏菜单
        menuTopOut = AnimationUtils.loadAnimation(context, R.anim.anim_readbook_top_out)
        menuBottomOut = AnimationUtils.loadAnimation(context, R.anim.anim_readbook_bottom_out)
        menuTopOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                vw_menu_bg.setOnClickListener(null)
            }

            override fun onAnimationEnd(animation: Animation) {
                this@ReadMenu.invisible()
                title_bar.invisible()
                bottom_menu.invisible()
                cnaShowMenu = false
                onMenuOutEnd?.invoke()
                callBack?.upSystemUiVisibility()
            }

            override fun onAnimationRepeat(animation: Animation) {

            }
        })
    }

    fun setAutoPage(autoPage: Boolean) {
        if (autoPage) {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page_stop)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page_stop)
        } else {
            fabAutoPage.setImageResource(R.drawable.ic_auto_page)
            fabAutoPage.contentDescription = context.getString(R.string.auto_next_page)
        }
    }

    interface CallBack {
        fun autoPage()
        fun skipToPage(page: Int)
        fun moveToPrevChapter(upContent: Boolean, last: Boolean): Boolean
        fun moveToNextChapter(upContent: Boolean): Boolean
        fun openReplaceRule()
        fun openChapterList()
        fun showReadStyle()
        fun showMoreSetting()
        fun showReadAloudDialog()
        fun upSystemUiVisibility()
    }

}

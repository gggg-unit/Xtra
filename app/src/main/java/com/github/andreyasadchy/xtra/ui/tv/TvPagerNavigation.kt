package com.github.andreyasadchy.xtra.ui.tv

import android.graphics.Rect
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.github.andreyasadchy.xtra.R
import com.github.andreyasadchy.xtra.util.isTelevision
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.tabs.TabLayout

private fun TabLayout.tvTabs() = (getChildAt(0) as? ViewGroup)?.children.orEmpty()

internal fun TabLayout.requestSelectedTvTabFocus(): Boolean =
    isShown && tvTabs().elementAtOrNull(selectedTabPosition.coerceAtLeast(0))?.requestFocus() == true

internal fun TabLayout.isFirstTvTab(view: View): Boolean = tvTabs().firstOrNull() === view

internal fun TabLayout.installTvPagerNavigation(
    appBar: AppBarLayout,
    pager: ViewPager2,
    sortBar: View,
    currentPage: () -> View?,
) {
    if (!context.isTelevision()) return
    appBar.children.forEach { child ->
        (child.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
    }
    appBar.setExpanded(true, false)
    pager.isUserInputEnabled = false

    val sortKeys = View.OnKeyListener { _, key, event ->
        event.action == KeyEvent.ACTION_DOWN && when (key) {
            KeyEvent.KEYCODE_DPAD_UP -> requestSelectedTvTabFocus()
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                currentPage()?.requestFirstTvPageAction()
                true
            }
            else -> false
        }
    }
    val tabs = tvTabs().toList()
    tabs.forEachIndexed { index, tab ->
        TvFocusHelper.install(tab, focusedScale = 1f, focusedTranslationZDp = 0f)
        tab.setOnKeyListener { _, key, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            when (key) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    val rtl = layoutDirection == View.LAYOUT_DIRECTION_RTL
                    val towardEnd = (key == KeyEvent.KEYCODE_DPAD_RIGHT) != rtl
                    val neighbor = tabs.getOrNull(index + if (towardEnd) 1 else -1)
                    neighbor?.requestFocus()
                    neighbor != null || towardEnd
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    val page = currentPage()
                    val visibleSort = sequenceOf(sortBar, page?.findViewById<View>(R.id.sortBar))
                        .filterNotNull().firstOrNull { it.isShown && it.isClickable && it.isEnabled }
                    if (visibleSort != null) {
                        TvFocusHelper.install(visibleSort, focusedScale = 1f, focusedTranslationZDp = 0f)
                        visibleSort.setOnKeyListener(sortKeys)
                        visibleSort.requestFocus()
                    } else {
                        page?.requestFirstTvPageAction()
                    }
                    // Keep the tab focused while its page loads.
                    true
                }
                else -> false
            }
        }
    }
}

internal fun View.requestFirstTvPageAction(): Boolean {
    val recycler = findViewById<RecyclerView>(R.id.recyclerView)
    val row = recycler?.children?.firstOrNull { it.isVisibleTvAction() }
    return row?.requestFocus() == true ||
        getFocusables(View.FOCUS_DOWN).firstOrNull { it.isVisibleTvAction() }?.requestFocus() == true
}

private fun View.isVisibleTvAction(): Boolean =
    isShown && isClickable && isFocusable && isEnabled && getGlobalVisibleRect(Rect())

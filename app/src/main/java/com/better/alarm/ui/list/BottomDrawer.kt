package com.better.alarm.ui.list

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.view.View
import com.better.alarm.R
import com.better.alarm.logger.Logger
import com.better.alarm.ui.main.MainViewModel
import com.better.alarm.ui.themes.resolveColor
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.melnykov.fab.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

fun configureBottomDrawer(
    context: Context,
    view: View,
    logger: Logger,
    mainViewModel: MainViewModel,
    scope: CoroutineScope,
) {
  val drawerContainer: View = view.findViewById<View>(R.id.bottom_drawer_container)
  val bottomDrawerToolbar = view.findViewById<View>(R.id.bottom_drawer_toolbar)
  val bottomDrawerContent = view.findViewById<View>(R.id.bottom_drawer_content)
  val fab: FloatingActionButton? = view.findViewById<FloatingActionButton>(R.id.fab)
  val infoFragment = view.findViewById<View>(R.id.list_activity_info_fragment)

  fun setDrawerBackgrounds(resolveColor: Int) {
    val colorDrawable = ColorDrawable(resolveColor)
    bottomDrawerToolbar.background = colorDrawable
    bottomDrawerContent.background = colorDrawable
    drawerContainer.background = colorDrawable
  }

  val openColor = context.theme.resolveColor(R.attr.drawerBackgroundColor)
  val closedColor = context.theme.resolveColor(R.attr.drawerClosedBackgroundColor)

  BottomSheetBehavior.from(drawerContainer).apply {
    val initialElevation = drawerContainer.elevation
    val initialFabElevation = fab?.elevation ?: 0f
    val fabAtOverlap = 3f
    // offset of about 0.1 means overlap
    val overlap = 0.1f
    val fabK = -((initialFabElevation - fabAtOverlap) / overlap)

    peekHeight = bottomDrawerToolbar.minimumHeight

    mainViewModel.drawerExpanded
        .onEach { expanded ->
          state =
              if (expanded) {
                setDrawerBackgrounds(openColor)
                // hide the info when drawer is open
                infoFragment.alpha = 0.0f
                BottomSheetBehavior.STATE_EXPANDED
              } else {
                setDrawerBackgrounds(closedColor)
                drawerContainer.elevation = 0f
                BottomSheetBehavior.STATE_COLLAPSED
              }
        }
        .launchIn(scope)

    bottomDrawerToolbar.setOnClickListener { mainViewModel.drawerExpanded.update { !it } }

    addBottomSheetCallback(
        object : BottomSheetCallback() {
          override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState == BottomSheetBehavior.STATE_EXPANDED) {
              mainViewModel.drawerExpanded.value = true
            } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
              mainViewModel.drawerExpanded.value = false
              // setDrawerBackgrounds(closedColor)
            }
          }

          override fun onSlide(bottomSheet: View, slideOffset: Float) {
            val color =
                ArgbEvaluator()
                    .evaluate(
                        slideOffset,
                        closedColor,
                        openColor,
                    ) as Int
            setDrawerBackgrounds(color)

            drawerContainer.elevation = initialElevation * slideOffset
            if (slideOffset > overlap) {
              fab?.elevation = fabAtOverlap
            } else {
              fab?.elevation = fabK * slideOffset + initialFabElevation
            }
            // hide the info when drawer is open
            infoFragment.alpha = 1.0f - slideOffset
          }
        })
  }
}

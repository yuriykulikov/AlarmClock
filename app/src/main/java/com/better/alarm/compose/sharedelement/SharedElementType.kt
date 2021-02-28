/**
 * Copied from here: https://github.com/vanpra/compose-shared-element
 */
package com.better.alarm.compose.sharedelement

import android.view.Choreographer
import androidx.compose.animation.OffsetPropKey
import androidx.compose.animation.core.FloatPropKey
import androidx.compose.animation.core.TransitionState
import androidx.compose.animation.core.transitionDefinition
import androidx.compose.animation.core.tween
import androidx.compose.animation.transition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.preferredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.invalidate
import androidx.compose.runtime.onDispose
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.AmbientDensity
import com.better.alarm.compose.sharedelement.SharedElementTransition.InProgress
import com.better.alarm.compose.sharedelement.SharedElementTransition.WaitingForEndElementPosition
import com.better.alarm.compose.sharedelement.SharedElementsTracker.State.Empty
import com.better.alarm.compose.sharedelement.SharedElementsTracker.State.EndElementRegistered
import com.better.alarm.compose.sharedelement.SharedElementsTracker.State.StartElementPositioned
import com.better.alarm.compose.sharedelement.SharedElementsTracker.State.StartElementRegistered
import kotlin.collections.set
import kotlin.properties.Delegates

enum class SharedElementType { FROM, TO }

private val disable = true

/**
 * Copied from here: https://github.com/vanpra/compose-shared-element
 */
@Composable
fun SharedElement(
  tag: Any,
  type: SharedElementType,
  modifier: Modifier = Modifier,
  placeholder: @Composable (() -> Unit)? = null,
  children: @Composable () -> Unit,
) {
  if (disable) {
    children()
    return
  }
  val elementInfo = SharedElementInfo(tag, type)
  val rootState = AmbientSharedElementsRootState.current

  rootState.onElementRegistered(elementInfo)

  val recompose = invalidate
  val visibilityModifier =
    if (rootState.shouldHideElement(elementInfo)) Modifier.graphicsLayer(alpha = 0f) else Modifier
  Box(modifier = modifier.then(visibilityModifier)) {
    Box(modifier = Modifier.onGloballyPositioned { coordinates ->
      rootState.onElementPositioned(
        elementInfo = elementInfo,
        placeholder = placeholder ?: children,
        coordinates = coordinates,
        invalidateElement = recompose
      )
    }) {
      children()
    }
  }

  onDispose {
    rootState.onElementDisposed(elementInfo)
  }
}

@Composable
fun SharedElementsRoot(children: @Composable () -> Unit) {
  val rootState = remember { SharedElementsRootState() }

  Box(modifier = Modifier.onGloballyPositioned { layoutCoordinates ->
    rootState.rootCoordinates = layoutCoordinates
  }) {
    Providers(AmbientSharedElementsRootState provides rootState) {
      children()
    }
    SharedElementTransitionsOverlay(rootState)
  }

  onDispose {
    rootState.onDisposed()
  }
}

@Composable
private fun SharedElementTransitionsOverlay(rootState: SharedElementsRootState) {
  rootState.invalidateTransitionsOverlay = invalidate
  rootState.trackers.values.forEach { tracker ->
    when (val transition = tracker.transition) {
      is WaitingForEndElementPosition -> SharedElementTransitionPlaceholder(
        sharedElement = transition.startElement,
        offsetX = transition.startElement.bounds.left,
        offsetY = transition.startElement.bounds.top
      )
      is InProgress -> {
        val transitionState = transition(
          definition = transition.transitionDefinition,
          toState = InProgress.State.END,
          initState = InProgress.State.START,
          label = null,
          onStateChangeFinished = { state ->
            if (state == InProgress.State.END) {
              transition.onTransitionFinished()
            }
          }
        )

        SharedElementTransitionPlaceholder(
          sharedElement = transition.startElement,
          transitionState = transitionState,
          propKeys = transition.startElementPropKeys
        )
        SharedElementTransitionPlaceholder(
          sharedElement = transition.endElement,
          transitionState = transitionState,
          propKeys = transition.endElementPropKeys
        )
      }
    }
  }
}

@Composable
private fun SharedElementTransitionPlaceholder(
  sharedElement: PositionedSharedElement,
  transitionState: TransitionState,
  propKeys: InProgress.SharedElementPropKeys,
) {
  SharedElementTransitionPlaceholder(
    sharedElement = sharedElement,
    offsetX = transitionState[propKeys.position].x,
    offsetY = transitionState[propKeys.position].y,
    scaleX = transitionState[propKeys.scaleX],
    scaleY = transitionState[propKeys.scaleY],
    alpha = transitionState[propKeys.alpha]
  )
}

@Composable
private fun SharedElementTransitionPlaceholder(
  sharedElement: PositionedSharedElement,
  offsetX: Float,
  offsetY: Float,
  scaleX: Float = 1f,
  scaleY: Float = 1f,
  alpha: Float = 1f,
) {
  with(AmbientDensity.current) {
    Box(
      modifier = Modifier.preferredSize(
        width = sharedElement.bounds.width.toDp(),
        height = sharedElement.bounds.height.toDp()
      ).offset(
        x = offsetX.toDp(),
        y = offsetY.toDp()
      ).graphicsLayer(
//                elevation = Float.MAX_VALUE,
        scaleX = scaleX,
        scaleY = scaleY,
        alpha = alpha
      ),
      content = { sharedElement.placeholder() }
    )
  }
}

private val AmbientSharedElementsRootState = staticAmbientOf<SharedElementsRootState> {
  error("SharedElementsRoot not found. SharedElement must be hosted in SharedElementsRoot.")
}

private class SharedElementsRootState {
  private val choreographer = ChoreographerWrapper()
  val trackers = mutableMapOf<SharedElementTag, SharedElementsTracker>()
  var invalidateTransitionsOverlay: () -> Unit = {}
  var rootCoordinates: LayoutCoordinates? = null

  fun shouldHideElement(elementInfo: SharedElementInfo): Boolean {
    return getTracker(elementInfo).shouldHideElement
  }

  fun onElementRegistered(elementInfo: SharedElementInfo) {
    choreographer.removeCallback(elementInfo)
    getTracker(elementInfo).onElementRegistered(elementInfo)
  }

  fun onElementPositioned(
    elementInfo: SharedElementInfo,
    placeholder: @Composable () -> Unit,
    coordinates: LayoutCoordinates,
    invalidateElement: () -> Unit,
  ) {
    val element = PositionedSharedElement(
      info = elementInfo,
      placeholder = placeholder,
      bounds = calculateElementBoundsInRoot(coordinates)
    )
    getTracker(elementInfo).onElementPositioned(element, invalidateElement)
  }

  fun onElementDisposed(elementInfo: SharedElementInfo) {
    choreographer.postCallback(elementInfo) {
      val tracker = getTracker(elementInfo)
      tracker.onElementUnregistered(elementInfo)
      if (tracker.isEmpty) trackers.remove(elementInfo.tag)
    }
  }

  fun onDisposed() {
    choreographer.clear()
  }

  private fun getTracker(elementInfo: SharedElementInfo): SharedElementsTracker {
    return trackers.getOrPut(elementInfo.tag) {
      SharedElementsTracker(onTransitionChanged = { invalidateTransitionsOverlay() })
    }
  }

  private fun calculateElementBoundsInRoot(elementCoordinates: LayoutCoordinates): Rect {
    return rootCoordinates?.childBoundingBox(elementCoordinates)
      ?: elementCoordinates.boundsInRoot
  }
}

private class SharedElementsTracker(
  private val onTransitionChanged: () -> Unit,
) {
  private var state: State = Empty

  var transition by Delegates.observable<SharedElementTransition?>(null) { _, oldValue, newValue ->
    if (oldValue != newValue) {
      (oldValue as? InProgress)?.cleanup()
      onTransitionChanged()
    }
  }

  val isEmpty: Boolean get() = state is Empty

  val shouldHideElement: Boolean get() = transition != null

  fun onElementRegistered(elementInfo: SharedElementInfo) {
    when (val state = state) {
      is StartElementPositioned -> {
        if (!state.isRegistered(elementInfo)) {
          this.state = EndElementRegistered(
            startElement = state.startElement,
            endElementInfo = elementInfo
          )
          transition = WaitingForEndElementPosition(state.startElement)
        }
      }
      is StartElementRegistered -> {
        if (elementInfo != state.startElementInfo) {
          this.state = StartElementRegistered(startElementInfo = elementInfo)
        }
      }
      is Empty -> {
        this.state = StartElementRegistered(startElementInfo = elementInfo)
      }
    }
  }

  fun onElementPositioned(element: PositionedSharedElement, invalidateElement: () -> Unit) {
    when (val state = state) {
      is EndElementRegistered -> {
        if (element.info == state.endElementInfo) {
          this.state = StartElementPositioned(startElement = element)
          transition = InProgress(
            startElement = state.startElement,
            endElement = element,
            onTransitionFinished = {
              transition = null
              invalidateElement()
            })
        } else if (element.info == state.startElementInfo) {
          this.state = EndElementRegistered(
            startElement = element,
            endElementInfo = state.endElementInfo
          )
          transition = WaitingForEndElementPosition(startElement = element)
        }
      }
      is StartElementRegistered -> {
        if (element.info == state.startElementInfo) {
          this.state = StartElementPositioned(startElement = element)
        }
      }
    }
  }

  fun onElementUnregistered(elementInfo: SharedElementInfo) {
    when (val state = state) {
      is EndElementRegistered -> {
        if (elementInfo == state.endElementInfo) {
          this.state = StartElementPositioned(startElement = state.startElement)
          transition = null
        } else if (elementInfo == state.startElement.info) {
          this.state = StartElementRegistered(startElementInfo = state.endElementInfo)
          transition = null
        }
      }
      is StartElementRegistered -> {
        if (elementInfo == state.startElementInfo) {
          this.state = Empty
          transition = null
        }
      }
    }
  }

  private sealed class State {
    object Empty : State()

    open class StartElementRegistered(val startElementInfo: SharedElementInfo) : State() {
      open fun isRegistered(elementInfo: SharedElementInfo): Boolean {
        return elementInfo == startElementInfo
      }
    }

    open class StartElementPositioned(open val startElement: PositionedSharedElement) :
      StartElementRegistered(startElement.info)

    class EndElementRegistered(
      override val startElement: PositionedSharedElement,
      val endElementInfo: SharedElementInfo,
    ) : StartElementPositioned(startElement) {
      override fun isRegistered(elementInfo: SharedElementInfo): Boolean {
        return super.isRegistered(elementInfo) || elementInfo == endElementInfo
      }
    }
  }
}

private typealias SharedElementTag = Any

private data class SharedElementInfo(val tag: SharedElementTag, val type: SharedElementType)

private class PositionedSharedElement(
  val info: SharedElementInfo,
  val placeholder: @Composable () -> Unit,
  val bounds: Rect,
)

private sealed class SharedElementTransition(val startElement: PositionedSharedElement) {

  class WaitingForEndElementPosition(startElement: PositionedSharedElement) :
    SharedElementTransition(startElement)

  class InProgress(
    startElement: PositionedSharedElement,
    val endElement: PositionedSharedElement,
    var onTransitionFinished: () -> Unit,
  ) : SharedElementTransition(startElement) {

    val startElementPropKeys = SharedElementPropKeys()
    val endElementPropKeys = SharedElementPropKeys()

    val transitionDefinition = transitionDefinition<State> {
      state(State.START) {
        this[startElementPropKeys.position] =
          Offset(startElement.bounds.left, startElement.bounds.top)
        this[startElementPropKeys.scaleX] = 1f
        this[startElementPropKeys.scaleY] = 1f
        this[startElementPropKeys.alpha] = 1f
        this[endElementPropKeys.position] = Offset(
          x = startElement.bounds.left + (startElement.bounds.width - endElement.bounds.width) / 2f,
          y = startElement.bounds.top + (startElement.bounds.height - endElement.bounds.height) / 2f
        )
        this[endElementPropKeys.scaleX] =
          startElement.bounds.width / endElement.bounds.width
        this[endElementPropKeys.scaleY] =
          startElement.bounds.height / endElement.bounds.height
        this[endElementPropKeys.alpha] = 0f
      }
      state(State.END) {
        this[startElementPropKeys.position] = Offset(
          x = endElement.bounds.left + (endElement.bounds.width - startElement.bounds.width) / 2f,
          y = endElement.bounds.top + (endElement.bounds.height - startElement.bounds.height) / 2f
        )
        this[startElementPropKeys.scaleX] =
          endElement.bounds.width / startElement.bounds.width
        this[startElementPropKeys.scaleY] =
          endElement.bounds.height / startElement.bounds.height
        this[startElementPropKeys.alpha] = 0f
        this[endElementPropKeys.position] =
          Offset(endElement.bounds.left, endElement.bounds.top)
        this[endElementPropKeys.scaleX] = 1f
        this[endElementPropKeys.scaleY] = 1f
        this[endElementPropKeys.alpha] = 1f
      }
      transition(fromState = State.START, toState = State.END) {
        sequenceOf(startElementPropKeys, endElementPropKeys).flatMap {
          sequenceOf(it.position, it.scaleX, it.scaleY, it.alpha)
        }.forEach { key ->
          key using tween(durationMillis = 300)
        }
      }
    }

    fun cleanup() {
      onTransitionFinished = {}
    }

    enum class State {
      START, END
    }

    class SharedElementPropKeys {
      val position = OffsetPropKey("position")
      val scaleX = FloatPropKey("scaleX")
      val scaleY = FloatPropKey("scaleY")
      val alpha = FloatPropKey("alpha")
    }
  }
}

private class ChoreographerWrapper {
  private val callbacks = mutableMapOf<SharedElementInfo, Choreographer.FrameCallback>()
  private val choreographer = Choreographer.getInstance()

  fun postCallback(elementInfo: SharedElementInfo, callback: () -> Unit) {
    if (callbacks.containsKey(elementInfo)) return

    val frameCallback = Choreographer.FrameCallback {
      callbacks.remove(elementInfo)
      callback()
    }
    callbacks[elementInfo] = frameCallback
    choreographer.postFrameCallback(frameCallback)
  }

  fun removeCallback(elementInfo: SharedElementInfo) {
    callbacks.remove(elementInfo)?.also(choreographer::removeFrameCallback)
  }

  fun clear() {
    callbacks.values.forEach(choreographer::removeFrameCallback)
    callbacks.clear()
  }
}
package com.better.alarm.statemachine

import com.better.alarm.domain.statemachine.State
import com.better.alarm.domain.statemachine.StateMachine
import com.better.alarm.logger.Logger
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class StateMachineTest {
  private val stateMachine =
      StateMachine<Event>(name = "TestStateMachine", logger = Logger.create())

  private val captured: MutableList<CapturedEvent> = mutableListOf()
  private val capturedEvents: List<OnEvent>
    get() = captured.filterIsInstance<OnEvent>()

  private val capturedTransitions: List<CapturedEvent>
    get() = captured.filterNot { it is OnEvent }

  private fun state(tag: String, block: (Event) -> Boolean): State<Event> {
    return object : State<Event>() {
      override fun enter(reason: Event?) {
        captured.add(OnEnter(this))
      }

      override fun exit(reason: Event?) {
        captured.add(OnExit(this))
      }

      override fun onEvent(event: Event): Boolean {
        captured.add(OnEvent(this, event))
        return block(event)
      }

      override val name
        get() = "[$tag]"
    }
  }

  @Test
  fun `events are processed in states hierarchy`() {
    val root = state("root") { true }
    val s1 = state("s1") { true }
    val s2 = state("s2") { false }
    val s3 = state("s3") { false }

    stateMachine.start(Switch(on = true)) {
      addState(state = root)
      addState(state = s1, parent = root)
      addState(state = s2, parent = s1)
      addState(state = s3, parent = s2, initial = true)
    }

    assertThat(capturedEvents).isEmpty()

    val grind = Grind(1)

    // event goes to s3, then s2 and is handled in s1
    stateMachine.sendEvent(grind)

    // root does not receive the events
    assertThat(capturedEvents)
        .containsExactly(OnEvent(s3, grind), OnEvent(s2, grind), OnEvent(s1, grind))
  }

  @Test
  fun `states are entered in the proper order on start`() {
    val root = state("root") { true }
    val s1 = state("s1") { true }
    val s2 = state("s2") { false }
    val s3 = state("s3") { false }

    stateMachine.start(Switch(on = true)) {
      addState(state = root)
      addState(state = s1, parent = root)
      addState(state = s2, parent = s1)
      addState(state = s3, parent = s2, initial = true)
    }

    assertThat(captured).containsExactly(OnEnter(root), OnEnter(s1), OnEnter(s2), OnEnter(s3))
  }

  @Test
  fun `states are entered in the proper order on start if branching is present`() {
    val root = state("root") { true }
    val s0 = state("s0") { true }
    val s1 = state("s1") { true }
    val s1a = state("s1a") { false }
    val s1b = state("s1b") { false }

    val s2 = state("s2") { true }
    val s2a = state("s2a") { false }
    val s2b = state("s2b") { false }
    //           root
    //           /
    //          s0
    //       /      \
    //      s1      s2
    //     /  \     /  \
    //  s1a  s1b   s2a  s2b
    stateMachine.start(Switch(on = true)) {
      addState(state = root)
      addState(state = s0, parent = root)
      addState(state = s1, parent = s0)
      addState(state = s1a, parent = s1)
      addState(state = s1b, parent = s1)
      addState(state = s2, parent = s0)
      addState(state = s2a, parent = s2)
      addState(state = s2b, parent = s2)
      setInitialState(s2b)
    }

    assertThat(captured).containsExactly(OnEnter(root), OnEnter(s0), OnEnter(s2), OnEnter(s2b))
  }

  @Test
  fun `transitions trigger enter methods`() {
    val root = state("root") { true }
    val s0 = state("s0") { true }
    val s1 = state("s1") { true }
    val s1a = state("s1a") { false }
    val s1b =
        state("s1b") {
          stateMachine.transitionTo(s0)
          true
        }

    val s2 = state("s2") { true }
    val s2a = state("s2a") { false }
    val s2b =
        state("s2b") {
          stateMachine.transitionTo(s1b)
          true
        }
    //           root
    //           /
    //          s0
    //       /      \
    //      s1      s2
    //     /  \     /  \
    //  s1a  s1b   s2a  s2b
    stateMachine.start(Switch(on = true)) {
      addState(state = root)
      addState(state = s0, parent = root)
      addState(state = s1, parent = s0)
      addState(state = s1a, parent = s1)
      addState(state = s1b, parent = s1)
      addState(state = s2, parent = s0)
      addState(state = s2a, parent = s2)
      addState(state = s2b, parent = s2)
      setInitialState(s2b)
    }
    // VERIFY after start we have entered s2b
    assertThat(capturedTransitions)
        .containsExactly(OnEnter(root), OnEnter(s0), OnEnter(s2), OnEnter(s2b))
    captured.clear()

    // WHEN trigger transition to s1b
    stateMachine.sendEvent(Brew(1))
    // VERIFY first we exit and then we enter
    assertThat(capturedTransitions)
        .containsExactly(OnExit(s2b), OnExit(s2), OnEnter(s1), OnEnter(s1b))

    // RESET
    captured.clear()
    // WHEN trigger transition to s0
    stateMachine.sendEvent(Brew(1))
    // VERIFY since we are already in root and s0, no states will be entered, only 2 states exit
    assertThat(capturedTransitions).containsExactly(OnExit(s1b), OnExit(s1))
  }

  @Test
  fun `defer allows to process events in the next state after transition`() {
    val root = state("root") { true }
    val s0 = state("s0") { true }
    val s1 = state("s1") { true }
    val s1a = state("s1a") { false }
    val ready = state("ready") { true }

    val s2 = state("s2") { true }
    val s2a = state("s2a") { false }
    val grinding =
        state("grinding") {
          when (it) {
            is DoneGrinding -> stateMachine.transitionTo(ready)
            // all other events are deferred because we are busy
            else -> stateMachine.deferEvent(it)
          }
          true
        }
    //           root
    //           /
    //          s0
    //       /      \
    //      s1      s2
    //     /  \     /  \
    //  s1a ready s2a  grinding
    stateMachine.start(Switch(on = true)) {
      addState(state = root)
      addState(state = s0, parent = root)
      addState(state = s1, parent = s0)
      addState(state = s1a, parent = s1)
      addState(state = ready, parent = s1)
      addState(state = s2, parent = s0)
      addState(state = s2a, parent = s2)
      addState(state = grinding, parent = s2)
      setInitialState(grinding)
    }

    // WHEN events are sent
    stateMachine.sendEvent(Brew(1))
    stateMachine.sendEvent(Brew(2))
    // VERIFY they are processed by current state
    assertThat(capturedEvents)
        .containsExactly(OnEvent(grinding, Brew(1)), OnEvent(grinding, Brew(2)))

    // RESET
    captured.clear()
    // WHEN trigger transition to s1b
    stateMachine.sendEvent(DoneGrinding)
    // VERIFY all deferred events are delivered to ready
    assertThat(capturedEvents)
        .containsExactly(
            OnEvent(grinding, DoneGrinding), OnEvent(ready, Brew(1)), OnEvent(ready, Brew(2)))
  }
}

sealed class Event {
  override fun toString(): String = javaClass.simpleName
}

data class Switch(val on: Boolean) : Event()

data class Grind(val portions: Int) : Event()

object DoneGrinding : Event()

data class Brew(val cups: Int) : Event()

object DoneBrewing : Event()

sealed class CapturedEvent

data class OnEnter(val state: State<Event>) : CapturedEvent()

data class OnExit(val state: State<Event>) : CapturedEvent()

data class OnEvent(val state: State<Event>, val event: Event) : CapturedEvent()

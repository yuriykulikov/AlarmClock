/**
 * Copyright (C) 2009 The Android Open Source Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.better.alarm.statemachine;

import com.better.alarm.logger.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class StateMachine {
    private final String mName;

    private final Logger log;

    /** Message.what value when initializing */
    private static final int SM_INIT_CMD = -2;

    private static class SmHandler implements MessageHandler {
        private final Logger log;

        /** The current message */
        private Message mMsg;

        /** true if construction of the state machine has not been completed */
        private boolean mIsConstructionCompleted;

        /** Stack used to manage the current hierarchy of states */
        private StateInfo mStateStack[];

        /** Top of mStateStack */
        private int mStateStackTopIndex = -1;

        /** A temporary stack used to manage the state stack */
        private StateInfo mTempStateStack[];

        /** The top of the mTempStateStack */
        private int mTempStateStackCount;

        /** Reference to the StateMachine */
        private StateMachine mSm;

        private final IHandler handler;

        /** The map of all of the states in the state machine */
        private final HashMap<State, StateInfo> mStateInfo = new HashMap<State, StateInfo>();

        /** The initial state that will process the first message */
        private State mInitialState;

        /** The destination state when transitionTo has been invoked */
        private State mDestState;

        /** The list of deferred messages */
        private final ArrayList<Message> mDeferredMessages = new ArrayList<Message>();

        private final CopyOnWriteArrayList<IOnStateChangedListener> onStateChangedListeners = new CopyOnWriteArrayList<IOnStateChangedListener>();

        /**
         * Information about a state. Used to maintain the hierarchy.
         */
        private class StateInfo {
            /** The state */
            public State state;

            /** The parent of this state, null if there is no parent */
            public StateInfo parentStateInfo;

            /** True when the state has been entered and on the stack */
            public boolean active;

            /**
             * Convert StateInfo to string
             */
            @Override
            public String toString() {
                return "state=" + state.getName() + ",active=" + active + ",parent="
                        + (parentStateInfo == null ? "null" : parentStateInfo.state.getName());
            }
        }

        /**
         * Handle messages sent to the state machine by calling the current
         * state's processMessage. It also handles the enter/exit calls and
         * placing any deferred messages back onto the queue when transitioning
         * to a new state.
         */
        @Override
        public final void handleMessage(Message msg) {
            /** Save the current message */
            mMsg = msg;

            if (mIsConstructionCompleted) {
                /** Normal path */
                processMsg(msg);
            } else if (!mIsConstructionCompleted && mMsg.what() == SM_INIT_CMD) {
                /** Initial one time path. */
                mIsConstructionCompleted = true;
                boolean resume = msg.arg1().or(-100500) == 1;
                invokeEnterMethods(0, resume);
            } else throw new RuntimeException("StateMachine.handleMessage: "
                    + "The start method not called, received msg: " + msg);
            performTransitions();
        }

        /**
         * Do any transitions
         */
        private void performTransitions() {
            /**
             * If transitionTo has been called, exit and then enter the
             * appropriate states. We loop on this to allow enter and exit
             * methods to use transitionTo.
             */
            State destState = null;
            while (mDestState != null) {
                /**
                 * Save mDestState locally and set to null to know if enter/exit
                 * use transitionTo.
                 */
                destState = mDestState;
                mDestState = null;

                /**
                 * Determine the states to exit and enter and return the common
                 * ancestor state of the enter/exit states. Then invoke the exit
                 * methods then the enter methods.
                 */
                StateInfo commonStateInfo = setupTempStateStackWithStatesToEnter(destState);
                invokeExitMethods(commonStateInfo);
                int stateStackEnteringIndex = moveTempStateStackToStateStack();
                invokeEnterMethods(stateStackEnteringIndex, false);

                /**
                 * Since we have transitioned to a new state we need to have any
                 * deferred messages moved to the front of the message queue so
                 * they will be processed before any other messages in the
                 * message queue.
                 */
                moveDeferredMessageAtFrontOfQueue();
            }
        }

        /**
         * Complete the construction of the state machine.
         *
         * @param resume
         */
        private final void completeConstruction(boolean resume) {
            /**
             * Determine the maximum depth of the state hierarchy so we can
             * allocate the state stacks.
             */
            int maxDepth = 0;
            for (StateInfo si : mStateInfo.values()) {
                int depth = 0;
                for (StateInfo i = si; i != null; depth++) {
                    i = i.parentStateInfo;
                }
                if (maxDepth < depth) {
                    maxDepth = depth;
                }
            }

            mStateStack = new StateInfo[maxDepth];
            mTempStateStack = new StateInfo[maxDepth];
            setupInitialStateStack();

            /**
             * Sending SM_INIT_CMD message to invoke enter methods
             * asynchronously
             */
            handler.obtainMessage(SM_INIT_CMD)
                    .withArg1(resume ? 1 : 0)
                    .sendAtFront();

            log.d("completed construction of " + mSm.getName());
        }

        /**
         * Process the message. If the current state doesn't handle it, call the
         * states parent and so on. If it is never handled then call the state
         * machines unhandledMessage method.
         */
        private final void processMsg(Message msg) {
            StateInfo curStateInfo = mStateStack[mStateStackTopIndex];
            // TODO handled/not handled
            log.d('[' + mSm.getName() + "] " + curStateInfo.state.getName() + " <- " + msg);

            while (!curStateInfo.state.processMessage(msg)) {
                /**
                 * Not processed
                 */
                curStateInfo = curStateInfo.parentStateInfo;
                if (curStateInfo == null) {
                    /**
                     * No parents left so it's not handled
                     */
                    mSm.log.e(mSm.mName + " was not able to handle " + msg);
                    break;
                }
                log.d('[' + mSm.getName() + "] \\" + curStateInfo.state.getName());
            }
        }

        /**
         * Call the exit method for each state from the top of stack up to the
         * common ancestor state.
         */
        private final void invokeExitMethods(StateInfo commonStateInfo) {
            while (mStateStackTopIndex >= 0 && mStateStack[mStateStackTopIndex] != commonStateInfo) {
                State curState = mStateStack[mStateStackTopIndex].state;
                curState.exit();
                mStateStack[mStateStackTopIndex].active = false;
                mStateStackTopIndex -= 1;
            }
        }

        /**
         * Invoke the enter method starting at the entering index to top of
         * state stack
         *
         * @param resume
         *            true if state machine is resuming from hibernation. In
         *            this case {@link IState#enter()} will not be invoked
         */
        private final void invokeEnterMethods(int stateStackEnteringIndex, boolean resume) {
            for (int i = stateStackEnteringIndex; i <= mStateStackTopIndex; i++) {
                if (!resume) {
                    onStateChanged(mStateStack[i].state);
                    mStateStack[i].state.enter();
                }
                mStateStack[i].state.resume();
                mStateStack[i].active = true;
            }
        }

        /**
         * Move the deferred message to the front of the message queue.
         */
        private final void moveDeferredMessageAtFrontOfQueue() {
            /**
             * The oldest messages on the deferred list must be at the front of
             * the queue so start at the back, which as the most resent message
             * and end with the oldest messages at the front of the queue.
             */
            for (int i = mDeferredMessages.size() - 1; i >= 0; i--) {
                Message curMsg = mDeferredMessages.get(i);
                log.d(curMsg + " in " + mSm.getName());
                handler.sendMessageAtFrontOfQueue(curMsg);
            }
            mDeferredMessages.clear();
        }

        /**
         * Move the contents of the temporary stack to the state stack reversing
         * the order of the items on the temporary stack as they are moved.
         *
         * @return index into mStateStack where entering needs to start
         */
        private final int moveTempStateStackToStateStack() {
            int startingIndex = mStateStackTopIndex + 1;
            int i = mTempStateStackCount - 1;
            int j = startingIndex;
            while (i >= 0) {
                mStateStack[j] = mTempStateStack[i];
                j += 1;
                i -= 1;
            }

            mStateStackTopIndex = j - 1;
            return startingIndex;
        }

        /**
         * Setup the mTempStateStack with the states we are going to enter.
         *
         * This is found by searching up the destState's ancestors for a state
         * that is already active i.e. StateInfo.active == true. The destStae
         * and all of its inactive parents will be on the TempStateStack as the
         * list of states to enter.
         *
         * @return StateInfo of the common ancestor for the destState and
         *         current state or null if there is no common parent.
         */
        private final StateInfo setupTempStateStackWithStatesToEnter(State destState) {
            /**
             * Search up the parent list of the destination state for an active
             * state. Use a do while() loop as the destState must always be
             * entered even if it is active. This can happen if we are
             * exiting/entering the current state.
             */
            mTempStateStackCount = 0;
            StateInfo curStateInfo = mStateInfo.get(destState);
            do {
                mTempStateStack[mTempStateStackCount++] = curStateInfo;
                curStateInfo = curStateInfo.parentStateInfo;
            } while (curStateInfo != null && !curStateInfo.active);

            return curStateInfo;
        }

        /**
         * Initialize StateStack to mInitialState.
         */
        private final void setupInitialStateStack() {
            StateInfo curStateInfo = mStateInfo.get(mInitialState);
            for (mTempStateStackCount = 0; curStateInfo != null; mTempStateStackCount++) {
                mTempStateStack[mTempStateStackCount] = curStateInfo;
                curStateInfo = curStateInfo.parentStateInfo;
            }

            // Empty the StateStack
            mStateStackTopIndex = -1;

            moveTempStateStackToStateStack();
        }

        /**
         * @return current message
         */
        private final Message getCurrentMessage() {
            return mMsg;
        }

        /**
         * @return current state
         */
        private final IState getCurrentState() {
            return mStateStack[mStateStackTopIndex].state;
        }

        /**
         * Add a new state to the state machine. Bottom up addition of states is
         * allowed but the same state may only exist in one hierarchy.
         *
         * @param state
         *            the state to add
         * @param parent
         *            the parent of state
         * @return stateInfo for this state
         */
        private final StateInfo addState(State state, State parent) {
            StateInfo parentStateInfo = null;
            if (parent != null) {
                parentStateInfo = mStateInfo.get(parent);
                if (parentStateInfo == null) {
                    // Recursively add our parent as it's not been added yet.
                    parentStateInfo = addState(parent, null);
                }
            }
            StateInfo stateInfo = mStateInfo.get(state);
            if (stateInfo == null) {
                stateInfo = new StateInfo();
                mStateInfo.put(state, stateInfo);
            }

            // Validate that we aren't adding the same state in two different
            // hierarchies.
            if (stateInfo.parentStateInfo != null && stateInfo.parentStateInfo != parentStateInfo)
                throw new RuntimeException("state already added");
            stateInfo.state = state;
            stateInfo.parentStateInfo = parentStateInfo;
            stateInfo.active = false;
            return stateInfo;
        }

        private SmHandler(HandlerFactory hf, StateMachine sm, Logger log) {
            mSm = sm;
            this.log = log;
            this.handler = hf.create(this);
        }

        /** @see StateMachine#setInitialState(State) */
        private final void setInitialState(State initialState) {
            mInitialState = initialState;
        }

        /** @see StateMachine#transitionTo(IState) */
        private final void transitionTo(IState destState) {
            mDestState = (State) destState;
            log.d("[" + mSm.getName() + "] " + mStateStack[mStateStackTopIndex].state.getName() + " -> "
                    + mDestState.getName());
        }

        /** @see StateMachine#deferMessage(Message) */
        private final void deferMessage(Message msg) {
            log.d(msg + " in " + mSm.getName());
            mDeferredMessages.add(msg);
        }

        /**
         * Get all states.
         *
         * @return
         */
        public Collection<State> getStates() {
            return mStateInfo.keySet();
        }

        private void onStateChanged(State state) {
            for (IOnStateChangedListener onStateChangedListener : onStateChangedListeners) {
                onStateChangedListener.onStateChanged(state);
            }
        }

        public void addOnStateChangedListener(IOnStateChangedListener onStateChangedListener) {
            onStateChangedListeners.add(onStateChangedListener);
        }
    }

    private SmHandler mSmHandler;

    /**
     * Constructor creates a StateMachine using the looper.
     *
     * @param name
     *            of the state machine
     */
    public StateMachine(String name, HandlerFactory handlerFactory, Logger log) {
        mName = name;
        this.log = log;
        mSmHandler = new SmHandler(handlerFactory, this, log);
    }

    /**
     * Add a new state to the state machine
     *
     * @param state
     *            the state to add
     * @param parent
     *            the parent of state
     */
    protected final void addState(State state, State parent) {
        mSmHandler.addState(state, parent);
    }

    public final Collection<State> getStates() {
        return mSmHandler.getStates();
    }

    /**
     * @return current message
     */
    protected final Message getCurrentMessage() {
        return mSmHandler.getCurrentMessage();
    }

    /**
     * @return current state
     */
    public final IState getCurrentState() {
        return mSmHandler.getCurrentState();
    }

    /**
     * Add a new state to the state machine, parent will be null
     *
     * @param state
     *            to add
     */
    protected final void addState(State state) {
        mSmHandler.addState(state, null);
    }

    /**
     * Set the initial state. This must be invoked before and messages are sent
     * to the state machine.
     *
     * @param initialState
     *            is the state which will receive the first message.
     */
    protected final void setInitialState(State initialState) {
        mSmHandler.setInitialState(initialState);
    }

    /**
     * transition to destination state. Upon returning from processMessage the
     * current state's exit will be executed and upon the next message arriving
     * destState.enter will be invoked.
     *
     * this function can also be called inside the enter function of the
     * previous transition target, but the behavior is undefined when it is
     * called mid-way through a previous transition (for example, calling this
     * in the enter() routine of a intermediate node when the current transition
     * target is one of the nodes descendants).
     *
     * @param destState
     *            will be the state that receives the next message.
     */
    protected final void transitionTo(IState destState) {
        mSmHandler.transitionTo(destState);
    }

    /**
     * transition to destination state. Upon returning from processMessage the
     * current state's exit will be executed and upon the next message arriving
     * destState.enter will be invoked.
     *
     * this function can also be called inside the enter function of the
     * previous transition target, but the behavior is undefined when it is
     * called mid-way through a previous transition (for example, calling this
     * in the enter() routine of a intermediate node when the current transition
     * target is one of the nodes descendants).
     *
     * @param destState
     *            will be the state that receives the next message.
     */
    protected final void transitionTo(Class<? extends IState> destState) {
        for (IState state : getStates()) {
            if (state.getClass().equals(destState)) {
                mSmHandler.transitionTo(state);
                return;
            }
        }
        throw new RuntimeException("State not found!");
    }

    /**
     * Defer this message until next state transition. Upon transitioning all
     * deferred messages will be placed on the queue and reprocessed in the
     * original order. (i.e. The next state the oldest messages will be
     * processed first)
     *
     * @param msg
     *            is deferred until the next transition.
     */
    protected final void deferMessage(Message msg) {
        mSmHandler.deferMessage(msg);
    }

    /**
     * This will be called before calling {@link IState#enter()}
     */
    protected void addOnStateChangedListener(IOnStateChangedListener onStateChangedListener) {
        mSmHandler.addOnStateChangedListener(onStateChangedListener);
    }

    /**
     * @return the name
     */
    public final String getName() {
        return mName;
    }

    /**
     * Enqueue a message to this state machine.
     */
    public final void sendMessage(int what) {
        mSmHandler.handler.obtainMessage(what).send();
    }

    public final ImmutableMessage obtainMessage(int what) {
        return mSmHandler.handler.obtainMessage(what);
    }

    /**
     * Start the state machine.
     */
    public void start() {
        mSmHandler.completeConstruction(false);
    }

    /**
     * Start the state machine.
     */
    public void resume() {
        mSmHandler.completeConstruction(true);
    }
}

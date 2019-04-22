package com.better.alarm.statemachine;

public abstract class ComplexTransition extends State {
    abstract public void performComplexTransition();

    @Override
    public final void enter() {
        performComplexTransition();
    }

    @Override
    public final void resume() {
        //empty
    }

    @Override
    public final boolean processMessage(Message msg) {
        throw new RuntimeException("performComplexTransition() must transit immediately");
    }

    @Override
    public final void exit() {
        // nothing to do
    }
}

package com.smartisanos.sara.voicecommand;

/**
 * Base class for implementing a stage in the chain of responsibility
 * for processing voice command.
 * <p>
 * Commonds are delivered to the stage by the {@link #deliver} method.  The stage
 * then has the choice of finishing the command or forwarding it to the next stage.
 * </p>
 */
abstract class VoiceCommand {

    interface OnHandleFinishListener {
        void onFinish(boolean isSuccess);
    }

    private OnHandleFinishListener mFinishListener;

    private final VoiceCommand mNext;

    protected static final int FORWARD = 0;
    protected static final int FINISH_HANDLED = 1;
    protected static final int FINISH_HANDLING = 2;
    protected static final int FINISH_NOT_HANDLED = 3;

    /**
     * Creates an input stage.
     *
     * @param next The next stage to which events should be forwarded.
     */
    public VoiceCommand(VoiceCommand next) {
        mNext = next;
    }

    /**
     * Delivers an event to be processed.
     */
    public final int deliver(CharSequence cmd) {
        int result = onProcess(cmd);
        if (result == FORWARD) {
            return forward(cmd);
        } else {
            return result;
        }
    }

    /**
     * Forwards the event to the next stage.
     */
    private int forward(CharSequence cmd) {
        if (mNext != null) {
            return mNext.deliver(cmd);
        } else {
            return FINISH_NOT_HANDLED;
        }
    }

    /**
     * Called when an cmd is ready to be processed.
     *
     * @return A result code indicating how the event was handled.
     */
    protected int onProcess(CharSequence cmd) {
        return FORWARD;
    }

    void setOnHandleFinshListener(OnHandleFinishListener listener) {
        mFinishListener = listener;
    }

    protected void onFinish (boolean isSuccess) {
        if (mFinishListener != null) {
            mFinishListener.onFinish(isSuccess);
        }
    }
}

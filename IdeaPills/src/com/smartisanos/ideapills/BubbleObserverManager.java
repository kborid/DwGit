package com.smartisanos.ideapills;

import com.smartisanos.ideapills.entity.BubbleItem;
import com.smartisanos.ideapills.entity.BubbleObserver;
import com.smartisanos.ideapills.util.LOG;

public class BubbleObserverManager {
    private BubbleObserver sBubbleObserver = null;

    public BubbleObserverManager(){

    }

    public void clearBubbleObserver() {
        LOG.d("clearBubbleObserver:" + sBubbleObserver);
        sBubbleObserver = null;
    }

    public void registerBubbleObserver(BubbleObserver observer) {
//        if (sBubbleObserver != null && observer != null && sBubbleObserver != observer) {
//            throw new RuntimeException("replace sBubbleObserver is not safe here");
//        }
        LOG.d("registerBubbleObserver:" + observer);
        sBubbleObserver = observer;
    }

    public void notifyBubbleObserver(BubbleItem item, int msg) {
        LOG.d("notifyBubbleObserver msg:" + msg);
        if (sBubbleObserver != null) {
            sBubbleObserver.onMessage(msg, item);
        }
    }

    public void unRegisterBubbleObserver(BubbleObserver observer) {
        if (sBubbleObserver == observer) {
            sBubbleObserver = null;
        }
    }
}

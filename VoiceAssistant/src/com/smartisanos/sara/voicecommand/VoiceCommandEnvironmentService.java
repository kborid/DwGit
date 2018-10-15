package com.smartisanos.sara.voicecommand;

import android.app.Service;
import android.content.Intent;
import android.graphics.Rect;
import android.os.IBinder;

public class VoiceCommandEnvironmentService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        return new VoiceCommandEnvironmentImpl().asBinder();
    }

    private static class VoiceCommandEnvironmentImpl extends IVoiceCommandEnvironment.Stub {

        public CharSequence getCurrentPackage() {
            VoiceCommandAccessibilityService service = VoiceCommandAccessibilityService.getInstance();
            if (service != null) {
                return VoiceCommandAccessibilityService.getInstance().getLatestPackage();
            }
            return null;
        }

        public CharSequence getCurrentWindowTitle() {
            VoiceCommandAccessibilityService service = VoiceCommandAccessibilityService.getInstance();
            if (service != null) {
                return VoiceCommandAccessibilityService.getInstance().getLatestWindowTitle();
            }
            return null;
        }

        public Rect getCurrentFocusRect() {
            VoiceCommandAccessibilityService service = VoiceCommandAccessibilityService.getInstance();
            if (service != null) {
                return VoiceCommandAccessibilityService.getInstance().getAccessibilityFocusRect();
            }
            return null;
        }

        public CharSequence getCurrentFocusText() {
            VoiceCommandAccessibilityService service = VoiceCommandAccessibilityService.getInstance();
            if (service != null) {
                return VoiceCommandAccessibilityService.getInstance().getAccessibilityFocusText();
            }
            return null;
        }

        public void clickVoiceButton(String text) {
            VoiceCommandAccessibilityService service = VoiceCommandAccessibilityService.getInstance();
            if (service != null) {
                VoiceCommandAccessibilityService.getInstance().clickVoiceButton(text);
            }
        }
    }
}

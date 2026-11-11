package com.arohi.healthcare.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

/**
 * Universal Sound System and Audio Alert Helper for AROHI Healthcare.
 * Powers emergency sirens, repeating beeps, and audio alerts across:
 * 1. Authority Portal (real-time loud beep alarm when doctor/nurse triggers emergency)
 * 2. Patient, Doctor, and Nurse Portals (audible confirmation & urgent alert chimes)
 */
public final class EmergencyAlertSoundHelper {

    private static ToneGenerator toneGenerator;
    private static Ringtone activeRingtone;
    private static Handler soundLoopHandler;
    private static Runnable soundLoopRunnable;
    private static boolean isBeeping = false;

    private EmergencyAlertSoundHelper() {
        // Utility class
    }

    /**
     * Plays a single short crisp confirmation beep.
     */
    public static synchronized void playShortConfirmationBeep() {
        try {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try {
                    tg.release();
                } catch (Exception ignored) {}
            }, 250);
        } catch (Exception ignored) {}
    }

    /**
     * Starts continuous emergency beep alert alarm (e.g. for Authority portal upon receiving critical staff alert).
     */
    public static synchronized void playEmergencyBeepAlert(Context context) {
        if (isBeeping) return;
        isBeeping = true;

        try {
            if (toneGenerator == null) {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
            }
        } catch (Exception e) {
            toneGenerator = null;
        }

        // Try to obtain system alarm or notification ringtone as complement
        if (context != null) {
            try {
                Uri alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alertUri == null) {
                    alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }
                if (alertUri != null) {
                    activeRingtone = RingtoneManager.getRingtone(context.getApplicationContext(), alertUri);
                }
            } catch (Exception ignored) {}
        }

        if (soundLoopHandler == null) {
            soundLoopHandler = new Handler(Looper.getMainLooper());
        }

        soundLoopRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isBeeping) return;

                try {
                    if (toneGenerator != null) {
                        // Play emergency double-beep pattern (2-second sustained tone)
                        toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 2000);
                    } else if (activeRingtone != null && !activeRingtone.isPlaying()) {
                        activeRingtone.play();
                    }
                } catch (Exception ignored) {}

                if (isBeeping && soundLoopHandler != null) {
                    soundLoopHandler.postDelayed(this, 3000); // Beep every 3 seconds (2s tone + 1s gap)
                }
            }
        };

        soundLoopHandler.post(soundLoopRunnable);
    }

    /**
     * Stops the active emergency beep alarm and releases audio resources.
     */
    public static synchronized void stopEmergencyBeepAlert() {
        isBeeping = false;

        if (soundLoopHandler != null && soundLoopRunnable != null) {
            soundLoopHandler.removeCallbacks(soundLoopRunnable);
            soundLoopRunnable = null;
        }

        if (toneGenerator != null) {
            try {
                toneGenerator.stopTone();
                toneGenerator.release();
            } catch (Exception ignored) {}
            toneGenerator = null;
        }

        if (activeRingtone != null) {
            try {
                if (activeRingtone.isPlaying()) {
                    activeRingtone.stop();
                }
            } catch (Exception ignored) {}
            activeRingtone = null;
        }
    }

    public static synchronized boolean isBeeping() {
        return isBeeping;
    }
}

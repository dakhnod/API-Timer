package d.d.timer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.Serializable;
import java.util.ArrayList;

public class TimerService extends Service {
    final String NOTIFICATION_CHANNEL_ID_RUNNING = "channel_timer_running";
    final String NOTIFICATION_CHANNEL_ID_FINISHED = "channel_timer_finished";
    final int NOTIFICATION_ID_RUNNING = 1;

    final static String ACTION_STOP_RINGING = "d.d.timer.STOP_RINGING";
    final static String ACTION_START_TIMER = "d.d.timer.START_TIMER";
    final static String ACTION_STOP_TIMER = "d.d.timer.STOP_TIMER";
    final static String ACTION_TIMER_FINISHED = "d.d.timer.TIMER_FINISHED";

    ArrayList<Timer> timers = new ArrayList<>();

    NotificationManager notificationManager;

    Notification.Builder notificationBuilder;

    Handler tickHandler = new Handler();

    Runnable tickRunnable = this::handleTick;

    Timer ringingTimer;
    Ringtone alarmRingtone;

    @Override
    public void onCreate() {
        super.onCreate();

        NotificationChannel runningTimerNotificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID_RUNNING,
                "Running timer notification",
                NotificationManager.IMPORTANCE_LOW
        );

        NotificationChannel finishedTimerNotificationChannel = new NotificationChannel(
                NOTIFICATION_CHANNEL_ID_FINISHED,
                "Finished timer notification",
                NotificationManager.IMPORTANCE_DEFAULT
        );

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.createNotificationChannel(runningTimerNotificationChannel);
        notificationManager.createNotificationChannel(finishedTimerNotificationChannel);

        notificationBuilder =
                new Notification.Builder(this, NOTIFICATION_CHANNEL_ID_RUNNING)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentTitle("Timers")
                        .setContentText("initializing timers...");

        registerReceiver(stopRingingReceiver, new IntentFilter(ACTION_STOP_RINGING));

        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        alarmRingtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unregisterReceiver(stopRingingReceiver);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timer newTimer = null;

        if(intent.hasExtra("TIMER")){
            Serializable s = intent.getSerializableExtra("TIMER");
            newTimer = (Timer) intent.getSerializableExtra("TIMER");
        }else if(intent.hasExtra("TIMER_TIME") && intent.hasExtra("TIMER_ID")){

            newTimer = new Timer(intent.getStringExtra("TIMER_ID"), intent.getIntExtra("TIMER_TIME", 0));
        }

        if(newTimer.equals(ringingTimer)){
            alarmRingtone.stop();
            notificationManager.cancel(ringingTimer.getNotificationId());
            ringingTimer = null;
        }

        switch (intent.getAction()) {
            case ACTION_START_TIMER: {
                newTimer.calculateEndTime();

                timers.remove(newTimer);
                timers.add(newTimer);

                timers.sort((a, b) -> Long.compare(a.getEndTime(), b.getEndTime()));

                startForeground(NOTIFICATION_ID_RUNNING, buildTimerNotification());

                notificationManager.cancel(newTimer.getNotificationId());

                tickHandler.removeCallbacks(tickRunnable);

                handleTick();
                break;
            }
            case ACTION_STOP_TIMER: {
                timers.remove(newTimer);
                if (timers.size() == 0 && ringingTimer == null) {
                    stop();
                } else {
                    startForeground(NOTIFICATION_ID_RUNNING, buildTimerNotification());
                }
                break;
            }
        }

        return START_NOT_STICKY;
    }
    
    public void stop(){
        tickHandler.removeCallbacks(tickRunnable);
        stopForeground(true);
    }

    void handleTick() {
        notificationManager.notify(NOTIFICATION_ID_RUNNING, buildTimerNotification());

        long millis = System.currentTimeMillis();

        for (Timer timer : timers) {
            long timerValue = timer.getEndTime();
            if (timerValue <= millis) {
                handleFinishedTimer(timer);
                timers.remove(timer);
            }
        }

        if (timers.size() == 0) {
            Log.d("TimerService", "no timers left");

            if(ringingTimer == null) {
                Log.d("TimerService", "finishing");
                stop();
            }
            return;
        }

        long dif = timers.get(0).getEndTime() - millis;

        long delay = getCountDownInterval(dif);

        delay = dif % delay;

        tickHandler.postDelayed(tickRunnable, delay);

        Log.d("TimerService", "tick   left: " + dif + "    next: " + delay);
    }

    private String formatMillis(long millis) {
        return DateFormat.format(millis > 60 * 60 * 1000 ? "hh:mm:ss" : "mm:ss", millis).toString();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    

    public String buildTimerNotificationText() {
        long millis = System.currentTimeMillis();
        StringBuilder builder = new StringBuilder();
        for (Timer timer : timers) {
            long remaining = timer.getEndTime() - millis;
            builder.append(timer.getId()).append(": ").append(remaining <= 0 ? "ringing..." : formatMillis(remaining)).append("\n");
        }
        return builder.toString();
    }

    private long getCountDownInterval(long time) {
        if (time <= 5 * 60 * 1000) return 1000;
        if (time <= 10 * 60 * 1000) return 10000;
        if (time <= 60 * 60 * 1000) return 30000;
        return 60000;
    }

    private Notification buildTimerNotification() {
        return
                notificationBuilder
                        .setContentText(buildTimerNotificationText())
                        .build();
    }

    private void handleFinishedTimer(Timer finishedTimer) {
        ringingTimer = finishedTimer;

        Intent restartIntent = new Intent(ACTION_START_TIMER);
        restartIntent.setPackage(getPackageName());
        restartIntent.putExtra("TIMER", new Timer(finishedTimer.getId(), finishedTimer.getDuration()));

        PendingIntent restartPendingIntent = PendingIntent.getBroadcast(this, 0, restartIntent, 0);

        PendingIntent stopRingingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_STOP_RINGING), 0);

        Notification timerNotification =
                new Notification.Builder(this, this.NOTIFICATION_CHANNEL_ID_FINISHED)
                        .setSmallIcon(R.mipmap.ic_launcher_round)
                        .setContentText("Timer " + finishedTimer.getId() + " finished")
                        .setContentTitle("ring ring")
                        .setActions(
                                new Notification.Action.Builder(null, "restart", restartPendingIntent)
                                        .build()
                        )
                        .setDeleteIntent(stopRingingIntent)
                        .build();

        notificationManager.notify(finishedTimer.getNotificationId(), timerNotification);

        Intent timerFinishedIntent = new Intent(ACTION_TIMER_FINISHED);
        // timerFinishedIntent.putExtra("TIMER", finishedTimer);
        timerFinishedIntent.putExtra("TIMER_ID", finishedTimer.getId());

        sendBroadcast(timerFinishedIntent);

        alarmRingtone.setVolume(1);
        alarmRingtone.setAudioAttributes(
                new AudioAttributes.Builder(alarmRingtone.getAudioAttributes())
                .setLegacyStreamType(AudioManager.STREAM_ALARM)
                .build()
        );
        alarmRingtone.play();
    }

    BroadcastReceiver stopRingingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ringingTimer = null;
            alarmRingtone.stop();

            if(timers.size() == 0){
                stop();
            }
        }
    };
}

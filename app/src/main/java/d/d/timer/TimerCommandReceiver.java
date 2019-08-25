package d.d.timer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class TimerCommandReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setClass(context, TimerService.class);
        context.startForegroundService(intent);
    }
}

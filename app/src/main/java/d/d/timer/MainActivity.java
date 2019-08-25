package d.d.timer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void startTimer(View view) {
        Intent i = new Intent(TimerService.ACTION_START_TIMER);
        i.putExtra("TIMER", new Timer("Shower", 1000));

        i.setPackage(getPackageName());

        sendBroadcast(i);
    }

    public void stopTimer(View view) {
        Intent i = new Intent(TimerService.ACTION_STOP_TIMER);
        i.putExtra("TIMER_ID", "Shower");
        i.putExtra("TIMER_TIME", 60 * 1000L);

        i.setPackage(getPackageName());

        sendBroadcast(i);
    }
}

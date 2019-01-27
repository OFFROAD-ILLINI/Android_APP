package illini.offroad.bajatimer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TimerActivity extends Activity {
    TimerTalker timer;
    Button button;
    TextView timeView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);
        button = findViewById(R.id.activator);
        timeView = findViewById(R.id.time);

        Intent intent = getIntent();
        String address = intent.getStringExtra(PairActivity.EXTRA_ADDRESS); //receive the address of the bluetooth device
        timer = new TimerTalker(this, address);


    }

    @Override
    protected void onResume() {
        super.onResume();


    }

    @Override
    protected void onPause() {
        super.onPause();
        timer.pause();
    }
    public void getTime() {
        while (true) {
            if (!timer.isActivated())
                continue;
            int time = timer.getTime();
            if (time != 0) {
                timeView.setText(secondsToMinutes(time));
                return;
            }
        }
    }

    public void activateTimer(View view) {
        if (timer.isActivated())
            return;
        //activate the timer in another thread
        new Thread(() -> timer.activate()).start();
        new Thread(() -> {
            button.setText("Activating");
            while (!timer.isActivated()) {
                //waits till the timer is ready to time
            }
            button.setText("Ready");
        }).start();

        //start listening for final time
        new Thread(this::getTime).start();

    }

    private String secondsToMinutes(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;

        String minS = Integer.toString(min);
        String secS = Integer.toString(sec);

        if(minS.length() < 2) minS = "0" + minS;
        if(secS.length() < 2) secS = "0" + secS;

        return minS + ":" + secS;
    }

}



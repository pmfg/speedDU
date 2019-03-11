package pt.pmfg.netspeed;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private Context mContext;
    //private Bitmap bm;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mBuilder;
    private RemoteViews remoteViews;
    private Handler mHandler;
    private Intent notificationIntent;
    private PendingIntent intent;
    int mId = 12;
    int delayThreadMs = 1;
    String download;
    String upload;
    long mRxBytesPrevious = 0;
    long mTxBytesPrevious = 0;
    long totalRx;
    long totalTx;
    TextView mainText;
    Button closeApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;
        mainText = findViewById(R.id.mainText);
        closeApp = findViewById(R.id.exit);
        prepareNotification();

        download = "Down: 0 KB/s | Total: 0 MB";
        upload   = "Up: 0 KB/s | Total: 0 MB";
        updateNotification(download, upload);
        totalRx = 0;
        totalTx = 0;
        mRxBytesPrevious = TrafficStats.getTotalRxBytes();
        mTxBytesPrevious = TrafficStats.getTotalTxBytes();
        prepareRunThread(1000);

        closeApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDestroy();
            }
        });
    }

    @Override
    protected void onDestroy() {
        Log.i("core_ ", "fui");
        stopThread();
        mNotificationManager.cancel(12);
        System.exit(0);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
        {
            Toast.makeText(MainActivity.this, "Running in Background.", Toast.LENGTH_LONG).show();
            this.moveTaskToBack(true);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void prepareRunThread(int delayMs) {
        delayThreadMs = delayMs;
        mHandler = new Handler();
        mStatusSpeedChecker.run();
    }

    private void prepareNotification() {
        /*bm = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.notification_speed),
                getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_width),
                getResources().getDimensionPixelSize(android.R.dimen.notification_large_icon_height),
                true);*/

        notificationIntent = new Intent(mContext, MainActivity.class);
        notificationIntent.addCategory(Intent.CATEGORY_HOME);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);

        remoteViews = new RemoteViews(getPackageName(), R.layout.net_speed_widget);

        mBuilder = new NotificationCompat.Builder(this, "12")
                .setSmallIcon(R.drawable.notification_speed)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(intent)
                .setContent(remoteViews);

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.notify(mId, mBuilder.build());
    }

    private void updateNotification(String download, String upload) {
        remoteViews.setTextViewText(R.id.textDownload, download);
        remoteViews.setTextViewText(R.id.textUpload,upload);
        mBuilder.setContent(remoteViews);
        mNotificationManager.notify(mId, mBuilder.build());
    }

    private void stopThread() {
        mHandler.removeCallbacks(mStatusSpeedChecker);
    }

    Runnable mStatusSpeedChecker = new Runnable() {
        @Override
        public void run() {
            try {
                download = calculateSpeedDownload();
                upload = calculateSpeedUpload();
                updateNotification(download, upload);
                mainText.setText(download+"\n"+upload);
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusSpeedChecker, delayThreadMs);
            }
        }
    };

    private String calculateSpeedDownload() {
        long mDownloadSpeedRx = TrafficStats.getTotalRxBytes() - mRxBytesPrevious;
        float mDownloadSpeedWithDecimalsRx;
        String mUnits;
        totalRx = totalRx + mDownloadSpeedRx;
        if (mDownloadSpeedRx >= 1000000000) {
            mDownloadSpeedWithDecimalsRx = (float) mDownloadSpeedRx / (float) 1000000000;
            mUnits = " GB/s";
        } else if (mDownloadSpeedRx >= 1000000) {
            mDownloadSpeedWithDecimalsRx = (float) mDownloadSpeedRx / (float) 1000000;
            mUnits = " MB/s";

        } else if (mDownloadSpeedRx >= 1000) {
            mDownloadSpeedWithDecimalsRx = (float) mDownloadSpeedRx / (float) 1000;
            mUnits = " KB/s";
        } else {
            mDownloadSpeedWithDecimalsRx = (float) mDownloadSpeedRx;
            mUnits = " B/s";
        }
        mRxBytesPrevious = TrafficStats.getTotalRxBytes();
        return "Down: " + String.format(Locale.US, "%.1f", mDownloadSpeedWithDecimalsRx) + mUnits + " | Total: "+getTotalValue(totalRx);
    }

    private String getTotalValue(long total) {
        float result;
        String mUnits;
        if (total >= 1000000000) {
            result = (float) total / (float) 1000000000;
            mUnits = " GB";
        } else if (total >= 1000000) {
            result = (float) total / (float) 1000000;
            mUnits = " MB";

        } else if (total >= 1000) {
            result = (float) total / (float) 1000;
            mUnits = " KB";
        } else {
            result = (float) total;
            mUnits = " B";
        }

        return String.format(Locale.US, "%.2f", result) + mUnits;
    }

    private String calculateSpeedUpload() {
        long mDownloadSpeedTx = TrafficStats.getTotalTxBytes() - mTxBytesPrevious;
        float mDownloadSpeedWithDecimalsTx;
        String mUnits;
        totalTx = totalTx + mDownloadSpeedTx;
        if (mDownloadSpeedTx >= 1000000000) {
            mDownloadSpeedWithDecimalsTx = (float) mDownloadSpeedTx / (float) 1000000000;
            mUnits = " GB/s";
        } else if (mDownloadSpeedTx >= 1000000) {
            mDownloadSpeedWithDecimalsTx = (float) mDownloadSpeedTx / (float) 1000000;
            mUnits = " MB/s";

        } else if (mDownloadSpeedTx >= 1000) {
            mDownloadSpeedWithDecimalsTx = (float) mDownloadSpeedTx / (float) 1000;
            mUnits = " KB/s";
        } else {
            mDownloadSpeedWithDecimalsTx = (float) mDownloadSpeedTx;
            mUnits = " B/s";
        }
        mTxBytesPrevious = TrafficStats.getTotalTxBytes();
        return "Up     : " + String.format(Locale.US, "%.1f", mDownloadSpeedWithDecimalsTx) + mUnits + " | Total: "+getTotalValue(totalTx);
    }
}

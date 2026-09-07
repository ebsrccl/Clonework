package id.mas.agent;

import android.app.*;
import android.content.Intent;
import android.os.*;

/** Keeps the app process available while the user completes browser login or a chat task. */
public final class AgentService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timeout = this::stopSelf;
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("agent_tasks", "Tugas agen lokal", NotificationManager.IMPORTANCE_LOW));
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification note = new Notification.Builder(this, "agent_tasks").setSmallIcon(id.mas.agent.R.drawable.ic_agent)
            .setContentTitle("MikroTik Agent").setContentText("Login atau tugas ChatGPT sedang aktif di HP.")
            .setContentIntent(open).setOngoing(true).build();
        startForeground(3, note);
        handler.removeCallbacks(timeout); handler.postDelayed(timeout, 600000);
        return START_NOT_STICKY;
    }
    @Override public void onDestroy() { handler.removeCallbacks(timeout); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}

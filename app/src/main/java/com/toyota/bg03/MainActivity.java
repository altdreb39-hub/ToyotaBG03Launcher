package com.toyota.bg03;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.media.AudioManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private DashboardView dashboard;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        dashboard = new DashboardView();
        setContentView(dashboard);
    }

    private void openMaps() {
        Intent map = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="));
        map.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(map); } catch (Exception e) { openAppList(); }
    }

    private void openBluetooth() {
        try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
        catch (Exception e) { openAppList(); }
    }

    private void openSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch (Exception e) { openAppList(); }
    }

    private void mediaKey(int keyCode) {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        long now = System.currentTimeMillis();
        KeyEvent down = new KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0);
        KeyEvent up = new KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0);
        if (am != null) { am.dispatchMediaKeyEvent(down); am.dispatchMediaKeyEvent(up); }
    }

    private void openAppList() {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(query, 0);
        final ArrayList<ResolveInfo> sorted = new ArrayList<>(apps);
        sorted.sort((a,b) -> String.valueOf(a.loadLabel(pm)).compareToIgnoreCase(String.valueOf(b.loadLabel(pm))));
        ArrayList<String> labels = new ArrayList<>();
        for (ResolveInfo info : sorted) labels.add(String.valueOf(info.loadLabel(pm)));
        ListView list = new ListView(this);
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("تطبيقات الجهاز").setView(list)
                .setNegativeButton("إغلاق", null).create();
        list.setOnItemClickListener((parent, view, position, id) -> {
            Intent launch = pm.getLaunchIntentForPackage(sorted.get(position).activityInfo.packageName);
            if (launch != null) startActivity(launch);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void launchByName(String... terms) {
        PackageManager pm = getPackageManager();
        Intent query = new Intent(Intent.ACTION_MAIN, null);
        query.addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo r : pm.queryIntentActivities(query, 0)) {
            String label = String.valueOf(r.loadLabel(pm)).toLowerCase(Locale.ROOT);
            for (String term : terms) {
                if (label.contains(term.toLowerCase(Locale.ROOT))) {
                    Intent i = pm.getLaunchIntentForPackage(r.activityInfo.packageName);
                    if (i != null) { startActivity(i); return; }
                }
            }
        }
        openAppList();
    }

    private class DashboardView extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Bitmap bg;
        private final RectF dst = new RectF();
        private float sx, sy;
        private long lastTap;
        DashboardView() {
            super(MainActivity.this);
            BitmapFactory.Options opts = new BitmapFactory.Options(); opts.inPreferredConfig = Bitmap.Config.RGB_565;
            bg = BitmapFactory.decodeStream(getAssetsStream(), null, opts);
        }
        private java.io.InputStream getAssetsStream() {
            try { return getAssets().open("dashboard.png"); } catch (Exception e) { return null; }
        }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (bg != null) c.drawBitmap(bg, null, new RectF(0,0,getWidth(),getHeight()), p);
            sx = getWidth() / 1672f; sy = getHeight() / 941f;
            // Replace the artwork's sample clock/date with the live device time.
            float l=78*sx, t=112*sy, r=590*sx, b=338*sy;
            p.setShader(new LinearGradient(l,t,r,b, new int[]{0xee020b19,0xe9081d35,0xee020914}, null, Shader.TileMode.CLAMP));
            p.setStyle(Paint.Style.FILL); c.drawRoundRect(new RectF(l,t,r,b), 34*sx, 34*sx, p); p.setShader(null);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3*sx); p.setColor(Color.rgb(0,160,255));
            c.drawRoundRect(new RectF(l,t,r,b),34*sx,34*sx,p); p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE); p.setTextAlign(Paint.Align.LEFT); p.setTypeface(android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD));
            p.setTextSize(125*sx); c.drawText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()), 125*sx, 236*sy, p);
            p.setTextAlign(Paint.Align.RIGHT); p.setTextSize(37*sx);
            String date = new SimpleDateFormat("EEEE d MMMM yyyy", new Locale("ar", "IQ")).format(new Date());
            c.drawText(date, 548*sx, 305*sy, p);
            postInvalidateDelayed(30000);
        }
        @Override public boolean onTouchEvent(android.view.MotionEvent e) {
            if (e.getAction() != android.view.MotionEvent.ACTION_UP) return true;
            float x=e.getX()/sx, y=e.getY()/sy;
            if (y>600 && y<800) {
                if (x<240) openAppList();
                else if (x<480) openMaps();
                else if (x<720) openBluetooth();
                else if (x<960) launchByName("radio", "fm");
                else if (x<1200) launchByName("video", "player", "youtube");
                else if (x<1440) launchByName("music", "audio");
                else openSettings();
            } else if (y>455 && y<590 && x<620) {
                if (x<370) mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
                else if (x<505) mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
                else mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT);
            } else if (y>820 && x>1200) {
                try { startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS)); } catch(Exception ignored) {}
            }
            return true;
        }
    }
}

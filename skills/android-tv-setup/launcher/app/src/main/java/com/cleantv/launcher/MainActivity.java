package com.cleantv.launcher;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {

    private static final int TOP = 0, DOCK = 1, GRID = 2, MENU = 3;
    private static final float LIFT = 1.08f;

    private static final class Input {
        String id;
        CharSequence label;
        boolean tuner, hdmi, connected;
    }

    private AppRepository repo = new AppRepository();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final List<Input> inputs = new ArrayList<>();

    private FrameLayout root;
    private ScrollView scroll;
    private View hero, dockBand, glow, bottomSpace;
    private RecyclerView dockRv, gridRv;
    private TextView greeting, clock, date, dockTitle, gridTitle;
    private TextView[] pills;
    private LinearLayout inputsMenu;
    private GlassHighlight highlight;
    private ImageView wallpaper;

    private float dp, radius;
    private int columns, cardH, gap;
    private Typeface thin, light, regular;

    private int zone = DOCK, topIdx, dockIdx, gridIdx, menuIdx;
    private View lifted;
    private ObjectAnimator scrollAnim;
    private ValueAnimator glowAnim;
    private int glowColor = Color.TRANSPARENT;
    private boolean resumed, dpiLaunching;
    private SkeletonAdapter.Shimmer shimmer;
    private List<String> dpiApps;
    private SharedPreferences prefs;
    private ConnectivityManager cm;

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            updateClock();
        }
    };

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            reloadApps();
        }
    };

    private final ConnectivityManager.NetworkCallback netCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onCapabilitiesChanged(Network n, NetworkCapabilities caps) {
            main.post(() -> updateNetwork(caps));
        }

        @Override
        public void onLost(Network n) {
            main.post(() -> updateNetwork(null));
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        dp = getResources().getDisplayMetrics().density;
        columns = getResources().getInteger(R.integer.columns);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        dpiApps = Arrays.asList(getResources().getStringArray(R.array.dpi_apps));

        root = findViewById(R.id.root);
        scroll = findViewById(R.id.scroll);
        hero = findViewById(R.id.hero);
        dockBand = findViewById(R.id.dockBand);
        glow = findViewById(R.id.glow);
        bottomSpace = findViewById(R.id.bottomSpace);
        dockRv = findViewById(R.id.dock);
        gridRv = findViewById(R.id.grid);
        greeting = findViewById(R.id.greeting);
        clock = findViewById(R.id.clock);
        date = findViewById(R.id.date);
        dockTitle = findViewById(R.id.dockTitle);
        gridTitle = findViewById(R.id.gridTitle);
        inputsMenu = findViewById(R.id.inputsMenu);
        highlight = findViewById(R.id.highlight);
        wallpaper = findViewById(R.id.wallpaper);
        pills = new TextView[]{findViewById(R.id.pillInputs), findViewById(R.id.pillWifi), findViewById(R.id.pillSettings)};

        thin = font(R.integer.clock_weight);
        light = font(R.integer.title_weight);
        regular = font(R.integer.body_weight);
        clock.setTypeface(thin);
        dockTitle.setTypeface(light);
        for (TextView t : new TextView[]{greeting, date, gridTitle}) t.setTypeface(regular);

        layoutGeometry();
        styleChrome();
        loadWallpaper();
        showSkeletons();
        reloadApps();

        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pf.addDataScheme("package");
        registerReceiver(packageReceiver, pf);

        cm = getSystemService(ConnectivityManager.class);
        cm.registerDefaultNetworkCallback(netCallback);
        updateNetwork(cm.getNetworkCapabilities(cm.getActiveNetwork()));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(packageReceiver);
        cm.unregisterNetworkCallback(netCallback);
        if (shimmer != null) shimmer.stop();
        io.shutdown();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        IntentFilter tf = new IntentFilter(Intent.ACTION_TIME_TICK);
        tf.addAction(Intent.ACTION_TIME_CHANGED);
        tf.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        registerReceiver(timeReceiver, tf);
        updateClock();
        // Back home from an app that needed the DPI bypass: switch it off. The flag lives in prefs
        // so it survives the launcher being killed while the app was open.
        if (!dpiLaunching && prefs.getBoolean("dpi_on", false)) {
            prefs.edit().putBoolean("dpi_on", false).apply();
            dpi("only_stop");
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        unregisterReceiver(timeReceiver);
        rememberFocus();
        super.onPause();
    }

    /** Home pressed while already home: close menus and return to the dock. */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!resumed) return;
        if (zone == MENU) hideMenu();
        if (!repo.dock.isEmpty()) zone = DOCK;
        applyFocus(true);
    }

    @Override
    public void onBackPressed() {
        // Home has nowhere to go back to.
    }

    // ---- layout ----

    private Typeface font(int weightRes) {
        try {
            return new Typeface.Builder(getAssets(), getString(R.string.font_file))
                    .setFontVariationSettings("'wght' " + getResources().getInteger(weightRes))
                    .build();
        } catch (RuntimeException e) {
            return Typeface.DEFAULT;
        }
    }

    private void layoutGeometry() {
        int screenW = getResources().getDisplayMetrics().widthPixels;
        int screenH = getResources().getDisplayMetrics().heightPixels;
        int side = px(64), bandMargin = px(40);
        gap = px(28);
        radius = px(14);
        int tileW = (screenW - 2 * side - (columns - 1) * gap) / columns;
        cardH = tileW * 9 / 16;

        int dockPad = side - bandMargin - gap / 2;
        dockRv.setPadding(dockPad, 0, dockPad, 0);
        gridRv.setPadding(side - gap / 2, 0, side - gap / 2, 0);
        for (RecyclerView rv : new RecyclerView[]{dockRv, gridRv}) {
            rv.setLayoutManager(new GridLayoutManager(this, columns));
            rv.setNestedScrollingEnabled(false);
            rv.setItemAnimator(null);
            rv.setFocusable(false);
            rv.setDescendantFocusability(RecyclerView.FOCUS_BLOCK_DESCENDANTS);
        }

        int labelSpace = px(12) + px(26);
        int bandH = dockBand.getPaddingTop() + dockBand.getPaddingBottom() + cardH + labelSpace;
        hero.getLayoutParams().height = screenH - bandH - px(30);
        bottomSpace.getLayoutParams().height = screenH;
    }

    private void styleChrome() {
        GradientDrawable band = new GradientDrawable();
        band.setColor(0xD9161619);
        band.setCornerRadius(px(30));
        band.setStroke(Math.max(1, px(1) / 2), 0x14FFFFFF);
        dockBand.setBackground(band);

        for (TextView p : pills) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x24FFFFFF);
            bg.setCornerRadius(px(24));
            p.setBackground(bg);
            p.setTypeface(regular);
        }

        GradientDrawable menu = new GradientDrawable();
        menu.setColor(0xFA1C1C1F);
        menu.setCornerRadius(px(22));
        menu.setStroke(px(1), 0x26FFFFFF);
        inputsMenu.setBackground(menu);

        // A circular radial gradient stretched horizontally reads as a soft ambient glow.
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        g.setGradientRadius(px(210));
        g.setColors(new int[]{Color.TRANSPARENT, Color.TRANSPARENT});
        glow.setBackground(g);
        glow.getLayoutParams().width = px(420);
        glow.getLayoutParams().height = px(420);
        ((FrameLayout.LayoutParams) glow.getLayoutParams()).setMarginStart(px(60));
        ((FrameLayout.LayoutParams) glow.getLayoutParams()).bottomMargin = -px(170);
        glow.setScaleX(2.3f);
    }

    private void loadWallpaper() {
        File f = new File(getExternalFilesDir(null), getString(R.string.wallpaper_file));
        if (!f.isFile()) return;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        int target = getResources().getDisplayMetrics().widthPixels;
        o.inSampleSize = 1;
        while (o.outWidth / (o.inSampleSize * 2) >= target) o.inSampleSize *= 2;
        o.inJustDecodeBounds = false;
        o.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap b = BitmapFactory.decodeFile(f.getPath(), o);
        if (b == null) return;
        wallpaper.setImageBitmap(b);
        wallpaper.setColorFilter(Color.argb(getResources().getInteger(R.integer.wallpaper_dim), 0, 0, 0),
                PorterDuff.Mode.SRC_ATOP);
        wallpaper.setVisibility(View.VISIBLE);
    }

    private int px(float v) {
        return Math.round(v * dp);
    }

    // ---- data ----

    private void reloadApps() {
        io.execute(() -> {
            AppRepository r = new AppRepository();
            r.load(this);
            main.post(() -> showApps(r));
        });
    }

    /** Cold start: placeholder plates in the last known counts, so the first frame is never an empty band. */
    private void showSkeletons() {
        int dockN = prefs.getInt("dock_n", columns), gridN = prefs.getInt("grid_n", columns);
        shimmer = new SkeletonAdapter.Shimmer();
        dockRv.setAdapter(new SkeletonAdapter(shimmer, dockN, cardH, gap, radius));
        gridRv.setAdapter(new SkeletonAdapter(shimmer, gridN, cardH, gap, radius));
        gridTitle.setVisibility(gridN == 0 ? View.GONE : View.VISIBLE);
        shimmer.start();
    }

    private void showApps(AppRepository r) {
        boolean first = shimmer != null;
        if (first) {
            shimmer.stop();
            shimmer = null;
        }
        repo = r;
        lifted = null;
        dockRv.setAdapter(new TileAdapter(repo.dock, cardH, gap, radius, regular, first));
        gridRv.setAdapter(new TileAdapter(repo.grid, cardH, gap, radius, regular, first));
        gridTitle.setVisibility(repo.grid.isEmpty() ? View.GONE : View.VISIBLE);
        prefs.edit().putInt("dock_n", repo.dock.size()).putInt("grid_n", repo.grid.size()).apply();
        if (zone != MENU && zone != TOP) restoreFocus();
        afterLayout(() -> applyFocus(false));
    }

    private void restoreFocus() {
        String pkg = prefs.getString("pkg", null);
        zone = repo.dock.isEmpty() ? (repo.grid.isEmpty() ? TOP : GRID) : DOCK;
        dockIdx = Math.min(dockIdx, Math.max(0, repo.dock.size() - 1));
        gridIdx = Math.min(gridIdx, Math.max(0, repo.grid.size() - 1));
        if (pkg == null) return;
        for (int i = 0; i < repo.dock.size(); i++)
            if (repo.dock.get(i).pkg.equals(pkg)) {
                zone = DOCK;
                dockIdx = i;
                return;
            }
        for (int i = 0; i < repo.grid.size(); i++)
            if (repo.grid.get(i).pkg.equals(pkg)) {
                zone = GRID;
                gridIdx = i;
                return;
            }
    }

    private void rememberFocus() {
        AppRepository.App a = focusedApp();
        if (a != null) prefs.edit().putString("pkg", a.pkg).apply();
    }

    private AppRepository.App focusedApp() {
        if (zone == DOCK && dockIdx < repo.dock.size()) return repo.dock.get(dockIdx);
        if (zone == GRID && gridIdx < repo.grid.size()) return repo.grid.get(gridIdx);
        return null;
    }

    private void afterLayout(Runnable r) {
        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                root.getViewTreeObserver().removeOnPreDrawListener(this);
                r.run();
                return true;
            }
        });
        root.invalidate();
    }

    // ---- keys and focus ----

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        boolean ours;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_BACK:
                ours = true;
                break;
            default:
                ours = false;
        }
        if (!ours) return super.dispatchKeyEvent(e);
        if (e.getAction() != KeyEvent.ACTION_DOWN) return true;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: move(-1, 0); break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: move(1, 0); break;
            case KeyEvent.KEYCODE_DPAD_UP: move(0, -1); break;
            case KeyEvent.KEYCODE_DPAD_DOWN: move(0, 1); break;
            case KeyEvent.KEYCODE_BACK: back(); break;
            default:
                if (e.getRepeatCount() == 0) activate();
        }
        return true;
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private int rowLength(int row) {
        return Math.min(columns, repo.grid.size() - row * columns);
    }

    private void move(int dx, int dy) {
        switch (zone) {
            case TOP:
                if (dx != 0) topIdx = clamp(topIdx + dx, 0, pills.length - 1);
                else if (dy > 0 && !repo.dock.isEmpty()) zone = DOCK;
                else if (dy > 0 && !repo.grid.isEmpty()) zone = GRID;
                break;
            case DOCK:
                if (dx != 0) dockIdx = clamp(dockIdx + dx, 0, repo.dock.size() - 1);
                else if (dy < 0) zone = TOP;
                else if (!repo.grid.isEmpty()) {
                    zone = GRID;
                    gridIdx = Math.min(dockIdx % columns, rowLength(0) - 1);
                }
                break;
            case GRID: {
                int row = gridIdx / columns, col = gridIdx % columns;
                int rows = (repo.grid.size() + columns - 1) / columns;
                if (dx != 0) {
                    gridIdx = row * columns + clamp(col + dx, 0, rowLength(row) - 1);
                } else if (dy < 0) {
                    if (row > 0) gridIdx = (row - 1) * columns + col;
                    else if (!repo.dock.isEmpty()) {
                        zone = DOCK;
                        dockIdx = Math.min(col, repo.dock.size() - 1);
                    } else zone = TOP;
                } else if (row + 1 < rows) {
                    gridIdx = (row + 1) * columns + Math.min(col, rowLength(row + 1) - 1);
                }
                break;
            }
            case MENU:
                if (dy != 0) menuIdx = clamp(menuIdx + dy, 0, inputsMenu.getChildCount() - 1);
                break;
        }
        applyFocus(true);
    }

    private void back() {
        if (zone == MENU) {
            hideMenu();
            zone = TOP;
        } else if (zone == GRID || zone == TOP) {
            zone = repo.dock.isEmpty() ? zone : DOCK;
        }
        applyFocus(true);
    }

    private void activate() {
        switch (zone) {
            case TOP:
                if (topIdx == 0) showInputs();
                else if (topIdx == 1) start(new Intent(Settings.ACTION_WIFI_SETTINGS));
                else start(new Intent(Settings.ACTION_SETTINGS));
                break;
            case DOCK:
            case GRID:
                AppRepository.App a = focusedApp();
                if (a != null) {
                    rememberFocus();
                    if (dpiApps.contains(a.pkg)) startWithDpi(a.intent);
                    else start(a.intent);
                }
                break;
            case MENU:
                if (menuIdx < inputs.size()) switchInput(inputs.get(menuIdx));
                hideMenu();
                zone = TOP;
                applyFocus(false);
                break;
        }
    }

    private void start(Intent i) {
        try {
            startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, e.getClass().getSimpleName(), Toast.LENGTH_SHORT).show();
        }
    }

    /** Start the DPI bypass, give its VPN a moment to come up, then open the app. */
    private void startWithDpi(Intent app) {
        // The toggle activity finishes at once and resumes us; dpiLaunching keeps onResume from stopping it.
        dpiLaunching = true;
        dpi("only_start");
        main.postDelayed(() -> {
            prefs.edit().putBoolean("dpi_on", true).apply();
            dpiLaunching = false;
            start(app);
        }, getResources().getInteger(R.integer.dpi_delay_ms));
    }

    /** ByeByeDPI's exported ToggleActivity takes only_start / only_stop extras. */
    private void dpi(String action) {
        ComponentName c = ComponentName.unflattenFromString(getString(R.string.dpi_toggle));
        start(new Intent().setComponent(c).putExtra(action, true));
    }

    /** The view the glass plate should cover for the current focus. */
    private View focusTarget() {
        switch (zone) {
            case TOP: return pills[topIdx];
            case DOCK: return card(dockRv, dockIdx);
            case GRID: return card(gridRv, gridIdx);
            case MENU: return inputsMenu.getChildAt(menuIdx);
        }
        return null;
    }

    private static View card(RecyclerView rv, int pos) {
        RecyclerView.LayoutManager lm = rv.getLayoutManager();
        View item = lm == null ? null : lm.findViewByPosition(pos);
        return item == null ? null : item.findViewById(R.id.card);
    }

    private void applyFocus(boolean animate) {
        // Placeholders take no focus; the top bar does, it is real from the first frame.
        if (shimmer != null && zone != TOP && zone != MENU) return;
        View target = focusTarget();
        if (target == null) return;
        if (lifted != null && lifted != target) lift(lifted, false);
        lift(target, true);
        lifted = target;

        int scrollTo = scrollTarget();
        if (scrollAnim != null) scrollAnim.cancel();
        if (scroll.getScrollY() != scrollTo) {
            if (animate) {
                scrollAnim = ObjectAnimator.ofInt(scroll, "scrollY", scrollTo);
                scrollAnim.setDuration(380);
                scrollAnim.setInterpolator(new DecelerateInterpolator(2f));
                scrollAnim.start();
            } else {
                scroll.scrollTo(0, scrollTo);
            }
        }

        Rect r = new Rect(0, 0, target.getWidth(), target.getHeight());
        root.offsetDescendantRectToMyCoords(target, r);
        if (zone != MENU) r.offset(0, scroll.getScrollY() - scrollTo);
        RectF plate = new RectF(r);
        float corner;
        if (zone == DOCK || zone == GRID) {
            plate.inset(-(r.width() * (LIFT - 1) / 2 + px(6)), -(r.height() * (LIFT - 1) / 2 + px(6)));
            corner = radius * LIFT + px(6);
        } else if (zone == MENU) {
            plate.inset(px(8), px(1));
            corner = px(14);
        } else {
            corner = r.height() / 2f;
        }
        highlight.moveTo(plate, corner, animate);
        updateDockTitle();
    }

    private void lift(View v, boolean on) {
        if (v.getId() == R.id.card) {
            v.animate().scaleX(on ? LIFT : 1).scaleY(on ? LIFT : 1).setDuration(220)
                    .setInterpolator(new DecelerateInterpolator()).start();
            View label = ((View) v.getParent()).findViewById(R.id.label);
            label.animate().alpha(on ? 1 : 0).setDuration(on ? 220 : 120).start();
        } else if (v instanceof TextView) {
            ((TextView) v).setTextColor(on ? 0xFFFFFFFF : 0xCCFFFFFF);
        }
    }

    private int scrollTarget() {
        if (zone != GRID) return 0;
        RecyclerView.LayoutManager lm = gridRv.getLayoutManager();
        View item = lm == null ? null : lm.findViewByPosition(gridIdx);
        if (item == null) return scroll.getScrollY();
        return Math.max(0, gridRv.getTop() + item.getTop() - px(150));
    }

    private void updateDockTitle() {
        boolean show = zone == DOCK && dockIdx < repo.dock.size();
        if (show) {
            AppRepository.App a = repo.dock.get(dockIdx);
            if (!a.label.equals(dockTitle.getText())) {
                dockTitle.setText(a.label);
                dockTitle.setAlpha(0.3f);
                dockTitle.setTranslationY(px(6));
            }
            animateGlow(a.color);
        }
        dockTitle.animate().alpha(show ? 1 : 0).translationY(0).setDuration(show ? 260 : 160).start();
        glow.animate().alpha(show ? 1 : 0).setDuration(show ? 420 : 200).start();
    }

    private void animateGlow(int color) {
        if (color == glowColor) return;
        if (glowAnim != null) glowAnim.cancel();
        int fromColor = glowColor == Color.TRANSPARENT ? color : glowColor;
        glowColor = color;
        GradientDrawable g = (GradientDrawable) glow.getBackground();
        glowAnim = ValueAnimator.ofObject(new ArgbEvaluator(), fromColor, color);
        glowAnim.setDuration(420);
        glowAnim.addUpdateListener(a -> {
            int c = (int) a.getAnimatedValue();
            g.setColors(new int[]{(c & 0x00FFFFFF) | 0x59000000, (c & 0x00FFFFFF) | 0x1A000000, Color.TRANSPARENT});
        });
        glowAnim.start();
    }

    // ---- top bar ----

    private void updateClock() {
        Date now = new Date();
        boolean h24 = DateFormat.is24HourFormat(this);
        clock.setText(DateFormat.format(h24 ? "HH:mm" : "h:mm", now));
        String d = new java.text.SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now);
        date.setText(d.isEmpty() ? d : Character.toUpperCase(d.charAt(0)) + d.substring(1));
        int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        greeting.setText(h >= 5 && h < 12 ? R.string.good_morning
                : h >= 12 && h < 18 ? R.string.good_day
                : h >= 18 && h < 23 ? R.string.good_evening : R.string.good_night);
    }

    private void updateNetwork(NetworkCapabilities caps) {
        TextView p = pills[1];
        if (caps == null) {
            p.setText(R.string.offline);
        } else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
            p.setText(R.string.ethernet);
        } else {
            WifiInfo info = getSystemService(WifiManager.class).getConnectionInfo();
            String ssid = info == null ? null : info.getSSID();
            boolean known = ssid != null && !ssid.isEmpty() && !ssid.equals(WifiManager.UNKNOWN_SSID);
            p.setText(known ? ssid.replaceAll("^\"|\"$", "") : getString(R.string.wifi));
        }
        if (zone == TOP) afterLayout(() -> applyFocus(false));
    }

    // ---- inputs ----

    private void loadInputs() {
        inputs.clear();
        TvInputManager tim = getSystemService(TvInputManager.class);
        if (tim == null) return;
        Input tuner = null;
        for (TvInputInfo info : tim.getTvInputList()) {
            if (info.getParentId() != null || info.isHidden(this)) continue;
            Input in = new Input();
            in.id = info.getId();
            in.connected = tim.getInputState(in.id) != TvInputManager.INPUT_STATE_DISCONNECTED;
            if (info.getType() == TvInputInfo.TYPE_TUNER) {
                // One Live TV entry, from the built-in tuner only (apps may register tuner inputs too).
                boolean system = (info.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                if (tuner != null || !system) continue;
                in.tuner = true;
                in.connected = true;
                in.label = getString(R.string.live_tv);
                tuner = in;
                continue;
            }
            if (!info.isPassthroughInput()) continue;
            in.hdmi = info.getType() == TvInputInfo.TYPE_HDMI;
            CharSequence custom = info.loadCustomLabel(this);
            in.label = custom != null ? custom
                    : info.getType() == TvInputInfo.TYPE_COMPOSITE ? getString(R.string.av) : info.loadLabel(this);
            inputs.add(in);
        }
        Collator col = Collator.getInstance();
        // HDMI ports first, then AV and the rest, Live TV last.
        inputs.sort((a, b) -> a.hdmi != b.hdmi ? (a.hdmi ? -1 : 1) : col.compare(a.label.toString(), b.label.toString()));
        if (tuner != null) inputs.add(tuner);
    }

    private void showInputs() {
        loadInputs();
        if (inputs.isEmpty()) return;
        inputsMenu.removeAllViews();
        for (Input in : inputs) {
            TextView t = new TextView(this);
            t.setText(in.label);
            t.setTypeface(regular);
            t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
            t.setTextColor(0xCCFFFFFF);
            t.setAlpha(in.connected ? 1f : 0.45f);
            t.setGravity(Gravity.CENTER_VERTICAL);
            t.setPadding(px(24), 0, px(24), 0);
            inputsMenu.addView(t, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(44)));
        }
        Rect pill = new Rect(0, 0, pills[0].getWidth(), pills[0].getHeight());
        root.offsetDescendantRectToMyCoords(pills[0], pill);
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) inputsMenu.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = Math.min(pill.left, root.getWidth() - px(24) - lp.width);
        lp.topMargin = pill.bottom + px(12);
        inputsMenu.setLayoutParams(lp);
        inputsMenu.setVisibility(View.VISIBLE);
        inputsMenu.setAlpha(0);
        inputsMenu.setTranslationY(-px(8));
        inputsMenu.animate().alpha(1).translationY(0).setDuration(200).start();
        zone = MENU;
        menuIdx = 0;
        for (int i = 0; i < inputs.size(); i++)
            if (inputs.get(i).connected) {
                menuIdx = i;
                break;
            }
        lifted = null;
        lift(pills[0], false);
        afterLayout(() -> applyFocus(false));
    }

    private void hideMenu() {
        inputsMenu.animate().alpha(0).setDuration(140).withEndAction(() -> inputsMenu.setVisibility(View.GONE)).start();
        lifted = null;
    }

    private void switchInput(Input in) {
        if (in.tuner) {
            Intent tv = getPackageManager().getLeanbackLaunchIntentForPackage(getString(R.string.live_tv_package));
            if (tv != null) start(tv);
            return;
        }
        start(new Intent(Intent.ACTION_VIEW, TvContract.buildChannelUriForPassthroughInput(in.id)));
    }
}

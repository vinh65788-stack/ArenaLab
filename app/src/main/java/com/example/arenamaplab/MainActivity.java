package com.example.arenamaplab;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "arena_map_lab_store";
    private static final String KEY_MARKERS = "markers";
    private static final String KEY_NOTES = "notes";

    private final int BG = Color.rgb(13, 23, 25);
    private final int PANEL = Color.rgb(21, 34, 35);
    private final int PANEL_ALT = Color.rgb(27, 43, 42);
    private final int TEXT = Color.rgb(238, 239, 226);
    private final int MUTED = Color.rgb(155, 171, 160);
    private final int GOLD = Color.rgb(214, 182, 109);
    private final int GREEN = Color.rgb(106, 194, 143);
    private final int RED = Color.rgb(236, 112, 101);
    private final int BLUE = Color.rgb(104, 174, 226);

    private final ArrayList<MapMarker> markers = new ArrayList<>();
    private final ArrayList<TimerBlock> timers = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timerTick;
    private MapCanvas mapCanvas;
    private TextView markerStatus;
    private String activeTool = "PING";
    private EditText notesInput;
    private final ArrayList<TextView> timerDisplays = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        loadMarkers();
        setupTimers();
        buildScreen();
        startTimerLoop();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(BG);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), dp(18), dp(18), dp(26));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        page.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout titleGroup = new LinearLayout(this);
        titleGroup.setOrientation(LinearLayout.VERTICAL);
        header.addView(titleGroup, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView title = text("ARENA MAP LAB", 22, TEXT, true);
        title.setLetterSpacing(0.045f);
        titleGroup.addView(title);
        TextView subtitle = text("BẢN ĐỒ CHIẾN THUẬT  •  OFFLINE", 10, MUTED, true);
        subtitle.setLetterSpacing(0.09f);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-2, -2);
        subParams.topMargin = dp(4);
        titleGroup.addView(subtitle, subParams);

        TextView offline = text("NO KEY\nNO LOGIN", 9, GOLD, true);
        offline.setGravity(Gravity.CENTER);
        offline.setPadding(dp(11), dp(8), dp(11), dp(8));
        offline.setBackground(shape(PANEL_ALT, GOLD, 1, 12));
        header.addView(offline);

        TextView intro = text("Bàn thao tác riêng để lên kế hoạch và ghi dấu thủ công.", 12, MUTED, false);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(-1, -2);
        introParams.topMargin = dp(10);
        page.addView(intro, introParams);

        LinearLayout mapCard = card();
        LinearLayout.LayoutParams mapCardParams = new LinearLayout.LayoutParams(-1, -2);
        mapCardParams.topMargin = dp(18);
        page.addView(mapCard, mapCardParams);

        LinearLayout mapHead = new LinearLayout(this);
        mapHead.setOrientation(LinearLayout.HORIZONTAL);
        mapHead.setGravity(Gravity.CENTER_VERTICAL);
        mapCard.addView(mapHead, new LinearLayout.LayoutParams(-1, -2));
        TextView mapTitle = text("TACTICAL MAP", 13, TEXT, true);
        mapTitle.setLetterSpacing(0.08f);
        mapHead.addView(mapTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView manual = text("MANUAL BOARD", 9, GREEN, true);
        manual.setPadding(dp(8), dp(5), dp(8), dp(5));
        manual.setBackground(shape(Color.rgb(25, 58, 44), GREEN, 1, 20));
        mapHead.addView(manual);

        mapCanvas = new MapCanvas(this);
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(-1, dp(350));
        canvasParams.topMargin = dp(12);
        mapCard.addView(mapCanvas, canvasParams);

        markerStatus = text("Chạm vào bản đồ để đặt dấu.", 11, MUTED, false);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(9);
        mapCard.addView(markerStatus, statusParams);

        sectionHeader(page, "01", "CÔNG CỤ ĐÁNH DẤU", "Chọn loại dấu rồi chạm vào vị trí trên sơ đồ.", dp(20));
        HorizontalScrollView toolsScroll = new HorizontalScrollView(this);
        toolsScroll.setHorizontalScrollBarEnabled(false);
        page.addView(toolsScroll, new LinearLayout.LayoutParams(-1, dp(48)));
        LinearLayout toolsRow = new LinearLayout(this);
        toolsRow.setOrientation(LinearLayout.HORIZONTAL);
        toolsRow.setGravity(Gravity.CENTER_VERTICAL);
        toolsScroll.addView(toolsRow);
        addToolChip(toolsRow, "PING", "Điểm ping", BLUE);
        addToolChip(toolsRow, "GANK", "Gank", GOLD);
        addToolChip(toolsRow, "OBJECTIVE", "Mục tiêu", GREEN);
        addToolChip(toolsRow, "DANGER", "Nguy hiểm", RED);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(42));
        actionParams.topMargin = dp(8);
        page.addView(actions, actionParams);
        Button undo = button("HOÀN TÁC", false);
        actions.addView(undo, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, -1, 1f);
        clearParams.leftMargin = dp(8);
        Button clear = button("XÓA TẤT CẢ", false);
        actions.addView(clear, clearParams);
        undo.setOnClickListener(v -> undoMarker());
        clear.setOnClickListener(v -> confirmClearMarkers());

        sectionHeader(page, "02", "ĐỒNG HỒ MỤC TIÊU", "Bộ đếm độc lập. Hãy tự chỉnh mốc thời gian phù hợp.", dp(23));
        TextView timerDisclaimer = text("Mốc 60 / 120 / 180 giây chỉ là giá trị mẫu, không phải thông số cố định trong game.", 10, MUTED, false);
        LinearLayout.LayoutParams disclaimerParams = new LinearLayout.LayoutParams(-1, -2);
        disclaimerParams.topMargin = dp(7);
        page.addView(timerDisclaimer, disclaimerParams);

        for (TimerBlock timer : timers) {
            addTimerCard(page, timer);
        }

        sectionHeader(page, "03", "GHI CHÚ CHIẾN THUẬT", "Nội dung được lưu ngay trên thiết bị.", dp(23));
        notesInput = new EditText(this);
        notesInput.setTextColor(TEXT);
        notesInput.setHintTextColor(Color.rgb(118, 137, 128));
        notesInput.setHint("Ví dụ: ưu tiên kiểm soát sông, canh thời điểm di chuyển...");
        notesInput.setTextSize(13);
        notesInput.setGravity(Gravity.TOP | Gravity.START);
        notesInput.setMinLines(4);
        notesInput.setPadding(dp(14), dp(12), dp(14), dp(12));
        notesInput.setBackground(shape(PANEL, Color.rgb(55, 75, 68), 1, 14));
        LinearLayout.LayoutParams notesParams = new LinearLayout.LayoutParams(-1, -2);
        notesParams.topMargin = dp(11);
        page.addView(notesInput, notesParams);
        notesInput.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_NOTES, ""));
        notesInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_NOTES, s.toString()).apply();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(dp(14), dp(15), dp(14), dp(5));
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(12);
        page.addView(footer, footerParams);
        TextView footerTitle = text("ARENA MAP LAB  •  v1.0.0", 10, GOLD, true);
        footer.addView(footerTitle);
        TextView footerNote = text("Ứng dụng hoạt động ngoại tuyến. Không đọc bộ nhớ, không sửa APK game, không theo dõi trận đấu và không hiển thị dữ liệu ẩn.", 10, MUTED, false);
        footerNote.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerNoteParams = new LinearLayout.LayoutParams(-1, -2);
        footerNoteParams.topMargin = dp(7);
        footer.addView(footerNote, footerNoteParams);

        setContentView(scroll);
    }

    private void addToolChip(LinearLayout parent, String tool, String label, int color) {
        TextView chip = text(label, 11, tool.equals(activeTool) ? BG : TEXT, true);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(dp(14), dp(9), dp(14), dp(9));
        chip.setBackground(shape(tool.equals(activeTool) ? color : PANEL_ALT,
                tool.equals(activeTool) ? color : Color.rgb(57, 77, 70), 1, 20));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(36));
        params.rightMargin = dp(7);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> {
            activeTool = tool;
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof TextView) {
                    TextView item = (TextView) child;
                    String tag = item.getTag() == null ? "" : item.getTag().toString();
                    boolean selected = tag.equals(activeTool);
                    int itemColor = toolColor(tag);
                    item.setTextColor(selected ? BG : TEXT);
                    item.setBackground(shape(selected ? itemColor : PANEL_ALT,
                            selected ? itemColor : Color.rgb(57, 77, 70), 1, 20));
                }
            }
            markerStatus.setText("Đang chọn: " + label + ". Chạm vào bản đồ để đặt dấu.");
        });
        chip.setTag(tool);
    }

    private int toolColor(String tool) {
        switch (tool) {
            case "GANK": return GOLD;
            case "OBJECTIVE": return GREEN;
            case "DANGER": return RED;
            default: return BLUE;
        }
    }

    private void setupTimers() {
        timers.add(new TimerBlock("MỤC TIÊU A", 60));
        timers.add(new TimerBlock("MỤC TIÊU B", 120));
        timers.add(new TimerBlock("MỤC TIÊU C", 180));
    }

    private void addTimerCard(LinearLayout parent, TimerBlock timer) {
        LinearLayout card = card();
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = dp(9);
        parent.addView(card, cardParams);

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(top, new LinearLayout.LayoutParams(-1, -2));
        TextView name = text(timer.label, 11, TEXT, true);
        name.setLetterSpacing(0.06f);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView value = text(formatTime(timer.remaining), 20, GOLD, true);
        value.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        top.addView(value);
        timerDisplays.add(value);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams controlsParams = new LinearLayout.LayoutParams(-1, dp(36));
        controlsParams.topMargin = dp(10);
        card.addView(controls, controlsParams);

        Button minus = smallButton("−10s");
        Button plus = smallButton("+10s");
        Button run = smallButton("BẮT ĐẦU");
        Button reset = smallButton("ĐẶT LẠI");
        controls.addView(minus, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams plusParams = new LinearLayout.LayoutParams(0, -1, 1f);
        plusParams.leftMargin = dp(5);
        controls.addView(plus, plusParams);
        LinearLayout.LayoutParams runParams = new LinearLayout.LayoutParams(0, -1, 1.25f);
        runParams.leftMargin = dp(5);
        controls.addView(run, runParams);
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(0, -1, 1f);
        resetParams.leftMargin = dp(5);
        controls.addView(reset, resetParams);

        minus.setOnClickListener(v -> {
            timer.remaining = Math.max(0, timer.remaining - 10);
            updateTimerDisplays();
        });
        plus.setOnClickListener(v -> {
            timer.remaining = Math.min(5999, timer.remaining + 10);
            updateTimerDisplays();
        });
        run.setOnClickListener(v -> {
            timer.running = !timer.running;
            run.setText(timer.running ? "TẠM DỪNG" : "TIẾP TỤC");
            if (timer.remaining == 0 && timer.running) timer.remaining = timer.initial;
            updateTimerDisplays();
        });
        reset.setOnClickListener(v -> {
            timer.running = false;
            timer.remaining = timer.initial;
            run.setText("BẮT ĐẦU");
            updateTimerDisplays();
        });
    }

    private void startTimerLoop() {
        timerTick = new Runnable() {
            @Override public void run() {
                boolean changed = false;
                for (TimerBlock timer : timers) {
                    if (timer.running) {
                        if (timer.remaining > 0) timer.remaining--;
                        if (timer.remaining <= 0) {
                            timer.remaining = 0;
                            timer.running = false;
                        }
                        changed = true;
                    }
                }
                if (changed) updateTimerDisplays();
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(timerTick, 1000);
    }

    private void updateTimerDisplays() {
        for (int i = 0; i < timers.size() && i < timerDisplays.size(); i++) {
            timerDisplays.get(i).setText(formatTime(timers.get(i).remaining));
            timerDisplays.get(i).setTextColor(timers.get(i).remaining == 0 ? RED : GOLD);
        }
    }

    private String formatTime(int seconds) {
        return String.format(Locale.getDefault(), "%02d:%02d", seconds / 60, seconds % 60);
    }

    private void undoMarker() {
        if (!markers.isEmpty()) {
            markers.remove(markers.size() - 1);
            saveMarkers();
            mapCanvas.invalidate();
            markerStatus.setText("Đã hoàn tác dấu gần nhất. Còn " + markers.size() + " dấu.");
        } else {
            markerStatus.setText("Chưa có dấu nào để hoàn tác.");
        }
    }

    private void confirmClearMarkers() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa toàn bộ dấu?")
                .setMessage("Các dấu thủ công trên sơ đồ sẽ bị xóa. Ghi chú và đồng hồ không bị ảnh hưởng.")
                .setNegativeButton("HỦY", null)
                .setPositiveButton("XÓA", (dialog, which) -> {
                    markers.clear();
                    saveMarkers();
                    mapCanvas.invalidate();
                    markerStatus.setText("Đã xóa toàn bộ dấu.");
                }).show();
    }

    private void loadMarkers() {
        String raw = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_MARKERS, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                markers.add(new MapMarker((float) o.optDouble("x"), (float) o.optDouble("y"), o.optString("type", "PING")));
            }
        } catch (Exception ignored) {
            markers.clear();
        }
    }

    private void saveMarkers() {
        JSONArray array = new JSONArray();
        for (MapMarker marker : markers) {
            JSONObject o = new JSONObject();
            try {
                o.put("x", marker.x);
                o.put("y", marker.y);
                o.put("type", marker.type);
                array.put(o);
            } catch (Exception ignored) { }
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_MARKERS, array.toString()).apply();
    }

    private void sectionHeader(LinearLayout parent, String number, String heading, String description, int topMargin) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = topMargin;
        params.bottomMargin = dp(9);
        parent.addView(block, params);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        block.addView(row);
        TextView num = text(number, 10, GOLD, true);
        num.setPadding(dp(8), dp(5), dp(8), dp(5));
        num.setBackground(shape(PANEL_ALT, Color.rgb(86, 87, 57), 1, 7));
        row.addView(num);
        TextView title = text(heading, 13, TEXT, true);
        title.setLetterSpacing(0.06f);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-2, -2);
        titleParams.leftMargin = dp(9);
        row.addView(title, titleParams);
        TextView desc = text(description, 10, MUTED, false);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(-1, -2);
        descParams.topMargin = dp(6);
        block.addView(desc, descParams);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(13), dp(14), dp(13));
        card.setBackground(shape(PANEL, Color.rgb(43, 64, 57), 1, 17));
        return card;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        if (bold) text.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return text;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(10);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? BG : TEXT);
        button.setAllCaps(false);
        button.setPadding(dp(5), 0, dp(5), 0);
        button.setBackground(shape(primary ? GOLD : PANEL_ALT,
                primary ? GOLD : Color.rgb(66, 84, 75), 1, 11));
        return button;
    }

    private Button smallButton(String label) {
        Button b = button(label, false);
        b.setTextSize(9);
        b.setPadding(0, 0, 0, 0);
        return b;
    }

    private GradientDrawable shape(int color, int strokeColor, int strokeWidth, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        if (strokeWidth > 0) drawable.setStroke(dp(strokeWidth), strokeColor);
        return drawable;
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        if (timerTick != null) handler.removeCallbacks(timerTick);
        super.onDestroy();
    }

    private static class TimerBlock {
        final String label;
        final int initial;
        int remaining;
        boolean running;
        TimerBlock(String label, int initial) {
            this.label = label;
            this.initial = initial;
            this.remaining = initial;
        }
    }

    private static class MapMarker {
        final float x;
        final float y;
        final String type;
        MapMarker(float x, float y, String type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    private class MapCanvas extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF bounds = new RectF();
        private final Path path = new Path();

        MapCanvas(Activity context) {
            super(context);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setBackground(shape(Color.rgb(17, 36, 32), Color.rgb(70, 93, 77), 1, 13));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            if (w <= 0 || h <= 0) return;
            canvas.save();
            float inset = dp(7);
            canvas.translate(inset, inset);
            w -= inset * 2;
            h -= inset * 2;
            canvas.scale(w / 1000f, h / 1000f);

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(20, 48, 39));
            canvas.drawRoundRect(0, 0, 1000, 1000, 48, 48, paint);

            // Subtle map grid.
            paint.setColor(Color.argb(22, 180, 207, 170));
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);
            for (int i = 1; i < 10; i++) {
                float p = i * 100f;
                canvas.drawLine(p, 0, p, 1000, paint);
                canvas.drawLine(0, p, 1000, p, paint);
            }
            paint.setStyle(Paint.Style.FILL);

            // Forest regions.
            drawForest(canvas, 140, 320, 115);
            drawForest(canvas, 350, 105, 105);
            drawForest(canvas, 465, 330, 120);
            drawForest(canvas, 670, 520, 120);
            drawForest(canvas, 830, 300, 105);
            drawForest(canvas, 320, 760, 105);
            drawForest(canvas, 600, 860, 120);
            drawForest(canvas, 760, 720, 100);

            // River winding across the central jungle.
            path.reset();
            path.moveTo(65, 590);
            path.cubicTo(220, 520, 280, 650, 410, 555);
            path.cubicTo(540, 455, 550, 365, 685, 420);
            path.cubicTo(820, 475, 820, 330, 930, 275);
            paint.setColor(Color.rgb(48, 91, 89));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(62);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            canvas.drawPath(path, paint);
            paint.setColor(Color.argb(100, 111, 168, 150));
            paint.setStrokeWidth(4);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);

            // Three lanes connecting the two bases.
            drawLane(canvas, new float[]{125, 835, 130, 650, 190, 405, 385, 190, 650, 145, 865, 130});
            drawLane(canvas, new float[]{125, 835, 270, 710, 450, 545, 650, 365, 865, 130});
            drawLane(canvas, new float[]{125, 835, 350, 865, 610, 850, 805, 660, 865, 130});

            // Jungle objective circles.
            drawObjective(canvas, 315, 490, "A");
            drawObjective(canvas, 690, 510, "B");
            drawObjective(canvas, 500, 260, "C");

            // Towers along each lane.
            drawTower(canvas, 155, 650);
            drawTower(canvas, 210, 390);
            drawTower(canvas, 410, 190);
            drawTower(canvas, 280, 710);
            drawTower(canvas, 520, 465);
            drawTower(canvas, 805, 660);
            drawTower(canvas, 640, 845);
            drawTower(canvas, 865, 280);

            // Bases.
            drawBase(canvas, 115, 875, "XANH", Color.rgb(95, 173, 224));
            drawBase(canvas, 885, 105, "ĐỎ", Color.rgb(226, 112, 101));

            // Lane captions.
            drawLabel(canvas, 400, 80, "ĐƯỜNG TRÊN");
            drawLabel(canvas, 500, 600, "ĐƯỜNG GIỮA");
            drawLabel(canvas, 650, 940, "ĐƯỜNG DƯỚI");

            // Hand-placed markers.
            for (MapMarker marker : markers) drawMarker(canvas, marker);

            canvas.restore();
        }

        private void drawForest(Canvas canvas, float x, float y, float radius) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(28, 64, 48));
            canvas.drawCircle(x, y, radius, paint);
            paint.setColor(Color.argb(100, 84, 123, 76));
            canvas.drawCircle(x - radius * 0.24f, y - radius * 0.18f, radius * 0.52f, paint);
            paint.setColor(Color.argb(65, 7, 24, 20));
            canvas.drawCircle(x + radius * 0.28f, y + radius * 0.25f, radius * 0.42f, paint);
        }

        private void drawLane(Canvas canvas, float[] pts) {
            Path p = new Path();
            p.moveTo(pts[0], pts[1]);
            for (int i = 2; i < pts.length; i += 2) p.lineTo(pts[i], pts[i + 1]);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setColor(Color.rgb(79, 91, 68));
            paint.setStrokeWidth(48);
            canvas.drawPath(p, paint);
            paint.setColor(Color.rgb(133, 141, 101));
            paint.setStrokeWidth(5);
            canvas.drawPath(p, paint);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawTower(Canvas canvas, float x, float y) {
            paint.setColor(Color.argb(100, 7, 18, 16));
            canvas.drawCircle(x + 4, y + 5, 26, paint);
            paint.setColor(Color.rgb(194, 178, 125));
            canvas.drawCircle(x, y, 24, paint);
            paint.setColor(Color.rgb(65, 76, 59));
            canvas.drawCircle(x, y, 15, paint);
            paint.setColor(Color.rgb(228, 214, 166));
            canvas.drawCircle(x, y, 6, paint);
        }

        private void drawObjective(Canvas canvas, float x, float y, String name) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(55, 214, 182, 109));
            canvas.drawCircle(x, y, 44, paint);
            paint.setColor(Color.rgb(173, 139, 72));
            canvas.drawCircle(x, y, 23, paint);
            paint.setColor(Color.rgb(239, 220, 158));
            canvas.drawCircle(x, y, 8, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(25);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setColor(Color.rgb(245, 238, 205));
            canvas.drawText(name, x, y - 34, paint);
        }

        private void drawBase(Canvas canvas, float x, float y, String label, int color) {
            paint.setColor(Color.argb(60, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawCircle(x, y, 65, paint);
            paint.setColor(color);
            canvas.drawCircle(x, y, 37, paint);
            paint.setColor(Color.rgb(239, 244, 230));
            canvas.drawCircle(x, y, 17, paint);
            paint.setColor(Color.rgb(10, 22, 19));
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(28);
            canvas.drawText(label, x, y + 82, paint);
        }

        private void drawLabel(Canvas canvas, float x, float y, String label) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(22);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setColor(Color.argb(180, 218, 226, 197));
            canvas.drawText(label, x, y, paint);
        }

        private void drawMarker(Canvas canvas, MapMarker marker) {
            float x = marker.x * 1000f;
            float y = marker.y * 1000f;
            int color = toolColor(marker.type);
            paint.setColor(Color.argb(75, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawCircle(x, y, 48, paint);
            paint.setColor(color);
            canvas.drawCircle(x, y, 23, paint);
            paint.setColor(Color.rgb(12, 24, 22));
            canvas.drawCircle(x, y, 7, paint);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(18);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setColor(Color.WHITE);
            String letter = marker.type.equals("OBJECTIVE") ? "M" :
                    marker.type.equals("DANGER") ? "!" :
                    marker.type.equals("GANK") ? "G" : "P";
            canvas.drawText(letter, x, y - 34, paint);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float inset = dp(7);
                float usableW = getWidth() - inset * 2;
                float usableH = getHeight() - inset * 2;
                float nx = (event.getX() - inset) / usableW;
                float ny = (event.getY() - inset) / usableH;
                if (nx >= 0 && nx <= 1 && ny >= 0 && ny <= 1) {
                    markers.add(new MapMarker(nx, ny, activeTool));
                    saveMarkers();
                    invalidate();
                    String label = activeTool.equals("OBJECTIVE") ? "Mục tiêu" :
                            activeTool.equals("DANGER") ? "Nguy hiểm" :
                            activeTool.equals("GANK") ? "Gank" : "Điểm ping";
                    markerStatus.setText("Đã đặt " + label + " • Tổng " + markers.size() + " dấu.");
                    return true;
                }
            }
            return true;
        }
    }
}

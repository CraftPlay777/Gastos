package com.gastos.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final Locale AR = new Locale("es", "AR");
    private static final int COLOR_MAIN = Color.parseColor("#6D28D9");
    private static final int COLOR_LIGHT = Color.parseColor("#EFE9FB");

    private static final String[] CATS = {"Comida", "Transporte", "Hogar", "Ocio", "Salud", "Otros"};
    private static final int[] COLORS = {
            Color.parseColor("#6D28D9"), Color.parseColor("#2563EB"), Color.parseColor("#16A34A"),
            Color.parseColor("#D97706"), Color.parseColor("#DC2626"), Color.parseColor("#6B7280")
    };

    private static class Expense {
        long time;
        String cat;
        double amount;
        String note;
    }

    private SharedPreferences prefs;
    private final List<Expense> all = new ArrayList<>();
    private final List<Expense> visible = new ArrayList<>();
    private final List<String> lines = new ArrayList<>();
    private final Calendar month = Calendar.getInstance();

    private final View[] screens = new View[2];
    private final Button[] tabs = new Button[2];

    private ArrayAdapter<String> adapter;
    private TextView monthText;
    private TextView totalText;
    private TextView chartMonth;
    private EditText amountInput;
    private EditText noteInput;
    private Spinner catSpinner;
    private BarChartView chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("gastos", MODE_PRIVATE);
        month.set(Calendar.DAY_OF_MONTH, 1);
        load();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        FrameLayout content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        screens[0] = buildList();
        screens[1] = buildChart();
        for (View v : screens) {
            content.addView(v, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(COLOR_LIGHT);
        String[] names = {"Gastos", "Gráfico"};
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(names[i]);
            b.setAllCaps(false);
            b.setBackgroundColor(Color.TRANSPARENT);
            b.setOnClickListener(v -> showScreen(idx));
            tabs[i] = b;
            bar.addView(b, new LinearLayout.LayoutParams(0, dp(56), 1f));
        }
        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
        showScreen(0);
        refresh();
    }

    // Pantallas

    private void showScreen(int idx) {
        for (int i = 0; i < 2; i++) {
            screens[i].setVisibility(i == idx ? View.VISIBLE : View.GONE);
            tabs[i].setTextColor(i == idx ? COLOR_MAIN : Color.GRAY);
            tabs[i].setTypeface(null, i == idx ? Typeface.BOLD : Typeface.NORMAL);
        }
    }

    private LinearLayout baseScreen(String title) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(20), dp(24), dp(20), dp(12));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(26);
        t.setTypeface(null, Typeface.BOLD);
        t.setTextColor(COLOR_MAIN);
        l.addView(t);
        return l;
    }

    private Button mainButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackgroundColor(COLOR_MAIN);
        return b;
    }

    // Lista de gastos

    private View buildList() {
        LinearLayout l = baseScreen("Gastos");

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = new Button(this);
        prev.setText("<");
        prev.setOnClickListener(v -> moveMonth(-1));
        nav.addView(prev, new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));
        monthText = new TextView(this);
        monthText.setGravity(Gravity.CENTER);
        monthText.setTextSize(17);
        monthText.setTypeface(null, Typeface.BOLD);
        nav.addView(monthText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button next = new Button(this);
        next.setText(">");
        next.setOnClickListener(v -> moveMonth(1));
        nav.addView(next, new LinearLayout.LayoutParams(dp(56), ViewGroup.LayoutParams.WRAP_CONTENT));
        l.addView(nav);

        totalText = new TextView(this);
        totalText.setGravity(Gravity.CENTER);
        totalText.setTextSize(30);
        totalText.setTypeface(null, Typeface.BOLD);
        totalText.setTextColor(COLOR_MAIN);
        totalText.setPadding(0, dp(4), 0, dp(8));
        l.addView(totalText);

        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);
        amountInput = new EditText(this);
        amountInput.setHint("Monto");
        amountInput.setSingleLine(true);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        row1.addView(amountInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        catSpinner = new Spinner(this);
        catSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, CATS));
        row1.addView(catSpinner, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        l.addView(row1);

        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);
        noteInput = new EditText(this);
        noteInput.setHint("Nota (opcional)");
        noteInput.setSingleLine(true);
        noteInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        row2.addView(noteInput, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button add = mainButton("Agregar");
        add.setOnClickListener(v -> addExpense());
        row2.addView(add);
        l.addView(row2);

        TextView hint = new TextView(this);
        hint.setText("Mantené apretado un gasto para borrarlo");
        hint.setTextColor(Color.GRAY);
        hint.setPadding(0, dp(8), 0, dp(4));
        l.addView(hint);

        ListView list = new ListView(this);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lines);
        list.setAdapter(adapter);
        list.setOnItemLongClickListener((parent, view, position, id) -> {
            if (position < visible.size()) {
                all.remove(visible.get(position));
                save();
                refresh();
                Toast.makeText(this, "Gasto borrado", Toast.LENGTH_SHORT).show();
            }
            return true;
        });
        l.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return l;
    }

    private void addExpense() {
        double amount;
        try {
            amount = Double.parseDouble(amountInput.getText().toString().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            amount = 0;
        }
        if (amount <= 0 || Double.isInfinite(amount) || Double.isNaN(amount)) {
            Toast.makeText(this, "Poné un monto válido", Toast.LENGTH_SHORT).show();
            return;
        }
        Expense e = new Expense();
        e.time = System.currentTimeMillis();
        e.cat = CATS[catSpinner.getSelectedItemPosition()];
        e.amount = amount;
        e.note = noteInput.getText().toString().replace('|', ' ').replace('\n', ' ').trim();
        all.add(e);
        Collections.sort(all, (a, b) -> Long.compare(b.time, a.time));
        save();

        amountInput.setText("");
        noteInput.setText("");
        month.setTimeInMillis(e.time);
        month.set(Calendar.DAY_OF_MONTH, 1);
        refresh();
    }

    private void moveMonth(int delta) {
        month.add(Calendar.MONTH, delta);
        refresh();
    }

    private boolean inMonth(long t) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(t);
        return c.get(Calendar.YEAR) == month.get(Calendar.YEAR)
                && c.get(Calendar.MONTH) == month.get(Calendar.MONTH);
    }

    private void refresh() {
        String label = new SimpleDateFormat("MMMM yyyy", AR).format(month.getTime());
        if (!label.isEmpty()) {
            label = label.substring(0, 1).toUpperCase(AR) + label.substring(1);
        }
        monthText.setText(label);
        chartMonth.setText(label);

        visible.clear();
        lines.clear();
        double total = 0;
        double[] sums = new double[CATS.length];
        SimpleDateFormat df = new SimpleDateFormat("dd/MM", AR);
        for (Expense e : all) {
            if (!inMonth(e.time)) continue;
            visible.add(e);
            total += e.amount;
            for (int i = 0; i < CATS.length; i++) {
                if (CATS[i].equals(e.cat)) sums[i] += e.amount;
            }
            String line = df.format(new Date(e.time)) + "  ·  " + e.cat + "  ·  " + money(e.amount);
            if (!e.note.isEmpty()) line += "\n" + e.note;
            lines.add(line);
        }
        adapter.notifyDataSetChanged();
        totalText.setText(money(total));
        chart.setData(CATS, sums, COLORS);
    }

    // Gráfico

    private View buildChart() {
        LinearLayout l = baseScreen("Por categoría");
        chartMonth = new TextView(this);
        chartMonth.setTextSize(17);
        chartMonth.setTextColor(Color.GRAY);
        chartMonth.setPadding(0, dp(4), 0, dp(16));
        l.addView(chartMonth);

        chart = new BarChartView(this);
        l.addView(chart, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return l;
    }

    private static class BarChartView extends View {
        private String[] labels;
        private double[] values;
        private int[] colors;
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);

        BarChartView(Context c) {
            super(c);
            text.setTextSize(13 * getResources().getDisplayMetrics().scaledDensity);
        }

        void setData(String[] labels, double[] values, int[] colors) {
            this.labels = labels;
            this.values = values;
            this.colors = colors;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas c) {
            super.onDraw(c);
            if (values == null) return;
            float d = getResources().getDisplayMetrics().density;

            double max = 0;
            for (double v : values) max = Math.max(max, v);
            if (max <= 0) {
                text.setColor(Color.GRAY);
                text.setTextAlign(Paint.Align.CENTER);
                c.drawText("Sin gastos este mes", getWidth() / 2f, getHeight() / 4f, text);
                text.setTextAlign(Paint.Align.LEFT);
                return;
            }

            int n = labels.length;
            float rowH = Math.min(getHeight() / (float) n, 56 * d);
            float labelW = 92 * d;
            float valueW = 110 * d;
            float barMax = getWidth() - labelW - valueW;
            float textShift = text.getTextSize() / 3f;

            for (int i = 0; i < n; i++) {
                float top = i * rowH;
                float mid = top + rowH / 2f;
                text.setColor(Color.DKGRAY);
                c.drawText(labels[i], 0, mid + textShift, text);

                bar.setColor(colors[i]);
                float len = (float) (barMax * values[i] / max);
                if (values[i] > 0) {
                    RectF r = new RectF(labelW, top + rowH * 0.2f,
                            labelW + Math.max(len, 3 * d), top + rowH * 0.8f);
                    c.drawRoundRect(r, 6 * d, 6 * d, bar);
                }
                c.drawText(money(values[i]), getWidth() - valueW + 6 * d, mid + textShift, text);
            }
        }
    }

    // Datos

    private static String money(double v) {
        return String.format(AR, "$ %,.2f", v);
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < all.size(); i++) {
            Expense e = all.get(i);
            if (i > 0) sb.append('\n');
            sb.append(e.time).append('|').append(e.cat).append('|')
                    .append(e.amount).append('|').append(e.note);
        }
        prefs.edit().putString("expenses", sb.toString()).apply();
    }

    private void load() {
        all.clear();
        String raw = prefs.getString("expenses", "");
        if (raw.isEmpty()) return;
        for (String line : raw.split("\n")) {
            String[] p = line.split("\\|", 4);
            if (p.length < 3) continue;
            try {
                Expense e = new Expense();
                e.time = Long.parseLong(p[0]);
                e.cat = p[1];
                e.amount = Double.parseDouble(p[2]);
                e.note = p.length > 3 ? p[3] : "";
                all.add(e);
            } catch (NumberFormatException ignored) {
            }
        }
        Collections.sort(all, (a, b) -> Long.compare(b.time, a.time));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}

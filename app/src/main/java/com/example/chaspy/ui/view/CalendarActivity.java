package com.example.chaspy.ui.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chaspy.R;
import com.example.chaspy.ui.adapter.CalendarAdapter;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {
    private GridView calendarGridView;
    private TextView tvYear;
    private TextView tvSelectedDate;
    private TextView tvCurrentMonth;
    private ImageButton btnPreviousMonth;
    private ImageButton btnNextMonth;
    private Button btnCancel;
    private Button btnOk;

    private CalendarAdapter calendarAdapter;
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat headerDateFormat = new SimpleDateFormat("EEE, d MMM", Locale.getDefault());
    private SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());
    private SimpleDateFormat monthYearFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    public static final String EXTRA_SELECTED_DATE = "selected_date";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.date_picker_dialog);

        // Get passed date if available
        long dateMillis = getIntent().getLongExtra(EXTRA_SELECTED_DATE, -1);
        if (dateMillis != -1) {
            selectedDate.setTimeInMillis(dateMillis);
        }

        initViews();
        setupCalendar();
        setupListeners();
    }

    private void initViews() {
        calendarGridView = findViewById(R.id.calendarGridView);
        tvYear = findViewById(R.id.tvYear);
        tvSelectedDate = findViewById(R.id.tvSelectedDate);
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth);
        btnPreviousMonth = findViewById(R.id.btnPreviousMonth);
        btnNextMonth = findViewById(R.id.btnNextMonth);
        btnCancel = findViewById(R.id.btnCancel);
        btnOk = findViewById(R.id.btnOk);
    }

    private void setupCalendar() {
        calendarAdapter = new CalendarAdapter(this, selectedDate);
        calendarGridView.setAdapter(calendarAdapter);

        // Update header texts
        updateHeaderTexts();

        // Select the date that was passed or today's date if none was passed
        selectDate(selectedDate);
    }

    private void selectDate(Calendar date) {
        for (int i = 0; i < calendarAdapter.getCount(); i++) {
            Date itemDate = (Date) calendarAdapter.getItem(i);
            Calendar cal = Calendar.getInstance();
            cal.setTime(itemDate);

            if (cal.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                    cal.get(Calendar.MONTH) == date.get(Calendar.MONTH) &&
                    cal.get(Calendar.DAY_OF_MONTH) == date.get(Calendar.DAY_OF_MONTH)) {
                calendarAdapter.setSelectedDate(i);
                selectedDate.setTime(itemDate);
                updateHeaderTexts();
                break;
            }
        }
    }

    private void updateHeaderTexts() {
        tvYear.setText(yearFormat.format(selectedDate.getTime()));
        tvCurrentMonth.setText(monthYearFormat.format(currentMonthCalendar().getTime()));
        tvSelectedDate.setText(headerDateFormat.format(selectedDate.getTime()));
    }

    private Calendar currentMonthCalendar() {
        return calendarAdapter.getCurrentCalendar();
    }

    private void setupListeners() {
        calendarGridView.setOnItemClickListener((parent, view, position, id) -> {
            Date date = (Date) calendarAdapter.getItem(position);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            // If clicked on a day from prev/next month, switch to that month
            if (cal.get(Calendar.MONTH) != currentMonthCalendar().get(Calendar.MONTH)) {
                calendarAdapter.setCurrentMonth(cal);
                updateHeaderTexts();
            }

            selectedDate.setTime(date);
            calendarAdapter.setSelectedDate(position);
            updateHeaderTexts();
        });

        btnPreviousMonth.setOnClickListener(v -> {
            calendarAdapter.previousMonth();
            updateHeaderTexts();
        });

        btnNextMonth.setOnClickListener(v -> {
            calendarAdapter.nextMonth();
            updateHeaderTexts();
        });

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnOk.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_SELECTED_DATE, selectedDate.getTimeInMillis());
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }

    public static void startForResult(Activity activity, int requestCode) {
        startForResult(activity, requestCode, null);
    }

    public static void startForResult(Activity activity, int requestCode, Calendar initialDate) {
        Intent intent = new Intent(activity, CalendarActivity.class);
        if (initialDate != null) {
            intent.putExtra(EXTRA_SELECTED_DATE, initialDate.getTimeInMillis());
        }
        activity.startActivityForResult(intent, requestCode);
    }
}
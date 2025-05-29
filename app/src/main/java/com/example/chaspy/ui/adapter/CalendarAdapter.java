package com.example.chaspy.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import com.example.chaspy.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CalendarAdapter extends BaseAdapter {
    private Context context;
    private Calendar currentCalendar;
    private List<Date> dates;
    private int selectedPosition = -1;
    private SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    public CalendarAdapter(Context context, Calendar calendar) {
        this.context = context;
        this.currentCalendar = (Calendar) calendar.clone();
        this.dates = new ArrayList<>();
        refreshDays();
    }

    @Override
    public int getCount() {
        return dates.size();
    }

    @Override
    public Object getItem(int position) {
        return dates.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getTodayPosition() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        for (int i = 0; i < getCount(); i++) {
            Date date = (Date) getItem(i);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            if (cal.equals(today) &&
                    cal.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {
                return i;
            }
        }
        return -1;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.calendar_day_item, parent, false);
        }

        TextView tvDay = convertView.findViewById(R.id.tvDay);
        Date date = dates.get(position);

        // Get day number
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int dayNumber = cal.get(Calendar.DAY_OF_MONTH);

        tvDay.setText(String.valueOf(dayNumber));

        // Current month days are black, other days are grey
        int currentMonth = currentCalendar.get(Calendar.MONTH);
        int dateMonth = cal.get(Calendar.MONTH);

        if (currentMonth != dateMonth) {
            tvDay.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray));
            tvDay.setSelected(false); // Ensure days from other months can't be selected
        } else {
            // Only check selected state for current month days
            tvDay.setSelected(position == selectedPosition);
            if (position == selectedPosition) {
                tvDay.setTextColor(ContextCompat.getColor(context, android.R.color.white));
            } else {
                tvDay.setTextColor(ContextCompat.getColor(context, R.color.calendar_day_text_color));
            }
        }

        return convertView;
    }

    public void setSelectedDate(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
    }

    public Date getSelectedDate() {
        if (selectedPosition >= 0 && selectedPosition < dates.size()) {
            return dates.get(selectedPosition);
        }
        return null;
    }

    public void refreshDays() {
        dates.clear();
        Calendar calendar = (Calendar) currentCalendar.clone();

        // Set calendar to first day of month
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        // Find the day of week for the first day of month
        int firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK);

        // Adjust for starting with Monday (as in your layout)
        firstDayOfMonth = firstDayOfMonth == Calendar.SUNDAY ? 6 : firstDayOfMonth - 2;

        // Move calendar back to show previous month's days
        calendar.add(Calendar.DAY_OF_MONTH, -firstDayOfMonth);

        // Populate 42 days (6 weeks)
        for (int i = 0; i < 42; i++) {
            dates.add(calendar.getTime());
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }

        notifyDataSetChanged();
    }

    public void nextMonth() {
        currentCalendar.add(Calendar.MONTH, 1);
        refreshDays();
        selectedPosition = -1;
    }

    public void previousMonth() {
        currentCalendar.add(Calendar.MONTH, -1);
        refreshDays();
        selectedPosition = -1;
    }

    public String getCurrentMonthYear() {
        return monthFormat.format(currentCalendar.getTime());
    }

    public int getCurrentYear() {
        return currentCalendar.get(Calendar.YEAR);
    }

    public Calendar getCurrentCalendar() {
        return (Calendar) currentCalendar.clone();
    }

    public void setCurrentMonth(Calendar calendar) {
        currentCalendar = (Calendar) calendar.clone();
        // Ensure it's set to the first of the month for consistent behavior
        currentCalendar.set(Calendar.DAY_OF_MONTH, 1);
        refreshDays();
    }
}
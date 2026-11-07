package com.arohi.healthcare.utils;

import android.app.DatePickerDialog;
import android.content.Context;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Calendar;
import java.util.Locale;

/**
 * Reusable helper that attaches a calendar DatePickerDialog to a TextInputLayout and TextInputEditText,
 * formatting the selected date as YYYY/MM/DD.
 */
public final class DatePickerHelper {

    private DatePickerHelper() {
        // Prevent instantiation
    }

    /**
     * Attaches a calendar popup to both the end-icon of the TextInputLayout and click of the EditText.
     *
     * @param context          Activity context
     * @param textInputLayout  The TextInputLayout displaying the calendar icon
     * @param editText         The TextInputEditText receiving the YYYY/MM/DD string
     * @param restrictPastOnly True for Date of Birth (sets maxDate to today)
     */
    public static void attachDatePicker(Context context,
                                        TextInputLayout textInputLayout,
                                        TextInputEditText editText,
                                        boolean restrictPastOnly) {
        if (context == null || textInputLayout == null || editText == null) {
            return;
        }

        Runnable showPicker = () -> {
            Calendar cal = Calendar.getInstance();

            // Pre-populate picker with already entered YYYY/MM/DD or YYYY-MM-DD if valid
            String existing = editText.getText() != null ? editText.getText().toString().trim() : "";
            String[] parts = existing.split("[/\\-]");
            if (parts.length == 3) {
                try {
                    int y = Integer.parseInt(parts[0]);
                    int m = Integer.parseInt(parts[1]) - 1;
                    int d = Integer.parseInt(parts[2]);
                    if (y >= 1900 && y <= 2100 && m >= 0 && m <= 11 && d >= 1 && d <= 31) {
                        cal.set(y, m, d);
                    }
                } catch (NumberFormatException ignored) {
                }
            } else if (restrictPastOnly) {
                // Default to 25 years ago for Date of Birth convenience
                cal.add(Calendar.YEAR, -25);
            }

            int year = cal.get(Calendar.YEAR);
            int month = cal.get(Calendar.MONTH);
            int day = cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    context,
                    (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                        String formatted = String.format(
                                Locale.US,
                                "%04d/%02d/%02d",
                                selectedYear,
                                selectedMonth + 1,
                                selectedDayOfMonth
                        );
                        editText.setText(formatted);
                        editText.setSelection(formatted.length());
                        textInputLayout.setError(null);
                    },
                    year,
                    month,
                    day
            );

            if (restrictPastOnly) {
                dialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            }

            dialog.show();
        };

        textInputLayout.setEndIconOnClickListener(v -> showPicker.run());
        editText.setOnClickListener(v -> showPicker.run());
    }

    public static void attachToField(Context context,
                                     TextInputLayout textInputLayout,
                                     TextInputEditText editText) {
        attachDatePicker(context, textInputLayout, editText, true);
    }
}

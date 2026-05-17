package com.aksharadeep.tutor.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.graphics.Color;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.aksharadeep.tutor.R;
import com.aksharadeep.tutor.database.AppDatabase;
import com.aksharadeep.tutor.models.Chapter;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AksharaDeepaPref";
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = AppDatabase.getDatabase(this);

        // Show student name from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String studentName = prefs.getString("studentName", "Student");
        String studentClass = prefs.getString("studentClass", "Class 10");

        TextView tvGreeting = findViewById(R.id.tv_greeting);
        TextView tvStudentClass = findViewById(R.id.tv_student_class);
        if (tvGreeting != null) tvGreeting.setText("Hello, " + studentName + "! 👋");
        if (tvStudentClass != null) tvStudentClass.setText(studentClass);

        // Logout button
        TextView tvLogout = findViewById(R.id.tv_logout);
        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> confirmLogout());
        }

        setupCards();
        updateOverallProgress();
        checkDailyGoal();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateOverallProgress();
        checkDailyGoal();
    }

    private void checkDailyGoal() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Chapter> all = db.chapterDao().getAllChaptersSync();
            
            // Get start of today
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long startOfToday = cal.getTimeInMillis();

            boolean goalMet = false;
            for (Chapter ch : all) {
                if (ch.isCompleted && ch.completedDate >= startOfToday) {
                    goalMet = true;
                    break;
                }
            }

            final boolean finalGoalMet = goalMet;
            runOnUiThread(() -> {
                TextView tvDailyGoal = findViewById(R.id.tv_daily_goal);
                LinearLayout layoutDailyGoal = findViewById(R.id.layout_daily_goal);
                if (tvDailyGoal != null && layoutDailyGoal != null) {
                    if (finalGoalMet) {
                        tvDailyGoal.setText("🌟 Daily Goal Met! Great job! 🎉");
                        layoutDailyGoal.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                    } else {
                        tvDailyGoal.setText("🎯 Daily Goal: Study at least 1 chapter today!");
                        layoutDailyGoal.setBackgroundColor(Color.parseColor("#33FFFFFF"));
                    }
                }
            });
        });
    }

    private void setupCards() {
        CardView cardSyllabus = findViewById(R.id.card_syllabus);
        CardView cardStrength = findViewById(R.id.card_strength);
        if (cardSyllabus != null)
            cardSyllabus.setOnClickListener(v -> startActivity(new Intent(this, SyllabusActivity.class)));
        if (cardStrength != null)
            cardStrength.setOnClickListener(v -> startActivity(new Intent(this, StrengthMapActivity.class)));
    }

    private void updateOverallProgress() {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            List<Chapter> all = db.chapterDao().getAllChaptersSync();
            int total = all.size();
            int completed = 0;
            for (Chapter ch : all) { if (ch.isCompleted) completed++; }
            int percent = total > 0 ? (completed * 100) / total : 0;
            int finalCompleted = completed;

            runOnUiThread(() -> {
                ProgressBar progressBar = findViewById(R.id.progress_overall);
                TextView tvProgress = findViewById(R.id.tv_progress_text);
                TextView tvChaptersDone = findViewById(R.id.tv_chapters_done);
                if (progressBar != null) progressBar.setProgress(percent);
                if (tvProgress != null) tvProgress.setText(percent + "% Complete");
                if (tvChaptersDone != null)
                    tvChaptersDone.setText(finalCompleted + " / " + total + " chapters done");
            });
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                            .edit().putBoolean("isLoggedIn", false).apply();
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}

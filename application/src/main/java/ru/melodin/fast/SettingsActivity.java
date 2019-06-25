package ru.melodin.fast;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import ru.melodin.fast.common.ThemeManager;
import ru.melodin.fast.fragment.FragmentSettings;
import ru.melodin.fast.util.ViewUtil;
import ru.melodin.fast.view.Toolbar;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        setTheme(ThemeManager.getCurrentTheme());
        ViewUtil.applyWindowStyles(getWindow());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar tb = findViewById(R.id.tb);
        tb.setBackVisible(true);
        tb.setOnBackClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        getSupportFragmentManager().beginTransaction().add(R.id.fragment_container, new FragmentSettings()).commit();
    }
}

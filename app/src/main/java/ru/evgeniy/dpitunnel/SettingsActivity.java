package ru.evgeniy.dpitunnel;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageButton;

public class SettingsActivity extends AppCompatActivity {

    private ImageButton settingsBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Set custom theme
        setTheme(R.style.SettingsFragmentStyle);

        // Find layout elements
        settingsBackButton = findViewById(R.id.settings_back_button);

        // Set listeners
        settingsBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}

package com.freeme.game;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.freeme.game.settings.GmSettingsActivity;
import com.freeme.service.game.GameManager;

public class MainActivity extends Activity {

    private Button control;
    private GameManager mManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mManager = GameManager.from(this);

        if (mManager == null) {
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        control = findViewById(R.id.game_mode_control);
        control.setText(mManager.isGameModeActive() ? R.string.test_stop_game_mode
                : R.string.test_start_game_mode);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.game_mode:
                startActivity(new Intent(this, GmSettingsActivity.class));
                break;
            case R.id.game_mode_control:
                boolean newMode = !mManager.isGameModeActive();
                control.setText(newMode ? R.string.test_stop_game_mode
                        : R.string.test_start_game_mode);
                mManager.turnGameMode(newMode);
                break;
            default:
                break;
        }
    }
}

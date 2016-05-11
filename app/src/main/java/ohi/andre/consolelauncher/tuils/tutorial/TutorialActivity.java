package ohi.andre.consolelauncher.tuils.tutorial;

import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.Tuils;

public class TutorialActivity extends AppCompatActivity {

    private final int APPLICATIONS = 0;
    private final int COMMANDS = 1;
    private final int SETTINGS = 2;
    private final int ALIAS = 3;
    private final int MUSIC = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int argument = getIntent().getIntExtra(TutorialIndexActivity.ARGUMENT_KEY, -1);
        if (argument == -1) {
            finish();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            Tuils.enableUpNavigation(this);

        setContentView(R.layout.activity_tutorial);

        int textResource = 0;
        switch (argument) {
            case APPLICATIONS:
                textResource = R.string.applications_tutorial;
                break;
            case COMMANDS:
                textResource = R.string.commands_tutorial;
                break;
            case SETTINGS:
                textResource = R.string.settings_tutorial;
                break;
            case ALIAS:
                textResource = R.string.alias_tutorial;
                break;
            case MUSIC:
                textResource = R.string.music_tutorial;
                break;
            default:
                return;
        }

        TextView tutorialText = (TextView) findViewById(R.id.tutorial_text);
        tutorialText.setText(textResource);
    }
}

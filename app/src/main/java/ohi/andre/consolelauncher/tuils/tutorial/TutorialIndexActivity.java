package ohi.andre.consolelauncher.tuils.tutorial;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import ohi.andre.consolelauncher.R;

public class TutorialIndexActivity extends AppCompatActivity {

    public static String ARGUMENT_KEY = "argument";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_index);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            enableUpNavigation();
        }

        ListView listView = (ListView) findViewById(R.id.tutorial_index_view);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                startTutorialActivity(position);
            }
        });
    }

    private void startTutorialActivity(int position) {
        Intent intent = new Intent(this, TutorialActivity.class);
        intent.putExtra(ARGUMENT_KEY, position);
        startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void enableUpNavigation() {
        try {
            this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        } catch (NullPointerException e) {}
    }

}

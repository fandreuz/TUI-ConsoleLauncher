package ohi.andre.consolelauncher.tuils.tutorial;

import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import ohi.andre.consolelauncher.BuildConfig;
import ohi.andre.consolelauncher.R;
import ohi.andre.consolelauncher.tuils.Animator;
import ohi.andre.consolelauncher.tuils.Tuils;

public class TutorialActivity extends AppCompatActivity {

    private final int FIRST = 0, END = 7;

    int position = FIRST;

    boolean[] visited = new boolean[END + 1];

    RelativeLayout root;
    TextView back, next, title;

    LayoutInflater inflater;
    Resources resources;

    RelativeLayout.LayoutParams containerParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    LinearLayout.LayoutParams bigMarginParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    LinearLayout.LayoutParams smallMarginParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    Typeface lucidaConsole;

    private View.OnClickListener nextListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(position < END) {
                position++;
                update();
            } else {
                TutorialActivity.this.finish();
            }
        }
    };

    private View.OnClickListener backListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(position > FIRST) {
                position--;
                update();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resources = getResources();
        setContentView(R.layout.tutorial_base_layout);

        root = (RelativeLayout) findViewById(R.id.tutorial_root);

        RelativeLayout.LayoutParams backParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        backParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        backParam.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        RelativeLayout.LayoutParams nextParam = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nextParam.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        nextParam.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        containerParams.addRule(RelativeLayout.BELOW, R.id.tutorial_std_title);
        containerParams.addRule(RelativeLayout.ABOVE, R.id.tutorial_navigation_container);
        bigMarginParams.setMargins(0,0,0,Tuils.dpToPx(resources, resources.getInteger(R.integer.tutorial_items_bigmargin)));
        smallMarginParams.setMargins(0,0,0,Tuils.dpToPx(resources, resources.getInteger(R.integer.tutorial_items_smallmargin)));

        back = (TextView) findViewById(R.id.tutorial_navigation_back);
        back.setOnClickListener(backListener);
        back.setTypeface(lucidaConsole);

        next = (TextView) findViewById(R.id.tutorial_navigation_next);
        next.setOnClickListener(nextListener);
        next.setTypeface(lucidaConsole);

        title = (TextView) findViewById(R.id.tutorial_std_title);
        title.setTypeface(lucidaConsole);

        inflater = getLayoutInflater();
        lucidaConsole = Typeface.createFromAsset(getAssets(), "lucida_console.ttf");

        update();
    }

    private void update() {
        View remove = root.findViewById(R.id.tutorial_container);
        if(remove != null) {
            root.removeView(remove);
        }

        back.setText(R.string.tutorial_navigation_left);
        next.setText(R.string.tutorial_navigation_right);

        View view = getView(inflater);
        view.setId(R.id.tutorial_container);

        if(position == FIRST) {
            TextView introduction = (TextView) view.findViewById(R.id.tutorial_first_title);
            introduction.setTypeface(lucidaConsole);

            TextView version = back;
            version.setText("vrs. " + BuildConfig.VERSION_NAME);

            title.setText(Tuils.EMPTYSTRING);

            if(!visited[position]) {
                Animator nextAnimator = new Animator(next);
                Animator versionAnimator = new Animator(version).setChained(nextAnimator);
                new Animator(introduction, getString(R.string.tutorial_title)).setChained(versionAnimator).animate();
            } else {
                introduction.setText(R.string.tutorial_title);
            }
        } else if(position == END) {

            TextView author = (TextView) view.findViewById(R.id.tutorial_end_label1);
            author.setTypeface(lucidaConsole);
            TextView location = (TextView) view.findViewById(R.id.tutorial_end_label2);
            location.setTypeface(lucidaConsole);

            title.setText(Tuils.EMPTYSTRING);

            final String email = "andreuzzi.francesco@gmail.com";
            String name = "Francesco Andreuzzi";

            ImageView git = (ImageView) view.findViewById(R.id.tutorial_end_github), mail = (ImageView) view.findViewById(R.id.tutorial_end_gmail),
                    googlep = (ImageView) view.findViewById(R.id.tutorial_end_googlep);

            git.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Andre1299/TUI-ConsoleLauncher"));
                    startActivity(browserIntent);
                }
            });

            mail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "About T-UI");
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });

            googlep.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final Intent intent = new Intent( Intent.ACTION_VIEW, Uri.parse( "https://plus.google.com/communities/103936578623101446195") );
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });

            if(!visited[position]) {
                Animator a2 = new Animator(location, email);
                new Animator(author, name).setChained(a2).animate();

                new Animator(mail).setChained(new Animator(git).setChained(new Animator(googlep))).animate();
            } else {
                location.setText(email);
                author.setText(name);
            }
        } else {
            buildTutorial((LinearLayout) view.findViewById(R.id.tutorial_std_container), title, position);
        }

        visited[position] = true;

        root.addView(view, 0, containerParams);
    }

    private void buildTutorial(LinearLayout container, TextView title, int position) {
        TypedArray ta = resources.obtainTypedArray(R.array.tutorial);
        CharSequence[] strings = ta.getTextArray(position - 1);
        ta.recycle();

        title.setText(strings[0]);
        title.setTypeface(lucidaConsole);

        for(int count = 1; count < strings.length; count++) {
            String bigSpaceString = strings[count].subSequence(0,1).toString(), isCommandString = strings[count].subSequence(1,2).toString();
            if(!bigSpaceString.equals("0") && !bigSpaceString.equals("1") && !isCommandString.equals("0") && !isCommandString.equals("1")) {
                bigSpaceString = "0";
                isCommandString = "1";
            } else {
                strings[count] = strings[count].subSequence(2,strings[count].length());
            }

            boolean bigSpace = bigSpaceString.equals("0");
            boolean isCommand = isCommandString.equals("0");

            TextView textView = new TextView(this);

            int styleRes = isCommand ? R.style.Tutorial_Text_Primary : R.style.Tutorial_Text_Secondary;
            textView.setTextAppearance(this, styleRes);
            textView.setTypeface(lucidaConsole);

            container.addView(textView, bigSpace ? bigMarginParams : smallMarginParams);

            if(isCommand && !visited[position]) {
                new Animator(textView, strings[count].toString()).animate();
            } else {
                textView.setText(strings[count]);
            }
        }
    }

    private View getView(LayoutInflater inflater) {
        int res;
        if(position == FIRST) {
            res = R.layout.tutorial_first_layout;
        } else if(position == END) {
            res = R.layout.tutorial_end_layout;
        } else {
            ScrollView scrollView = new ScrollView(this);
            scrollView.setClipToPadding(false);
            if(Build.VERSION.SDK_INT >= 9) {
                scrollView.setOverScrollMode(ScrollView.OVER_SCROLL_NEVER);
            }

            LinearLayout container = new LinearLayout(this);
            container.setOrientation(LinearLayout.VERTICAL);
            container.setId(R.id.tutorial_std_container);

            scrollView.addView(container);

            return scrollView;
        }

        return inflater.inflate(res, null);
    }
}

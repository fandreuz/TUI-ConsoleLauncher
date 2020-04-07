package ohi.andre.consolelauncher.tuils.interfaces;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by francescoandreuzzi on 05/03/2018.
 */

public interface Reloadable {

    String MESSAGE = "msg";

    void reload();
    void addMessage(String header, String message);

    class RestartMessageCategory {

        public String header;
        public List<String> lines;

        public RestartMessageCategory(String header) {
            this.header = header;

            lines = new ArrayList<>();
        }

        public CharSequence text() {
            CharSequence sequence = TextUtils.concat(header, "\n");

            StringBuilder builder = new StringBuilder();
            final String dash = "-";
            for(int c = 0; c < lines.size(); c++) builder.append(" ").append(dash).append(" ").append(lines.get(c)).append("\n");

            return TextUtils.concat(sequence, builder.toString());
        }

        @Override
        public String toString() {
            return text().toString();
        }
    }
}

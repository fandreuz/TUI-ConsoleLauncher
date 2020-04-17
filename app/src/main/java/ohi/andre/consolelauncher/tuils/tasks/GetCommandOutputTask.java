package ohi.andre.consolelauncher.tuils.tasks;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ohi.andre.consolelauncher.tuils.interfaces.OnCommandUpdate;

public class GetCommandOutputTask extends AsyncTask<String, Integer, String> {

        private Runnable runnable;
        private OnCommandUpdate onCommandUpdate;

        public GetCommandOutputTask(Runnable ru, OnCommandUpdate onCommandUpdate){
           runnable=ru;
           this.onCommandUpdate = onCommandUpdate;
        }
        @Override
        protected String doInBackground(String... commands) {
            try {
                Process process = Runtime.getRuntime().exec(commands[0]);
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder log = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    log.append(line + "\n");
                }
                return log.toString();

            } catch (IOException e) {
                return "error";
            }
        }


        @Override
        protected void onPostExecute(String output) {
            this.onCommandUpdate.update(output,runnable);
        }
    }
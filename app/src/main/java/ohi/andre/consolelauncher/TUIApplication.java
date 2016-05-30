package ohi.andre.consolelauncher;

/*Copyright Francesco Andreuzzi

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/

import android.app.Application;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import ohi.andre.consolelauncher.tuils.Tuils;

public class TUIApplication extends Application {

    private final String ERROR_FILE = "error.log";

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler =
            new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    File tuiFolder = Tuils.getTuiFolder();
                    File errorLog = new File(tuiFolder, ERROR_FILE);
                    try {
                        FileWriter writer = new FileWriter(errorLog);
                        writer.write(ex.toString());
                        writer.write(Tuils.NEWLINE);
                        writer.write(Tuils.NEWLINE);
                        for(StackTraceElement element : ex.getStackTrace()) {
                            writer.write(element.toString());
                            writer.write(Tuils.NEWLINE);
                        }
                        writer.close();
                    } catch (IOException e) {}
                }
            };

    public TUIApplication() {
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
    }
}

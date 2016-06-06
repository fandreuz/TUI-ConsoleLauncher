package ohi.andre.consolelauncher.tuils;

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

import java.io.File;
import java.io.FilenameFilter;

public class SpecificExtensionFileFilter implements FilenameFilter {

    private String extension;

    public void setExtension(String extension) {
        this.extension = extension.toLowerCase();
    }

    @Override
    public boolean accept(File dir, String filename) {
        int dot = filename.lastIndexOf(Tuils.DOT);
        if(dot == -1) {
            return false;
        }

        String fileExtension = filename.substring(dot + 1);
        return fileExtension.toLowerCase().equals(extension);
    }
}

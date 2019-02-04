package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.content.Intent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import ohi.andre.consolelauncher.tuils.Tuils;

public class FileManager {

    public static final int FILE_NOTFOUND = 10;
    public static final int ISDIRECTORY = 11;
    public static final int IOERROR = 12;

    private static final String ASTERISK = "*";
    private static final String DOT = Tuils.DOT;

    public static String writeOn(File file, String text) {
        try {
            FileOutputStream stream = new FileOutputStream(file);
            stream.write(text.getBytes());
            stream.flush();
            stream.close();
            return null;
        } catch (FileNotFoundException e) {
            return e.toString();
        } catch (IOException e) {
            return e.toString();
        }
    }

    public static int openFile(Context c, File file) {
        if (file == null) {
            return FileManager.FILE_NOTFOUND;
        }
        if (file.isDirectory()) {
            return FileManager.ISDIRECTORY;
        }

        Intent intent = Tuils.openFile(c, file);

        c.startActivity(intent);
        return 0;
    }

    public static DirInfo cd(File currentDirectory, String path) {
        File file;
        String notFound = "";

//        path == "/" (root folder)
        if (path.equals(File.separator))
            return new DirInfo(new File(path), null);

//        remove the useless "/" from the end of path
        if (path.endsWith(File.separator))
            path = path.substring(0, path.length() - 1);

//        absolute path
        if (path.startsWith(File.separator))
            file = new File(path);
//        relative path
        else {
//            create a file from the path
            file = new File(currentDirectory, path);
//            assign path
            path = file.getAbsolutePath();
        }

//        cycle on path until file exists
        String toAdd;
        while (!file.exists()) {
//            find the last slash
            int slash = path.lastIndexOf(File.separator);
            if (slash == -1)
                slash = 0;

            toAdd = path.substring(slash, path.length());
//            add a "/" to be sure
            if (!toAdd.startsWith(File.separator))
                toAdd = toAdd.concat(File.separator);
//            toadd is concatenated at the end of not found
            notFound = toAdd.concat(notFound);

//            adjust path
            file = file.getParentFile();
            path = file.getAbsolutePath();
        }

//        ok, now file exists, path = file absolute path

//        check if path contains ".."
        String cut, pathSection;
        for (int count = path.length(); path.contains(".."); ) {

            pathSection = path.substring(0, count);
            count = pathSection.lastIndexOf(File.separator);

//            get the part between "/" and end of path or "/"
            cut = pathSection.substring(count + 1, pathSection.length());
//            if cut is ..
            if (cut.equals("..")) {
//                find the slash before count
                int preSlash = path.substring(0, count).lastIndexOf(File.separator);
//                find the part after ".."
                String rightPart = path.substring(count + cut.length() + 1);

                path = path.substring(0, preSlash + 1).concat(rightPart);
            }
        }
        file = new File(path);

        if (notFound.length() <= 0)
            notFound = null;
        else if (notFound.length() > 1) {
            if (notFound.startsWith(File.separator))
                notFound = notFound.substring(1);
            if (notFound.endsWith(File.separator))
                notFound = notFound.substring(0, notFound.length() - 1);
        }

        return new DirInfo(file, notFound);
    }

    public static WildcardInfo wildcard(String path) {
        if (path == null || !path.contains(ASTERISK) || path.contains(File.separator)) {
            return null;
        }

        if(path.trim().equals(ASTERISK)) {
            return new WildcardInfo(true);
        }

        int dot = path.lastIndexOf(DOT);
        try {
            String beforeDot = path.substring(0, dot);
            String afterDot = path.substring(dot + 1);
            return new WildcardInfo(beforeDot, afterDot);
        } catch (Exception e) {
            return null;
        }
    }

    public static class DirInfo {
        public File file;
        public String notFound;

        public DirInfo(File f, String nF) {
            this.file = f;
            this.notFound = nF;
        }

        public String getCompletePath() {
            return file.getAbsolutePath() + "/" + notFound;
        }
    }

    public static class WildcardInfo {

        public boolean allNames;
        public boolean allExtensions;
        public String name;
        public String extension;

        public WildcardInfo(String name, String extension) {
            this.name = name;
            this.extension = extension;

            allNames = name.length() == 0 || name.equals(ASTERISK);
            allExtensions = extension.length() == 0 || extension.equals(ASTERISK);
        }

        public WildcardInfo(boolean all) {
            if(all) {
                this.allExtensions = all;
                this.allNames = all;
            }
        }
    }

    public static class SpecificNameFileFilter implements FilenameFilter {

        private String name;

        public void setName(String name) {
            this.name = name.toLowerCase();
        }

        @Override
        public boolean accept(File dir, String filename) {
            int dot = filename.lastIndexOf(Tuils.DOT);
            if(dot == -1) {
                return false;
            }

            filename = filename.substring(0, dot);
            return filename.toLowerCase().equals(name);
        }
    }

    public static class SpecificExtensionFileFilter implements FilenameFilter {

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
}

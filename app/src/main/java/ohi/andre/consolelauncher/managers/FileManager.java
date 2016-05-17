package ohi.andre.consolelauncher.managers;

import android.content.Context;
import android.content.Intent;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import ohi.andre.comparestring.Compare;
import ohi.andre.consolelauncher.tuils.Tuils;

public class FileManager {

    public static final int FILE_NOTFOUND = 10;
    public static final int ISDIRECTORY = 11;
    public static final int IOERROR = 12;
    public static final int ISFILE = 13;

    public static final boolean USE_SCROLL_COMPARE = true;
    public static final String ALL = "allFiles";

    public static int mv(File[] files, File where, boolean su) throws IOException {
        if (files == null || files.length == 0 || where == null)
            return FileManager.FILE_NOTFOUND;

        if (!where.isDirectory())
            return FileManager.ISFILE;

        for (File f : files)
            mv(f, where, su);

        return 0;
    }

    public static int mv(File f, File where, boolean su) throws IOException {
//        if contains spaces, do this with java
        if (!su && (f.getAbsolutePath().contains(" ") || where.getAbsolutePath().contains(" ")))
            FileUtils.moveToDirectory(f, where, false);
//        else to this with shell
        else {
            ArrayList<String> cmds = new ArrayList<>();
            if (su) {
                cmds.add("su");
                cmds.add("-c");
            }
            cmds.add("mv");
            cmds.add("-r");
            cmds.add(f.getAbsolutePath());
            cmds.add(where.getAbsolutePath());

            String[] command = new String[cmds.size()];
            cmds.toArray(command);

            Process p;
            int exit;
            try {
                p = Runtime.getRuntime().exec(command);
                exit = p.waitFor();
            } catch (IOException | InterruptedException e) {
                return FileManager.IOERROR;
            }

            if (exit != 0)
                return FileManager.IOERROR;
        }
        return 0;
    }

    public static int rm(File[] files, boolean su) {
        if (files == null || files.length == 0)
            return FileManager.FILE_NOTFOUND;

        for (File f : files)
            rm(f, su);

        return 0;
    }

    public static int rm(File f, boolean su) {
        if (!su && f.getAbsolutePath().contains(" "))
            FileUtils.deleteQuietly(f);
        else {
            ArrayList<String> cmds = new ArrayList<>();
            if (su) {
                cmds.add("su");
                cmds.add("-c");
            }
            cmds.add("rm");
            cmds.add("-r");
            cmds.add(f.getAbsolutePath());

            String[] command = new String[cmds.size()];
            cmds.toArray(command);

            Process p;
            int exit;
            try {
                p = Runtime.getRuntime().exec(command);
                exit = p.waitFor();
            } catch (IOException | InterruptedException e) {
                return FileManager.IOERROR;
            }

            if (exit != 0)
                return FileManager.IOERROR;
        }
        return 0;
    }

    public static int cp(File[] files, File where, boolean su) throws IOException {
        if (files == null || files.length == 0 || where == null)
            return FileManager.FILE_NOTFOUND;

        if (!where.isDirectory())
            return FileManager.ISFILE;

        for (File f : files)
            cp(f, where, su);

        return 0;
    }

    public static int cp(File f, File where, boolean su) throws IOException {
        if (!su && (f.getAbsolutePath().contains(" ") || where.getAbsolutePath().contains(" "))) {
            if (f.isDirectory())
                FileUtils.copyDirectoryToDirectory(f, where);
            else
                FileUtils.copyFileToDirectory(f, where);
        } else {
            ArrayList<String> cmds = new ArrayList<>();
            if (su) {
                cmds.add("su");
                cmds.add("-c");
            }
            cmds.add("cp");
            cmds.add("-r");
            cmds.add(f.getAbsolutePath());
            cmds.add(where.getAbsolutePath());

            String[] command = new String[cmds.size()];
            cmds.toArray(command);

            Process p;
            int exit;
            try {
                p = Runtime.getRuntime().exec(command);
                exit = p.waitFor();
            } catch (IOException | InterruptedException e) {
                return FileManager.IOERROR;
            }

            if (exit != 0)
                return FileManager.IOERROR;
        }
        return 0;
    }

    public static List<File> lsFile(File f, boolean showHidden) {
        File[] content = f.listFiles();

        Arrays.sort(content, new Comparator<File>() {
            @Override
            public int compare(File lhs, File rhs) {
                if (lhs.isDirectory() && !rhs.isDirectory())
                    return -1;
                if (rhs.isDirectory() && !lhs.isDirectory())
                    return 1;

                return Compare.alphabeticCompare(lhs.getName(), rhs.getName());
            }
        });

        List<File> files = new ArrayList<>();
        for (File u : content)
            if (!u.isHidden() || showHidden)
                files.add(u);

        return files;
    }

    public static int openFile(Context c, File file) {
        if (file == null)
            return FileManager.FILE_NOTFOUND;
        if (file.isDirectory())
            return FileManager.ISDIRECTORY;

        Intent intent = Tuils.openFile(file);

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

    public static String wildcard(String path) {
        if (path == null || !path.contains("*") || path.contains("/"))
            return null;

//        if there is only "*", means that you have to select all files in folder
        String after = path.substring(path.indexOf("*") + 1);
        if (after.length() == 0)
            return ALL;

        return after;
    }

    public static class DirInfo {
        public File file;
        public String notFound;

        public DirInfo(File f, String nF) {
            this.file = f;
            this.notFound = nF;
        }
    }
}

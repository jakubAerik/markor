/*#######################################################
 *
 *   Maintained by Gregor Santner, 2017-
 *   https://gsantner.net/
 *
 *   License of this file: Apache 2.0 (Commercial upon request)
 *     https://www.apache.org/licenses/LICENSE-2.0
 *     https://github.com/gsantner/opoc/#licensing
 *
#########################################################*/
package net.gsantner.opoc.util;


import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@SuppressWarnings({"WeakerAccess", "unused", "SameParameterValue", "SpellCheckingInspection", "deprecation"})
public class FileUtils {
    // Used on methods like copyFile(src, dst)
    private static final int BUFFER_SIZE = 4096;
    private final Activity mContext;
    private static final String PATH_SEPERATOR = "/";
    private static final String pdfExtension = ".pdf";

    public FileUtils(Activity mContext) {
        this.mContext = mContext;
    }

    public static String readTextFileFast(final File file) {
        try {
            return new String(readCloseBinaryStream(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            System.err.println("readTextFileFast: File " + file + " not found.");
        }
        return "";
    }

    public static String readTextFile(final File file) {
        try {
            return readCloseTextStream(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            System.err.println("readTextFile: File " + file + " not found.");
        }

        return "";
    }

    public static String getFormattedDate(File file) {
        Date lastModDate = new Date(file.lastModified());
        String[] formatdate = lastModDate.toString().split(" ");
        String time = formatdate[3];
        String[] formattime =  time.split(":");
        String date = formattime[0] + ":" + formattime[1];
        return formatdate[0] + ", " + formatdate[1] + " " + formatdate[2] + " at " + date;
    }

    public static String getFormattedSize(File file) {
        return String.format("%.2f MB", (double) file.length() / (1024 * 1024));
    }

    public String getFileName(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();
        if (scheme.equals("file"))
            fileName = uri.getLastPathSegment();
        else if (scheme.equals("content")) {
            Cursor cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.getCount() != 0) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                fileName = cursor.getString(columnIndex);
            }
            if (cursor != null)
                cursor.close();
        }
        return fileName;
    }
    public static String getFileName(String path) {
        return path.substring(path.lastIndexOf(PATH_SEPERATOR) + 1);
    }



    public static String readCloseTextStream(final InputStream stream) {
        return readCloseTextStream(stream, true).get(0);
    }

    public static List<String> readCloseTextStream(final InputStream stream, boolean concatToOneString) {
        final ArrayList<String> lines = new ArrayList<>();
        BufferedReader reader = null;
        String line = "";
        try {
            StringBuilder sb = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(stream));

            while ((line = reader.readLine()) != null) {
                if (concatToOneString) {
                    sb.append(line).append('\n');
                } else {
                    lines.add(line);
                }
            }
            line = sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (concatToOneString) {
            lines.clear();
            lines.add(line);
        }
        return lines;
    }

    public static byte[] readBinaryFile(final File file) {
        try {
            return readCloseBinaryStream(new FileInputStream(file), (int) file.length());
        } catch (FileNotFoundException e) {
            System.err.println("readBinaryFile: File " + file + " not found.");
        }

        return new byte[0];
    }

    public static String getFileNameWithoutExtension(String path) {
        String p = path.substring(path.lastIndexOf(PATH_SEPERATOR) + 1);
        p = p.replace(pdfExtension, "");
        return p;
    }

    public static byte[] readCloseBinaryStream(final InputStream stream, int byteCount) {
        final ArrayList<String> lines = new ArrayList<>();
        BufferedInputStream reader = null;
        byte[] buf = new byte[byteCount];
        int totalBytesRead = 0;
        try {
            reader = new BufferedInputStream(stream);
            while (totalBytesRead < byteCount) {
                int bytesRead = reader.read(buf, totalBytesRead, byteCount - totalBytesRead);
                if (bytesRead > 0) {
                    totalBytesRead = totalBytesRead + bytesRead;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return buf;
    }

    public static String getFileDirectoryPath(String path) {
        return path.substring(0, path.lastIndexOf(PATH_SEPERATOR) + 1);
    }


    // Read binary stream (of unknown conf size)
    public static byte[] readCloseBinaryStream(final InputStream stream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return baos.toByteArray();
    }

    public static boolean writeFile(final File file, byte[] data) {
        try {
            OutputStream output = null;
            try {
                output = new BufferedOutputStream(new FileOutputStream(file));
                output.write(data);
                output.flush();
                return true;
            } finally {
                if (output != null) {
                    output.close();
                }
            }
        } catch (Exception ex) {
            return false;
        }
    }

    public static boolean writeFile(final File file, final String content) {
        BufferedWriter writer = null;
        try {
            if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs())
                return false;

            writer = new BufferedWriter(new FileWriter(file));
            writer.write(content);
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean copyFile(final File src, final File dst) {
        // Just touch file if src is empty
        if (src.length() == 0) {
            return touch(dst);
        }

        InputStream is = null;
        FileOutputStream os = null;
        try {
            try {
                is = new FileInputStream(src);
                os = new FileOutputStream(dst);
                byte[] buf = new byte[BUFFER_SIZE];
                int len;
                while ((len = is.read(buf)) > 0) {
                    os.write(buf, 0, len);
                }
                return true;
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            }
        } catch (IOException ex) {
            return false;
        }
    }

    // Returns -1 if the file did not contain any of the needles, otherwise,
    // the index of which needle was found in the contents of the file.
    //
    // Needless MUST be in lower-case.
    public static int fileContains(File file, String... needles) {
        try {
            FileInputStream in = new FileInputStream(file);

            int i;
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            while ((line = reader.readLine()) != null) {
                for (i = 0; i != needles.length; ++i)
                    if (line.toLowerCase(Locale.ROOT).contains(needles[i])) {
                        return i;
                    }
            }

            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public static boolean deleteRecursive(final File file) {
        boolean ok = true;
        if (file.exists()) {
            if (file.isDirectory()) {
                for (File child : file.listFiles())
                    ok &= deleteRecursive(child);
            }
            ok &= file.delete();
        }
        return ok;
    }

    // Example: Check if this is maybe a conf: (str, "jpg", "png", "jpeg")
    public static boolean hasExtension(String str, String... extensions) {
        String lc = str.toLowerCase(Locale.ROOT);
        for (String extension : extensions) {
            if (lc.endsWith("." + extension.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean renameFile(File srcFile, File destFile) {
        if (srcFile.getAbsolutePath().equals(destFile.getAbsolutePath())) {
            return false;
        }

        // renameTo will fail in case of case-changed filename in same dir.Even on case-sensitive FS!!!
        if (srcFile.getParent().equals(destFile.getParent()) && srcFile.getName().toLowerCase(Locale.getDefault()).equals(destFile.getName().toLowerCase(Locale.getDefault()))) {
            File tmpFile = new File(destFile.getParent(), UUID.randomUUID().getLeastSignificantBits() + ".tmp");
            if (!tmpFile.exists()) {
                renameFile(srcFile, tmpFile);
                srcFile = tmpFile;
            }
        }

        if (!srcFile.renameTo(destFile)) {
            if (copyFile(srcFile, destFile) && !srcFile.delete()) {
                if (!destFile.delete()) {
                    return false;
                }
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean renameFileInSameFolder(File srcFile, String destFilename) {
        return renameFile(srcFile, new File(srcFile.getParent(), destFilename));
    }

    public static boolean touch(File file) {
        try {
            if (!file.exists()) {
                new FileOutputStream(file).close();
            }
            return file.setLastModified(System.currentTimeMillis());
        } catch (IOException e) {
            return false;
        }
    }

    // Get relative path to specified destination
    public static String relativePath(File src, File dest) {
        try {
            String[] srcSplit = (src.isDirectory() ? src : src.getParentFile()).getCanonicalPath().split(Pattern.quote(File.separator));
            String[] destSplit = dest.getCanonicalPath().split(Pattern.quote(File.separator));
            StringBuilder sb = new StringBuilder();
            int i = 0;

            for (; i < destSplit.length && i < srcSplit.length; ++i) {
                if (!destSplit[i].equals(srcSplit[i]))
                    break;
            }
            if (i != srcSplit.length) {
                for (int iUpperDir = i; iUpperDir < srcSplit.length; ++iUpperDir) {
                    sb.append("..");
                    sb.append(File.separator);
                }
            }
            for (; i < destSplit.length; ++i) {
                sb.append(destSplit[i]);
                sb.append(File.separator);
            }
            if (!dest.getPath().endsWith("/") && !dest.getPath().endsWith("\\")) {
                sb.delete(sb.length() - File.separator.length(), sb.length());
            }
            return sb.toString();
        } catch (IOException | NullPointerException exception) {
            return null;
        }
    }

    /**
     * Try to detect MimeType by backwards compatible methods
     */
    public static String getMimeType(File file) {
        String guess = null;
        if (file != null && file.exists() && file.isFile()) {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(file));
                guess = URLConnection.guessContentTypeFromStream(is);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ignored) {
                    }
                }
            }

            if (guess == null || guess.isEmpty()) {
                guess = "*/*";
                int dot = file.getName().lastIndexOf(".") + 1;
                if (dot > 0 && dot < file.getName().length()) {
                    switch (file.getName().substring(dot)) {
                        case "md":
                        case "markdown":
                        case "mkd":
                        case "mdown":
                        case "mkdn":
                        case "mdwn":
                        case "rmd":
                            guess = "text/markdown";
                            break;
                        case "txt":
                            guess = "text/plain";
                            break;
                    }
                }
            }
        }
        return guess;
    }

    public static boolean isTextFile(File file) {
        String mime = getMimeType(file);
        return mime != null && mime.startsWith("text/");
    }

    /**
     * Analyze given textfile and retrieve multiple information from it
     * Information is written back to the {@link AtomicInteger} parameters
     */
    public static void retrieveTextFileSummary(File file, AtomicInteger numCharacters, AtomicInteger numLines) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                numLines.getAndIncrement();
                numCharacters.getAndSet(numCharacters.get() + line.length());
            }
        } catch (Exception e) {
            e.printStackTrace();
            numCharacters.set(-1);
            numLines.set(-1);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /**
     * Format filesize to human readable format
     * Get size in bytes e.g. from {@link File} using {@code File#length()}
     */
    public static String getReadableFileSize(long size, boolean abbreviation) {
        if (size <= 0) {
            return "0B";
        }
        String[] units = abbreviation ? new String[]{"B", "kB", "MB", "GB", "TB"} : new String[]{"Bytes", "Kilobytes", "Megabytes", "Gigabytes", "Terabytes"};
        int unit = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, unit))
                + " " + units[unit];
    }
}

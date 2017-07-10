// Copyright 2007, CargoSmart, Inc.


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;



public class FileUtil {
  public static final String FILE_NAME_EXTENSION_TEXT = ".txt";
  public static final String FILE_NAME_EXTENSION_HTML = ".htm";
  public static final String FILE_NAME_EXTENSION_XLS = ".xls";
  private static final int MAXLEN_FILE_FULL_NAME = 80;
  private static final int MAXLEN_FILE_NAME = 70;

  private FileUtil() {
    super();
  }

  public static boolean createNewFile(String filePath) throws IOException {
    File file = new File(filePath);
    if (!file.exists()) {
      return file.createNewFile();
    }
    return true;
  }

  public static boolean checkAndCreateFolder(String folder) throws IOException {
    File file = new File(folder);
    if (!file.exists()) {
      return file.mkdir();
    }
    return true;
  }

  public static boolean checkAndCreateFolders(String folder) throws IOException {
    File file = new File(folder);
    if (!file.exists()) {
      return file.mkdirs();
    }
    return true;
  }

  public static File getFileByCurrentZoneThenStandardZone(
      String parentFilePath, String zonePath, String filePath) {
    String fileFullPathHeaderString = getFormatedFilePathHeader(parentFilePath, zonePath, filePath);
    File file = getFileByCurrentZone(fileFullPathHeaderString, zonePath, filePath);
    if (null == file) {
      file = getFileByStandZone(fileFullPathHeaderString, filePath);
    }
    return file;
  }

  public static File getFileByCurrentZoneThenANZoneThenStandardZone(String parentFilePath, String zonePath, String filePath) {
    String fileFullPathHeaderString = getFormatedFilePathHeader(parentFilePath, zonePath, filePath);
    File file = getFileByCurrentZone(fileFullPathHeaderString, zonePath, filePath);
    
    if (null == file) {
      file = getFileByStandZone(fileFullPathHeaderString, filePath);
    }
    return file;
  }

  private static String getFormatedFilePathHeader(String parentFilePath, String zonePath, String filePath) {
    return parentFilePath.endsWith(File.separator) ? parentFilePath : parentFilePath + File.separator;
  }

  private static File getFileByCurrentZone(String fileFullPathBeginString, String zonePath, String filePath) {
    return checkFile(fileFullPathBeginString + zonePath + File.separator + filePath);
  }



  private static File getFileByStandZone(String fileFullPathBeginString, String filePath) {
    return checkFile(fileFullPathBeginString + filePath);
  }

  public static File checkFile(String pathname) {
    File file = new File(pathname);
    return file.exists() ? file : null;
  }

  public static String getFileName(String pathName) {
    String fileName = pathName;
    int lastInt1 = pathName.lastIndexOf("/");
    int lastInt2 = pathName.lastIndexOf("\\");
    int lastInt = (lastInt1 > lastInt2 ? lastInt1 : lastInt2);
    if (lastInt > 0) {
      fileName = pathName.substring(lastInt + 1);
    }
    return fileName;
  }

  public static String getFileExtension(String fileName) {
    int lastInt = fileName.lastIndexOf(".");
    if (lastInt >= 0) {
      return fileName.substring(lastInt + 1);
    }
    return "";
  }

  public static String getFilePath(String pathname) {
    String filePath = null;
    int lastInt = pathname.lastIndexOf(File.separator);
    if (lastInt >= 0) {
      filePath = pathname.substring(0, lastInt + 1);
    }
    return filePath;
  }

  public static byte[] getBytesFromFile(String path) throws Exception {
    byte[] bytes = null;
    File file = new File(path);
    InputStream is = new FileInputStream(file);
    long length = file.length();

    if (length > Integer.MAX_VALUE) {
      throw new Exception("file too large.");
    }

    bytes = new byte[(int) length];

    int offset = 0;
    int numRead = 0;

    while (offset < bytes.length
        && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
      offset += numRead;
    }

    if (offset < bytes.length) {
      throw new Exception("Could not completely read file.");
    }

    is.close();

    return bytes;
  }

  public static byte[] getBytesFromFile(File file) throws Exception {
    byte[] bytes = null;
    InputStream is = new FileInputStream(file);
    long length = file.length();
    if (length > Integer.MAX_VALUE) {
      throw new Exception("file too large.");
    }
    bytes = new byte[(int) length];
    int offset = 0;
    int numRead = 0;
    while (offset < bytes.length
        && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
      offset += numRead;
    }
    if (offset < bytes.length) {
      throw new Exception("Could not completely read file.");
    }
    is.close();
    return bytes;
  }

  public static String writeToFile(String fileName, InputStream stream,
      Date date, boolean isAppend) throws IOException {
    InputStream source = stream;
    OutputStream dest = null;
    byte[] data = null;
    int fsize = 0;
    int nbytes = 0;

    try {
      String folder = fileName.substring(0, fileName
          .lastIndexOf(File.separator));

      (new File(folder)).mkdirs();

      File tmpfile = new File(fileName);

      dest = new FileOutputStream(tmpfile, isAppend);
      data = new byte[1024];

      while ((nbytes = source.read(data)) != -1) {
        dest.write(data, 0, nbytes);
        fsize = fsize + nbytes;
      }
    } catch (IOException e) {
      throw e;
    } finally {
      if (null != dest) {
        try {
          dest.close();
        } catch (IOException e) {
        }
      }
      if (null != source) {
        try {
          source.close();
        } catch (IOException e) {
        }
      }
    }

    return "" + Math.round(fsize / 1024f); // return KB
  }

  public static String readTxtFile(String fileName, String charset)
      throws IOException {
    return readTxtFileWithEnterSymbol(fileName, charset, "\r\n");
  }

  private static String readTxtFileWithEnterSymbol(String fileName, String charset, String enterSymbol) throws FileNotFoundException,
      IOException {
    FileInputStream fileInputStream = null;
    fileInputStream = new FileInputStream(fileName);
    List<String> lines = readTxtFile(fileInputStream, charset);
    StringBuilder stringBuilder = new StringBuilder();
    for (String line : lines) {
      stringBuilder.append(line + enterSymbol);
    }
    return stringBuilder.toString();
  }

  public static String readTxtFile(InputStream inputStream) throws IOException {
    List<String> lines = readTxtFile(inputStream, null);
    StringBuilder stringBuilder = new StringBuilder();
    for (String line : lines) {
      stringBuilder.append(line + "\r\n");
    }
    return stringBuilder.toString();
  }

  public static List<String> readTxtFile(InputStream inputStream, String charset)
      throws IOException {
    InputStreamReader inputStreamReader = null;
    BufferedReader bufferedReader = null;
    try {
      if (charset != null && !"".equals(charset)) {
        inputStreamReader = new InputStreamReader(inputStream, charset);
      } else {
        inputStreamReader = new InputStreamReader(inputStream);
      }
      bufferedReader = new BufferedReader(inputStreamReader);
      List<String> lines = new ArrayList<String>();
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        lines.add(line);
      }
      return lines;
    } finally {
      if (bufferedReader != null) {
        try {
          bufferedReader.close();
        } catch (IOException e) {
        }
      }
      if (inputStreamReader != null) {
        try {
          inputStreamReader.close();
        } catch (IOException e) {
        }
      }
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (IOException e) {
        }
      }
    }
  }

  public static String readTxtFile(String fileName) throws IOException {
    return readTxtFile(fileName, null);
  }

  public static void writeZipEntry(ZipOutputStream out, String filePath, String entryName) throws IOException {
    InputStream in = null;
    try {
      in = new FileInputStream(filePath);
      out.putNextEntry(new ZipEntry(entryName));
      int bytes = 0;
      byte[] buffer = new byte[1024];
      while ((bytes = in.read(buffer)) > 0) {
        out.write(buffer, 0, bytes);
      }
    } finally {
      if (null != in) {
        in.close();
      }
    }
  }

  public static String readAsTxtFile(String fileName) throws IOException {
    StringBuffer str = new StringBuffer();
    FileReader fr = new FileReader(fileName);
    int charCount = 0;
    char[] buffer = new char[1024];
    while ((charCount = fr.read(buffer)) != -1) {
      if (charCount < 1024) {
        for (int i = 0; i < charCount; i++) {
          str.append(buffer[i]);
        }
        break;
      } else {
        str.append(buffer);
      }
    }
    fr.close();
    return str.toString();
  }

  public static void writeAsTxtFile(String fileName, String fileContent)
      throws IOException {
    FileWriter fw = new FileWriter(fileName);
    fw.write(fileContent, 0, fileContent.length());
    fw.flush();
    fw.close();
  }

  public static boolean isFileNameTooLong(String filePath) {
    String fullName = FileUtil.getFileName(filePath);
    int endIndex = fullName.lastIndexOf('.');
    String name = fullName.substring(0, endIndex == -1 ? fullName.length()
        : endIndex);
    return (fullName.length() > MAXLEN_FILE_FULL_NAME || name.length() > MAXLEN_FILE_NAME);
  }

  public static File[] getFilesInDirectorByFilterName(String dirPath, String fileNameContain){
    File dir = new File(dirPath);
    if(!dir.isDirectory()){
      return null;
    }
    return dir.listFiles(new FilenameFilterImpl(fileNameContain));
  }

  public static String readTextFileToString(String string) throws FileNotFoundException, IOException {
    return readTxtFileWithEnterSymbol(string, null, "\n");
  }

  public static boolean copyFile(String sourceFileName, String targetFileName) {
    File targetFile = new File(targetFileName);
    if (targetFile.exists()) {
      targetFile.delete();
      targetFile = new File(targetFileName);
    }
    FileInputStream from = null;
    FileOutputStream to = null;
    try {
      from = new FileInputStream(sourceFileName);
      to = new FileOutputStream(targetFile);
   
    } catch (Exception e) {
      return false;
    } finally {

    }
    return true;
  }

  public static boolean moveFile(String sourceFileName, String targetFileName) {
    if (copyFile(sourceFileName, targetFileName)) {
      File sourceFile = new File(sourceFileName);
      sourceFile.delete();
    } else {
      return false;
    }
    return true;
  }

 
}

class FilenameFilterImpl implements FilenameFilter {
  private String fileContains;

  public FilenameFilterImpl(String fileContains){
    this.fileContains = fileContains;
  }
  public boolean accept(File arg0, String arg1) {
    
    return true;
  }

}

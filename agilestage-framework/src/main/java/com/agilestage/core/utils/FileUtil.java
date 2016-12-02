/**
 * Copyright (c) All rights reserved.
 */
package com.agilestage.core.utils;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件操作api， 请在可能的情况下尽量使用 {@link org.apache.commons.io.FileUtils}的相应方法
 * 
 * @author <a href="mailto:729824941@qq.com">fengxing</a>
 * 2016年11月29日
 */
public class FileUtil {
    // 文件类型
    public static final String FILE_TYPE_HTML = "html";
    public static final String FILE_TYPE_HTM = "htm";
    public static final String FILE_TYPE_TXT = "txt";
    public static final String FILE_TYPE_JSP = "jsp";
    public static final String FILE_TYPE_JSON = "json";
    public static final String FILE_TYPE_XML = "xml";
    public static final String FILE_TYPE_JS = "js";
    public static final String FILE_TYPE_CSV = "csv";

    // 文件编码
    public static final String FILE_CHARSET_UTF8 = "utf-8";
    public static final String FILE_CHARSET_GBK = "gbk";
    public static final String FILE_CHARSET_GB2312 = "gb2312";

    // 路径分割符
    public static final String FILE_SEPARATOR = "/";
    public static final String URL_SEPARATOR = FILE_SEPARATOR;
    public static final String FILE_DOT = ".";

    /** 默认的zip压缩级别 */
    public static final int DEFAULT_ZIP_LEVEL = 5;
    /** 最大的zip压缩级别 */
    public static final int MAX_ZIP_LEVEL = 9;

    private static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    /**
     * 图片过滤的filter
     */
    private static FileFilter imageFileFilter = new FileFilter() {
        @Override
        public boolean accept(final File file) {
            String fileName = file.getName().toLowerCase();

            return StringUtils.endsWithAny(fileName, ".jpg", ".jpeg", ".gif", ".bmp", ".png");
        }
    };

    /**
     * 将可变参数以“/”为分隔，拼接文件路径（结尾不带分隔符）
     * <p>
     * 若可变参数中存在为空的字符，则所得路径中将以空字符占位，即对应位置会出现多个分隔符，为null的将以字符串“null”占位<br>
     * 例：<br>
     * FileUtil.path((String[]) null) --> ""<br>
     * FileUtil.path("a", "b", "c") --> a/b/c<br>
     * FileUtil.path("", "b", "c") --> /b/c<br>
     * FileUtil.path(null, "b", "c") --> null/b/c<br>
     * FileUtil.path(null, "/b/", "c/") --> null/b/c
     * <p>
     * <b>注：</b>如果需要确保子文件不能超出父目录之外（路径中包含“..”会使得最终的文件位置超出父目录以外），则需使用{@link #subfile(String, String...)}或
     * {@link #subfile(File, String...)}接口
     * 
     * @param pathes 路径集合，集合中的每个元素均可为一个相对路径，如“[a, b/c, d/e/f]”。为null或空时，返回空字符串
     */
    public static String path(String... pathes) {
        String separator = "/";
        StringBuffer sb = new StringBuffer();

        if (null != pathes && pathes.length > 0) {
            for (int i = 0; i < pathes.length; i++) {
                String path = String.valueOf(pathes[i]);

                if (i > 0) {
                    sb.append(separator);
                }
                sb.append(path);
            }
        }

        String path = StringUtils.stripEnd(sb.toString().replaceAll("/{2,}", separator), separator);
        return path;
    }

    /**
     * 文件路径{@link #path(String...) 拼接}，并返回File对象
     * <p>
     * <b>注：</b>如果需要确保子文件不能超出父目录之外（路径中包含“..”会使得最终的文件位置超出父目录以外），则需使用{@link #subfile(String, String...)}或
     * {@link #subfile(File, String...)}接口
     * 
     * @param pathes 路径集合，集合中的每个元素均可为一个相对路径，如“[a, b/c, d/e/f]”
     */
    public static File file(String... pathes) {
        return new File(path(pathes));
    }

    /**
     * 子文件路径{@link #path(String...) 拼接}，并返回File对象
     * <p>
     * <b>注：</b>如果需要确保子文件不能超出父目录之外（路径中包含“..”会使得最终的文件位置超出父目录以外），则需使用{@link #subfile(String, String...)}或
     * {@link #subfile(File, String...)}接口
     * 
     * @param parent 父目录，可为null
     * @param pathes 子路径集合，集合中的每个元素均可为一个相对路径，如“[a, b/c, d/e/f]”。为null或空时，返回空字符串
     */
    public static File file(File parent, String... pathes) {
        return new File(parent, path(pathes));
    }

    /**
     * 获取子文件（最终文件位置不能超出父目录之外）
     * <p>
     * 
     * @param parent 父目录，不可为null或空
     * @param pathes 子路径集合，集合中的每个元素均可为一个相对路径，如“[a, b/c, d/e/f]”
     * @throws IllegalArgumentException 子目录未指定或子目录的最终位置超出父目录之外时抛出
     */
    public static File subfile(String parent, String... pathes) {
        return subfile(new File(parent), pathes);
    }

    /**
     * 获取子文件（最终文件位置不能超出父目录之外）
     * <p>
     * 
     * @param parent 父目录，不可为null，且其路径不能为空
     * @param pathes 子路径集合，集合中的每个元素均可为一个相对路径，如“[a, b/c, d/e/f]”
     * @throws IllegalArgumentException 子目录未指定或子目录的最终位置超出父目录之外时抛出
     */
    public static File subfile(File parent, String... pathes) {
        if (null == parent || StringUtils.isBlank(parent.getPath())) {
            throw new IllegalArgumentException("parent is null or it's path is empty");
        }

        String subPath = path(pathes);
        File sub = new File(parent, subPath);

        if (subPath.contains("..") || subPath.contains(".")) {
            try {
                String parentCanonicalPath = parent.getCanonicalPath();
                String sonCanonicalPath = sub.getCanonicalPath();

                if (!sonCanonicalPath.startsWith(parentCanonicalPath)) {
                    throw new IllegalArgumentException("The specified subfile absolute path is out of parent file");
                }
            } catch (IOException e) {
                throw new RuntimeException("Exception while trying to get file canonical path", e);
            }
        }

        return sub;
    }

    /**
     * 获取子文件/目录相对于父目录的路径
     * <p>
     * 
     * @return 若<code>subFile</code>非<code>parent</code>的子文件/目录，则返回空字符串
     */
    public static String getRelativePath(File parent, File subFile) {
        String parentPath = parent.getAbsolutePath();
        String subFilePath = subFile.getAbsolutePath();

        if (!subFilePath.equals(parentPath) && subFilePath.startsWith(parentPath)) {
            return subFilePath.substring(parentPath.length() + 1);
        } else {
            return "";
        }
    }

    /**
     * 检查并确保path目录的存在
     * 
     * @param path
     * @return
     */
    public static File checkAndCreateFilePath(final String path) {

        File file = new File(path);
        file.mkdirs();

        return file;
    }

    /**
     * 复制整个目录的内容
     * 
     * @param srcDirName 待复制目录的目录名
     * @param destDirName 目标目录名
     * @param overlay 如果目标目录存在，是否覆盖
     * @throws IOException
     */
    public static void
            copyDirectory(final String srcDirName, final String destDirName, final boolean overlay) throws IOException {

        // 判断源目录是否存在
        File srcDir = new File(srcDirName);
        File destDir = new File(destDirName);

        if (!srcDir.exists() || !srcDir.isDirectory()) {
            throw new IOException("src file is not exists or is not a directory.");
        } else if (destDir.exists() && !overlay) {
            throw new IOException("dest dir is already exists.");
        } else if (!destDir.mkdirs()) {
            throw new IOException("dest dir may be not writable.");
        } else {

            File[] files = srcDir.listFiles();
            for (File file : files) {
                // 复制文件
                String destPath = new File(destDirName, file.getName()).getAbsolutePath();

                if (file.isFile()) {
                    copyFile(file.getAbsolutePath(), destPath, overlay);
                } else if (file.isDirectory()) {
                    copyDirectory(file.getAbsolutePath(), destPath, overlay);

                }
            }
        }
    }

    /**
     * 把源文件拷贝到服务器中的目标文件,覆盖原文件
     * 
     * @param src 源文件
     * @param dst 目标文件
     * @throws IOException
     */
    public static void copyFile(final File src, final File dst) throws IOException {

        FileUtils.copyFile(src, dst);
    }

    /**
     * 把源文件拷贝到服务器中的目标文件,覆盖原文件
     * 
     * @param src 源文件
     * @param dst 目标文件
     * @return
     * @throws Exception
     */
    public static void copyFile(final File src, final File dst, final boolean overlay) throws IOException {
        if (!dst.exists() || overlay) {
            copyFile(src, dst);
        }
    }

    /**
     * 把源文件拷贝到服务器中的目标文件,覆盖原文件
     * 
     * @param src 源文件
     * @param dst 目标文件
     * @throws Exception
     */
    public static void copyFile(final String src, final String dst) throws IOException {
        copyFile(src, dst, true);
    }

    /**
     * 复制单个文件
     * 
     * @param srcFileName 待复制的文件名
     * @param descFileName 目标文件名
     * @param overlay 如果目标文件存在，是否覆盖
     * @throws IOException
     */
    public static void
            copyFile(final String srcFileName, final String destFileName, final boolean overlay) throws IOException {

        File srcFile = new File(srcFileName);
        File destFile = new File(destFileName);

        copyFile(srcFile, destFile, overlay);
    }

    /**
     * 生成文件
     * 
     * @param content
     * @param fileNme
     * @param filePath
     * @return
     * @throws IOException
     */
    private static String createFile(final String content, final String filePath, final String fileName,
                                     final String chartSet) throws IOException {

        checkAndCreateFilePath(filePath);
        File dest = new File(filePath, fileName);
        Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dest), chartSet));
        out.write(content);
        out.close();

        return dest.getCanonicalPath();

    }

    /**
     * 解压jar包
     * <p>
     * 优先选择{@link ZipUtils#unzip(ZipFile, File, String...) unzip}等接口
     * 
     * @param jarFile jar文件
     * @param destDir 解压目录
     * @param overlay 是否允许覆盖
     * @return 解压是否成功
     */
    public static boolean extractJar(final File jarFile, final File destDir, final String[] filters,
                                     final boolean overlay) {
        return extractJar(jarFile.getAbsolutePath(), destDir.getAbsolutePath(), "", "", filters, overlay);
    }

    /**
     * 解压jar包下指定文件
     * <p>
     * 注意，请在试用后关闭输入流
     * 
     * @param zipPath zip文件路径
     * @param zipFolderName 解压jar子目录
     * @return inputStream 获取失败时返回null
     */
    public static InputStream getZipInputStream(final ZipFile jarFile, final String jarFolderName) {
        InputStream inputStream = null;
        try {
            ZipEntry jarEntry = jarFile.getEntry(jarFolderName);
            if (jarEntry != null && !jarEntry.isDirectory()) {
                inputStream = jarFile.getInputStream(jarEntry);
            }
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }

        return inputStream;
    }

    /**
     * 解压jar包
     * <p>
     * 优先选择{@link ZipUtils#unzip(ZipFile, File, String...) unzip}等接口
     * 
     * @param jarPath 解压jar包路径
     * @param destDirPath 解压文件存放路径
     * @param jarFolderName 解压jar目录
     * @param overlay 是否覆盖
     * @return 解压是否成功
     */
    public static boolean extractJar(final String jarPath, final String destDirPath, final String jarFolderName,
                                     final boolean overlay) {
        return extractJar(jarPath, destDirPath, jarFolderName, jarFolderName, null, overlay);
    }

    /**
     * 解压jar包，将jar中指定文件夹下条目解压到指定目录中 extractFromJar("D:/FVSD_EP/WorkSpace/process_Change.jar", "D:/process_Change/",
     * "resources/UI/", "UI/", null, true)
     * <p>
     * 优先选择{@link ZipUtils#unzip(ZipFile, File, String...) unzip}等接口
     * 
     * @param jarPath 解压jar包路径
     * @param destDirPath 解压文件存放路径
     * @param jarFolderName 解压jar目录
     * @param destFolderName 解压后dest文件目录
     * @param filters 过滤条件
     * @param overlay 如果目标文件存在，是否覆盖
     * @return 解压是否成功
     */
    public static boolean extractJar(final String jarPath, final String destDirPath, String jarFolderName,
                                     String destFoldName, String[] filters, final boolean overlay) {
        File jarFile = new File(jarPath);
        // 目标文件是否存在
        if (!jarFile.exists()) {
            return false;
        }
        // 解压jar目录，默认全部解压
        String extractFolder = StringUtils.defaultString(jarFolderName);

        // 解压后dest文件目录，默认与解压前jarFolder相同
        String destFolder = StringUtils.defaultString(destFoldName, extractFolder);

        String[] filterArr = null == filters ? new String[] {} : filters;

        InputStream input = null;
        OutputStream output = null;
        try {
            JarFile file = new JarFile(jarFile);
            Enumeration<JarEntry> entrys = file.entries();

            JarEntry jarEntry = null;
            String jarEntryName = "";
            String fileType = "";
            String destPath = "";
            File destFile = null;
            boolean isFilted = false;
            while (entrys.hasMoreElements()) {
                isFilted = false;
                jarEntry = entrys.nextElement();
                jarEntryName = jarEntry.getName();

                // 文件类型(目录)过滤
                if (jarEntryName.lastIndexOf(FILE_DOT) > -1) {
                    fileType = jarEntryName.substring(jarEntryName.lastIndexOf(FILE_DOT));
                } else {
                    fileType = jarEntryName;
                }
                for (String filterStr : filterArr) {
                    if (fileType.equals(filterStr)) {
                        isFilted = true;
                    }
                }
                if (isFilted) {
                    continue;
                }

                // 解压目录
                if (!jarEntryName.startsWith(extractFolder)) {
                    continue;
                }
                destPath = jarEntryName.replaceFirst(extractFolder, destFolder);
                destFile = new File(destDirPath, destPath);
                // 判断是否是目录（问题 jarEntry.isDirectory()只能识别以'/'为结尾目录）
                if (jarEntry.isDirectory()) {
                    if (!destFile.exists()) {
                        destFile.mkdirs();
                    }
                } else {
                    if (!destFile.exists()) {
                        destFile.getParentFile().mkdirs();
                    } else if (!overlay) {
                        continue;
                    } else {
                        destFile.delete();
                    }
                    output = new BufferedOutputStream(new FileOutputStream(destFile));
                    input = file.getInputStream(jarEntry);

                    IOUtils.copy(input, output);

                    output.close();
                    input.close();
                }
            }

            file.close();
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
            return false;
        } finally {
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(output);
        }
        return true;
    }

    /**
     * 将字符串写入文件
     * 
     * @param content 文件内容
     * @param fileName 文件名
     * @param filePath 文件路径
     * @param fileType 扩展名
     * @param charSet 字符集
     * @throws IOException
     */
    public static String genFile(final String content, final String fileName, final String filePath,
                                 final String fileType, final String charSet) throws IOException {
        return createFile(content, filePath, fileName + FILE_DOT + fileType, charSet);
    }

    /**
     * 生成html文件
     * 
     * @param content
     * @param fileName
     * @param filePath
     * @param fileType
     * @return
     * @throws IOException
     */
    public static String genHtml(final String content, final String fileName, final String filePath,
                                 final String charSet) throws IOException {
        return createFile(content, filePath, fileName + FILE_DOT + FILE_TYPE_HTML, charSet);
    }

    /**
     * 生成jsp文件
     * 
     * @param content
     * @param fileName
     * @param filePath
     * @throws IOException
     */
    public static
            String
            genJsp(final String content, final String fileName, final String filePath, final String charSet)
                                                                                                            throws IOException {
        return createFile(content, filePath, fileName + FILE_DOT + FILE_TYPE_JSP, charSet);
    }

    /**
     * 生成txt文本文件
     * 
     * @param content
     * @param fileName
     * @param filePath
     * @return
     * @throws IOException
     */
    public static
            String
            genTxt(final String content, final String fileName, final String filePath, final String charSet)
                                                                                                            throws IOException {
        return createFile(content, filePath, fileName + FILE_DOT + FILE_TYPE_TXT, charSet);
    }

    /**
     * 获取文件扩展名
     * 
     * @param fileName
     * @return
     */
    public static String getExtension(final String fileName) {
        return fileName.substring(fileName.lastIndexOf(FILE_DOT) + 1);
    }

    /**
     * 获取无扩展名的文件名
     * 
     * @param fileName
     * @return
     */
    public static String getNameWithoutExtension(final String fileName) {
        return fileName.substring(0, fileName.lastIndexOf(FILE_DOT));
    }

    /**
     * 得到某个路径下的图片文件
     * 
     * @param path
     * @return
     */
    public static File[] imageFileInPath(final String path) {
        return new File(path).listFiles(imageFileFilter);
    }

    /**
     * 判断文件路径是否存在
     * 
     * @param path
     * @return boolean
     */
    public static boolean isExists(final String path) {
        return new File(path).exists();
    }

    /**
     * 获取路径下的所有指定类型的文件
     * 
     * @param path
     * @param fileType
     * @return
     */
    public static File[] listFiles(final String path, final String fileType) {
        File directory = new File(path);
        if (!directory.isDirectory()) {
            return null;
        }
        if (fileType != null && !"".equals(fileType)) {

            return directory.listFiles(new FilenameFilter() {

                @Override
                public boolean accept(final File dir, final String name) {
                    return name.endsWith(fileType);
                }

            });
        }
        return directory.listFiles();
    }

    /**
     * 通过路径和文件类型，返回目录下所有该类型文件路径列表
     * 
     * @param path
     * @param fileType
     * @return
     */
    public static List<String> listFilesPath(final String path, final String fileType) {
        List<String> filesPath = new ArrayList<String>();
        File[] files = new File(path).listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                List<String> filesPathTemp = listFilesPath(f.getPath(), fileType);
                if (filesPathTemp != null) {
                    filesPath.addAll(filesPathTemp);
                }
            } else if (f.isFile() && f.getPath().toLowerCase().endsWith(fileType.toLowerCase())) {
                filesPath.add(f.getPath());
            }
        }
        return filesPath.size() > 0 ? filesPath : null;
    }

    /**
     * 将文件或者目录打包成jar文件
     * <p>
     * 优先选择{@link ZipUtils#zip(java.util.zip.ZipOutputStream, File) zip}等接口
     * 
     * @param dirFile 需要打包的文件或目录
     * @param jarFile 输出的jar文件
     * @param jarFolderName jar文件的根目录
     * @param 过滤条件 ,jar文件的根目录默认为空
     * @throws IOException
     */
    public static boolean packageJar(final File dirFile, final File jarFile) throws IOException {
        if (StringUtils.isBlank(dirFile.getPath())) {
            return false;
        }
        return packageJar(dirFile, jarFile, "", null);
    }

    /**
     * 将目录打包
     * <p>
     * 优先选择{@link ZipUtils#zip(java.util.zip.ZipOutputStream, File) zip}等接口
     * 
     * @param dirFile 需要打包的文件或目录
     * @param jarFile 生成的jar文件
     * @param jarFolderName jar文件的根目录
     * @param filters 过滤条件
     * @param 默认压缩级别为5 ,追加模式
     * @throws Exception
     */
    public static boolean packageJar(final File dirFile, final File jarFile, final String jarFolderName,
                                     final String[] filters) throws IOException {
        if (StringUtils.isBlank(dirFile.getPath())) {
            return false;
        }
        return packageJar(dirFile.getAbsolutePath(), jarFile.getAbsolutePath(), jarFolderName, filters, 0, true);
    }

    /**
     * 遍历目录并添加到jar包输出流.
     * <p>
     * 优先选择{@link ZipUtils#zip(java.util.zip.ZipOutputStream, File) zip}等接口
     * 
     * @param srcFile 目录文件名
     * @param jos JAR 输出流
     * @param jarFolderName jar文件的根目录
     * @param filters 文件扩展名过滤条件
     * @throws Exception
     */
    public static void packageJar(final File srcFile, final JarOutputStream jos, String jarFolderName,
                                  final String[] filters) throws IOException {

        if (isExceptFileType(srcFile, filters)) {
            return;
        }

        String jarFolder = jarFolderName;
        if (srcFile.isDirectory()) {

            if (StringUtils.isBlank(jarFolder)) {
                jarFolder = "";
            } else if (!jarFolder.endsWith(FILE_SEPARATOR)) {
                jarFolder += FILE_SEPARATOR;
            }
            jarFolder += srcFile.getName() + FILE_SEPARATOR;

            jos.putNextEntry(new JarEntry(jarFolder));

            String[] fileNames = srcFile.list();
            if (fileNames != null) {
                for (String fileName : fileNames) {// 递归
                    packageJar(new File(srcFile, fileName), jos, jarFolder, filters);
                }
            }
        } else {

            InputStream input = new FileInputStream(srcFile);
            jarFolder = StringUtils.defaultIfBlank(jarFolder, "");
            jos.putNextEntry(new JarEntry(jarFolder + srcFile.getName()));

            IOUtils.copy(input, jos);
            jos.closeEntry();
            IOUtils.closeQuietly(input);
        }
    }

    private static boolean isExceptFileType(File srcFile, String[] extNames) {
        String fileType = "";

        if (srcFile.getName().lastIndexOf(FILE_DOT) > -1) {
            fileType = srcFile.getName().substring(srcFile.getName().lastIndexOf(FILE_DOT));
        } else {
            fileType = srcFile.getName();
        }

        if (null != extNames) {
            for (String filterStr : extNames) {
                if (fileType.equals(filterStr)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 将目录打包
     * <p>
     * 优先选择{@link ZipUtils#zip(java.util.zip.ZipOutputStream, File) zip}等接口
     * 
     * @param dirPath 需要打包的文件或目录路径
     * @param jarPath 生成的jar文件路径
     * @param jarFolderName jar文件的根目录
     * @param filters 过滤条件
     * @param level 压缩级别(0~9)
     * @param isAppend 是否追加或者覆盖 false/true
     * @throws Exception
     */
    public static boolean packageJar(final String dirPath, final String jarPath, String jarFolderName,
                                     String[] filters, int level, final boolean isAppend) throws IOException {

        if (null == dirPath || "".equals(dirPath)) {
            return false;
        }
        File dirFile = new File(dirPath);

        // 源文件、目录不存在
        if (!dirFile.exists()) {
            return false;
        }
        File jarFile = new File(jarPath);

        // 生产的jar文件是否存在，判断是否是追加或者覆盖
        if (jarFile.exists()) {
            if (!isAppend) {
                jarFile.delete();
            }
        } else if (!jarFile.getParentFile().exists()) {

            // 如果目标文件所在目录不存在，则创建目录
            jarFile.getParentFile().mkdirs();
        }

        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarFile), new Manifest());

        try {
            jos.setLevel(level < 0 || level > MAX_ZIP_LEVEL ? DEFAULT_ZIP_LEVEL : level);
            packageJar(dirFile, jos, jarFolderName, filters);

        } catch (IOException e) {
            log.debug(e.getMessage(), e);
            return false;
        } finally {
            if (jos != null) {
                jos.close();
            }
        }
        return true;
    }

    /**
     * 将文件以字符串形式读到内存中
     * 
     * @param filepath
     * @return
     * @throws IOException
     */
    public static String readFileToString(final String filepath) throws IOException {

        String retString = "";
        FileInputStream fileinputstream = null;

        try {
            fileinputstream = new FileInputStream(filepath);
            readFileToString(fileinputstream);
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        } finally {
            if (null != fileinputstream) {
                fileinputstream.close();
            }
        }
        return retString;
    }

    /**
     * 将文件以字符串形式读到内存中
     * 
     * @param filepath
     * @param charset
     * @return
     * @throws IOException
     */
    public static String readFileToString(final String filepath, final String charset) throws IOException {
        String retString = "";
        FileInputStream fileinputstream = null;

        try {
            fileinputstream = new FileInputStream(filepath);
            retString = readFileToString(fileinputstream, charset);
        } catch (FileNotFoundException e) {
            log.debug(e.getMessage(), e);
        } finally {
            if (null != fileinputstream) {
                fileinputstream.close();
            }
        }

        return retString;
    }

    /**
     * 读取流中的数据生成字符串
     * 
     * @param input
     * @return
     * @throws IOException
     */
    public static String readFileToString(final InputStream input) throws IOException {
        String retString = "";

        try {
            int i = input.available();
            byte[] byte0 = new byte[i];
            input.read(byte0);
            retString = new String(byte0);
        } catch (FileNotFoundException e) {
            log.debug(e.getMessage(), e);
        }

        return retString;
    }

    /**
     * 读取流中的数据生成字符串
     * 
     * @param input 输入流
     * @param charset 字符编码
     * @return
     * @throws IOException
     */
    public static String readFileToString(final InputStream input, final String charset) throws IOException {
        String retString = "";

        try {
            int i = input.available();
            byte[] byte0 = new byte[i];
            input.read(byte0);
            retString = new String(byte0, charset);
        } catch (FileNotFoundException e) {
            log.debug(e.getMessage(), e);
        }

        return retString;
    }

    /**
     * 删除文件或文件夹
     * 
     * @param filePath
     * @return boolean
     */
    public static boolean removeFile(final String filePath) {
        File file = new File(filePath);

        if (file.isDirectory()) {
            String[] tempList = file.list();
            File temp = null;
            for (String element : tempList) {
                temp = new File(filePath, element);
                if (temp.isFile()) {
                    temp.delete();
                } else {
                    removeFile(filePath + File.separator + element + File.separator);
                }
            }

        }
        return new File(filePath).delete();
    }

}

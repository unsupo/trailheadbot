package utilities;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class FileOptions {

    private static Gson gson = new GsonBuilder()
            .serializeSpecialFloatingPointValues()
            .create();

    public static Gson getGson(){
        return gson;
    }

    public final static String OS = System.getProperty("os.name").toLowerCase();
    final static public DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String SEPERATOR = System.getProperty("file.separator"),
            DEFAULT_DIR = System.getProperty("user.dir") + SEPERATOR,
            RESOURCE_DIR = cleanFilePath(DEFAULT_DIR+"/ogamebotapp/src/main/resources/");
    public static final String WEB_DRIVER_DIR = DEFAULT_DIR + "config" + SEPERATOR + "web_drivers" + SEPERATOR;



    public static void main(String[] args) throws IOException {
//		String path = System.getProperty("user.dir");
//		String s = System.getProperty("file.separator");
//		String out = path + s + ".." + s + "all_jars", in = path + s + ".." + s
//				+ "jars";
//		moveAllFiles(in, out);

//        getAllFilesRegex(getBaseDirectories()[0].getAbsolutePath(), "mvn").forEach(System.out::println);

//        getPermissionSet(755).forEach(System.out::println);
//        new File(DEFAULT_DIR+"test").createNewFile();

        runConcurrentProcessNonBlocking(IntStream.range(0,10).boxed().map(a->(Callable)()->{Thread.sleep(2000);
            System.out.println("Done with 2000: "+a);return null;}).collect(Collectors.toList()));


        runConcurrentProcessNonBlocking(IntStream.range(0,10).boxed().map(a->(Callable)()->{Thread.sleep(1000);
            System.out.println("Done with 1000: "+a);return null;}).collect(Collectors.toList()));


        runConcurrentProcessNonBlocking(IntStream.range(0,10).boxed().map(a->(Callable)()->{Thread.sleep(3000);
            System.out.println("Done with 3000: "+a);return null;}).collect(Collectors.toList()));

        System.out.println("Done with all");

    }

    public static Path setPermissionUnix(int octalPermission, String file) throws IOException{
        return setPermissionUnix(convertOctalToText(octalPermission),file);
    }public static Path setPermissionUnix(String unixPermission, String file) throws IOException {
        return Files.setPosixFilePermissions(new File(file).toPath(),getPermissionSet(unixPermission));
    }
    /**
     * Expects unixpermission like -r--r--r-- or
     * owner read, group read, all users read
     * read,write,getMessages
     *
     * o(r,w,e),g(r,w,e),u(r,w,e)
     *
     * @param unixPermission
     * @return
     */
    private static Set<PosixFilePermission> getPermissionSet(String unixPermission){
        Set<PosixFilePermission> perms = new HashSet<>();
        char[] chars = unixPermission.toCharArray();
        if(chars.length != 9 && chars.length != 10)
            throw new IllegalArgumentException("Unix Permission must be of length 9 or 10: "+chars.length+" = "+unixPermission);
        if(chars.length == 10)
            chars = unixPermission.substring(1).toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if(chars[i] == '-') continue;
            if(i<3)
                switch (chars[i]){
                    case 'r': perms.add(PosixFilePermission.OWNER_READ); break;
                    case 'w': perms.add(PosixFilePermission.OWNER_WRITE); break;
                    case 'x': perms.add(PosixFilePermission.OWNER_EXECUTE); break;
                }
            if(i>=3 && i<6)
                switch (chars[i]){
                    case 'r': perms.add(PosixFilePermission.GROUP_READ); break;
                    case 'w': perms.add(PosixFilePermission.GROUP_WRITE); break;
                    case 'x': perms.add(PosixFilePermission.GROUP_EXECUTE); break;
                }
            if(i>=6)
                switch (chars[i]){
                    case 'r': perms.add(PosixFilePermission.OTHERS_READ); break;
                    case 'w': perms.add(PosixFilePermission.OTHERS_WRITE); break;
                    case 'x': perms.add(PosixFilePermission.OTHERS_EXECUTE); break;
                }
        }
        return perms;
    }
    private static String convertOctalToText(int octal){
        StringBuilder sb = new StringBuilder();
        for (char s : (octal+"").toCharArray()) {
            int num = Integer.parseInt(s+"");
            sb.append((num & 4) == 0 ? '-' : 'r');
            sb.append((num & 2) == 0 ? '-' : 'w');
            sb.append((num & 1) == 0 ? '-' : 'x');
        }
        return sb.toString();
    }
    private static Set<PosixFilePermission> getPermissionSet(int octalPermission){
        return getPermissionSet(convertOctalToText(octalPermission));
    }


    public static ExecutorService runSystemProcess(String command) throws IOException {
        return runSystemProcess(Runtime.getRuntime().exec(command));
    }public static ExecutorService runSystemProcess(String command, String directory) throws IOException {
        return runSystemProcess(new ProcessBuilder(Arrays.asList(command.split(" ")))
                .directory(new File(directory))
                .start());
    }public static ExecutorService runSystemProcess(Process process) throws IOException {
        return runConcurrentProcess(Arrays.asList(process.getInputStream(),process.getErrorStream()).stream()
                .map(a->(Callable)()->{
                    BufferedReader br = new BufferedReader(new InputStreamReader(a));
                    String l;
                    while ((l = br.readLine()) != null)
                        System.out.println(l);
                    return null;
                }).collect(Collectors.toList()));
    }

    public static ExecutorService runConcurrentProcess(Callable callable){
        return runConcurrentProcess(Arrays.asList(callable));
    }    public static ExecutorService runConcurrentProcess(Callable callable, int threads){
        return runConcurrentProcess(Arrays.asList(callable), threads);
    }    public static ExecutorService runConcurrentProcess(Callable callable, int time, TimeUnit timeUnit){
        return runConcurrentProcess(Arrays.asList(callable), time, timeUnit);
    }    public static ExecutorService runConcurrentProcess(Callable callable, int threads, int time, TimeUnit timeUnit){
        return runConcurrentProcess(Arrays.asList(callable), threads, time, timeUnit);
    }
    public static ExecutorService runConcurrentProcess(List<Callable> callables){
        return runConcurrentProcess(callables, callables.size(), 5, TimeUnit.MINUTES);
    }
    public static ExecutorService runConcurrentProcess(List<Callable> callables, int threads){
        return runConcurrentProcess(callables, threads, 5, TimeUnit.MINUTES);
    }
    public static ExecutorService runConcurrentProcess(List<Callable> callables, int time, TimeUnit timeUnit){
        return runConcurrentProcess(callables, callables.size(), time, timeUnit);
    }
    public static ExecutorService runConcurrentProcess(List<Callable> callables, int threads, int time, TimeUnit timeUnit){
        ExecutorService service = Executors.newFixedThreadPool(threads);
        callables.forEach(a->{
            service.submit(a);
        });

        try {
//            System.out.println("attempt to shutdown executor");
            service.shutdown();
            service.awaitTermination(time, timeUnit);
        }
        catch (InterruptedException e) {
//            System.err.println("tasks interrupted");
        }
        finally {
//            if (!service.isTerminated()) {
////                System.err.println("cancel non-finished tasks");
//            }
            service.shutdownNow();
        }
//        while (!service.isTerminated() && !service.isShutdown())
//            Thread.sleep(1000);
        return service;
    }


    public static ExecutorService runConcurrentProcessNonBlocking(Callable callable){
        return runConcurrentProcessNonBlocking(Arrays.asList(callable));
    }    public static ExecutorService runConcurrentProcessNonBlocking(Callable callable, int threads){
        return runConcurrentProcessNonBlocking(Arrays.asList(callable), threads);
    }    public static ExecutorService runConcurrentProcessNonBlocking(Callable callable, int time, TimeUnit timeUnit){
        return runConcurrentProcessNonBlocking(Arrays.asList(callable), time, timeUnit);
    }    public static ExecutorService runConcurrentProcessNonBlocking(Callable callable, int threads, int time, TimeUnit timeUnit){
        return runConcurrentProcessNonBlocking(Arrays.asList(callable), threads, time, timeUnit);
    }
    public static ExecutorService runConcurrentProcessNonBlocking(List<Callable> callables){
        return runConcurrentProcessNonBlocking(callables, callables.size(), 5, TimeUnit.MINUTES);
    }
    public static ExecutorService runConcurrentProcessNonBlocking(List<Callable> callables, int threads){
        return runConcurrentProcessNonBlocking(callables, threads, 5, TimeUnit.MINUTES);
    }
    public static ExecutorService runConcurrentProcessNonBlocking(List<Callable> callables, int time, TimeUnit timeUnit){
        return runConcurrentProcessNonBlocking(callables, callables.size(), time, timeUnit);
    }
    public static ExecutorService runConcurrentProcessNonBlocking(List<Callable> callables, int threads, int time, TimeUnit timeUnit){
        ExecutorService service = Executors.newFixedThreadPool(threads);
        callables.forEach(a->{
            service.submit(a);
        });
        service.shutdown();
        return service;
    }

    public static HashMap<String,List<String>> getZipFileContents(String path) throws IOException {
        HashMap<String,List<String>> fileContents = new HashMap<>();
        ZipFile zip = new ZipFile(path);
        for (Enumeration e = zip.entries(); e.hasMoreElements(); ) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            if (!entry.isDirectory()) {
                //TODO add other types of files to process
//                if (FilenameUtils.getExtension(entry.getName()).equals("png")) {
//                    byte[] image = getImage(zip.getInputStream(entry));
//                    //do your thing
//                } else
                if (FilenameUtils.getExtension(entry.getName()).equals("txt")) {
                    List<String> fileSeperator = new ArrayList<>();
                    StringBuilder out = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry)));
                    String line;
                    try {
                        while ((line = reader.readLine()) != null)
                            fileSeperator.add(line);
//                            out.append(line);
                    } catch (IOException ee) {
                        // do something, probably not a text file
//                            ee.printStackTrace();
                    }
                    fileContents.put(entry.getName(), fileSeperator);
                }
            }
        }
        return fileContents;
    }

    public static File[] getBaseDirectories() {
        return File.listRoots();
    }

    public static List<File> getAllDirectories(String path){
        return new ArrayList<>(Arrays.asList(new File(path).listFiles()))
                .stream().filter(a->a.isDirectory()).collect(Collectors.toList());
    }

    public static List<File> findOnFileSystem(String regex) throws IOException {
        Pattern p = Pattern.compile(regex);
        List<File> files = new ArrayList<>();
        for (File f : getBaseDirectories())
            files.addAll(
//					Files.walk(f.toPath())
//							.filter(fp->)
//							.filter(fp->p.matcher(fp.toFile().getName()).find())

                    Files.find(f.toPath(), Integer.MAX_VALUE,
                            (fp, fa) -> Files.isReadable(fp) && Files.isWritable(fp) && p.matcher(fp.toFile().getName()).find()
                    )
                            .map(a -> a.toFile())
                            .collect(Collectors.toList()));

        return files;
    }

    public static String cleanFilePath(String filePath) {
        String regex = "\\[\\*replace_me\\*\\]";
        filePath = filePath.replaceAll("/", regex);
        filePath = filePath.replaceAll("\\\\", regex);
        return filePath.replaceAll(regex, Matcher.quoteReplacement(System.getProperty("file.separator")));
    }

    public static void copyFileUtil(File from, File to) throws IOException {
        FileUtils.copyFile(from, to);
    }

    public static void downloadFile(String link, String path) throws IOException {
        URL website = new URL(link);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(path);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
    }


    public static void deleteDirectory(String path) throws IOException {
        FileUtils.deleteDirectory(new File(path));
//		Files.delete(new File(path).toPath());
    }


    public static void writeToFileOverWrite(String filePath, String contents) throws IOException {
        FileOutputStream out = new FileOutputStream(filePath);
        out.write(contents.getBytes());
        out.close();
    }

    public static BufferedWriter writeToFileAppend(String file, String contents) throws IOException {
        BufferedWriter bw = null;
        try {
            bw = new BufferedWriter(new FileWriter(file, true));
        } catch (FileNotFoundException fnfe) {
            String[] split = file.split("\\\\");
            new File(file.substring(0, file.indexOf(split[split.length - 1]))).mkdirs();
            return writeToFileAppend(file, contents);
        }
        bw.write(contents);
        bw.newLine();
        bw.flush();
        return bw;
    }

    public static String readFileIntoString(String path) throws IOException {
        return readFileIntoListString(path).stream().collect(Collectors.joining("\n"));
    }public static List<String> readFileIntoListString(String path) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(path));
        String line;
        List<String> result = new ArrayList<>();
        while ((line = br.readLine()) != null)
            result.add(line);
        br.close();
        return result;
    }
    public static List<File> getAllFilesWithName(String path, String name) throws IOException{
        List<File> files = new ArrayList<>();
        _getAllFiles(path, files);
        return files.stream().filter(a->a.getName().equals(name)).collect(Collectors.toList());
    }public static List<File> getAllFilesContains(String path, String contains) throws IOException {
        List<File> files = new ArrayList<>();
        _getAllFiles(path, files);
        return files.stream().filter(a->a.getName().contains(contains)).collect(Collectors.toList());
    }public static List<File> getAllFilesRegex(String path, String regex) throws IOException {
        List<File> files = new ArrayList<>();
        _getAllFiles(path, files);
        return files.stream().filter(a->Pattern.compile(regex).matcher(a.getName()).find()).collect(Collectors.toList());
    }public static List<File> getAllFilesEndsWith(String path, String endsWith) throws IOException{
        List<File> files = new ArrayList<>();
        _getAllFiles(path,files);
        return files.stream().filter(a->a.getName().endsWith(endsWith)).collect(Collectors.toList());
    }public static List<File> getAllFiles(String path, String contains) throws IOException{
        List<File> files = new ArrayList<>();
        _getAllFiles(path,files);
        return files.stream().filter(a->a.getName().contains(contains)).collect(Collectors.toList());
    }public static List<File> getAllFiles(String path) throws IOException{
        List<File> files = new ArrayList<>();
        _getAllFiles(path,files);
        return files;
    }private static void _getAllFiles(String path,List<File> files) throws IOException{
        if(!Files.isReadable(Paths.get(path))) return;
        for(File f : new File(path).listFiles())
            if(f.isDirectory())
                _getAllFiles(f.getAbsolutePath(), files);
            else
                files.add(f);
    }

    public static List<File> findFile(String path, String name) throws IOException {
        final List<File> foundFiles = new ArrayList<>();
        Files.walk(Paths.get(path))
                .forEach(
                        filePath -> {
                            if (Files.isRegularFile(filePath)
                                    && filePath.getFileName().toString()
                                    .contains(name)) {
//								System.out.println(filePath);
                                foundFiles.add(filePath.toFile());
                            }
                        });
        return foundFiles;
    }public static List<File> findFileRegex(String path, String regex) throws IOException {
        List<File> files = new ArrayList<>();
        Files.walk(Paths.get(path))
                .forEach(
                        filePath -> {
                            if (Files.isRegularFile(filePath)
                                    && Pattern.compile(regex).matcher(filePath.getFileName().toString()).find()) {
                                files.add(filePath.toFile());
                            }
                        });
        return files;
    }


    public static void moveAllFiles(String in, String out) throws IOException {
        String s = System.getProperty("file.separator");
        for (File f : new File(in).listFiles())
            if (f.isFile() && f.getAbsolutePath().endsWith(".jar"))
                copyFile(f, new File(out + s + f.getName()));
            else if (f.isDirectory())
                moveAllFiles(f.getAbsolutePath(), out);
    }
    public static void copyFile(String source, String dest) throws IOException{
        copyFile(new File(source), new File(dest));
    }
    public static void copyFile(File source, File dest) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(source);
            output = new FileOutputStream(dest);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
        } finally {
            input.close();
            output.close();
        }
    }

    public static void renameAllFiles(String path, String ext) {
        for (File f : new File(path).listFiles())
            if (f.isFile())
                f.renameTo(new File(f.getAbsolutePath() + ext));
    }

    private static int next = 0;
    public static int getNext() {
        return next++;
    }


    public static void setLogger(String loggerString) {
//        String cn = "org.apache.logging.log4j.jul.LogManager";
        System.setProperty("java.util.logging.manager",loggerString);
        java.util.logging.LogManager lm = java.util.logging.LogManager.getLogManager();
        if (!loggerString.equals(lm.getClass().getName())) {
            try {
                ClassLoader.getSystemClassLoader().loadClass(loggerString);
            } catch (ClassNotFoundException cnfe) {
                throw new IllegalStateException("Jars not in system class path.", cnfe);
            }
            throw new IllegalStateException("Found " + lm.getClass().getName() + " set as launch param instead.");
        }
    }
}


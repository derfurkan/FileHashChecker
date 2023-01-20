package de.furkan.filehashchecker;

import java.io.File;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.Adler32;
import java.util.zip.CRC32;

public class HashThread implements Runnable {

    private final boolean isHostThread;
    private final java.lang.Thread thread;
    private final List<HashThread> currentThreads = new ArrayList<>();
    Timer timer;
    private HashMap<File, String> checkList;
    private File checkFile;

    private long time;

    public HashThread(boolean isHostThread, SortedMap<File, String> checkFiles, HashMap<File, String> checkList) {
        this.isHostThread = isHostThread;
        this.thread = new java.lang.Thread(this, isHostThread ? "HostThread" : "HashThread");
        if (!isHostThread) {
            this.checkList = checkList;
            this.thread.start();

        } else {
            this.thread.start();
            currentThreads.clear();

            if (!Core.getInstance().getConfigManager().getCurrentConfig().multiThreading()) {
                HashThread checkThread = new HashThread(false, null, new HashMap<>(checkFiles));
                currentThreads.add(checkThread);
                try {
                    checkThread.thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                endOperation();
                return;
            }
            splitMap(checkFiles, checkFiles.size() / Core.getInstance().getConfigManager().getCurrentConfig().threads()).forEach(fileStringSortedMap -> {
                HashThread hashThread = new HashThread(false, null, new HashMap<>(fileStringSortedMap));
                currentThreads.add(hashThread);
            });
            for (HashThread currentThread : currentThreads) {
                try {
                    currentThread.thread.join();
                    currentThread.checkList.forEach((file, s) -> checkFiles.remove(file));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            if (checkFiles.size() == 0) {
                endOperation();
            } else {
                HashThread hashThread = new HashThread(false, null, new HashMap<>(checkFiles));
                try {
                    hashThread.thread.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                endOperation();
            }
        }

    }

    private void endOperation() {
        if (isHostThread) {
            timer.cancel();
            pasteStatus();
            System.out.println("\n Freeing up memory...");
            System.gc();
            System.runFinalization();
            System.out.println(" Successfully checked all files!");
        }
    }

    public void abortOperation() {
        if (isHostThread) {
            timer.cancel();
            System.out.println(" Aborting operation...");
            System.out.println(" Freeing up memory...");
            currentThreads.forEach(t -> t.thread.interrupt());
            System.gc();
            System.runFinalization();
            System.exit(0);
        }
    }


    private void pasteStatus() {
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (Exception ignored) {
        }

        StringBuilder stringBuilder = new StringBuilder("\n\n");

        Core.getInstance().getAllFiles().forEach(file -> {
            stringBuilder.append(file.getName()).append(" | ").append(Core.getInstance().getCheckedFiles().contains(Core.getInstance().getConfigManager().getCurrentConfig().inputFolder().getPath() + file.getPath()) ? "Valid" : Core.getInstance().getInvalidFiles().contains(Core.getInstance().getConfigManager().getCurrentConfig().inputFolder().getPath() + file.getPath()) ? "Invalid" : Core.getInstance().getNotFoundFiles().contains(Core.getInstance().getConfigManager().getCurrentConfig().inputFolder().getPath() + file.getPath()) ? "Not Found" : "...").append("\n");
        });

        currentThreads.removeIf(hashThread -> !hashThread.thread.isAlive());
        long hours = time / 3600L;
        long secondsLeft = time - hours * 3600L;
        long minutes = secondsLeft / 60L;
        long remainder = secondsLeft - minutes * 60L;


        stringBuilder.append("\n ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append(" ").append(hours > 9 ? hours : "0" + hours).append(":").append(minutes > 9 ? minutes : "0" + minutes).append(":").append(remainder > 9 ? remainder : "0" + remainder).append(Core.getInstance().getConfigManager().getCurrentConfig().multiThreading() ? " [Running with " + currentThreads.size() + " Threads]" : " [Running single threaded]\n Currently Checking: " + (currentThreads.size() == 0 ? "None" : currentThreads.get(0).checkFile.getName())).append("\n\n ").append(Core.getInstance().getInvalidFiles().size() + Core.getInstance().getCheckedFiles().size() + Core.getInstance().getNotFoundFiles().size()).append("/").append(Core.getInstance().getAllFiles().size()).append(" (").append(calculatePercentage()).append("%)\n  ").append(Core.getInstance().getInvalidFiles().size()).append(" Invalid files\n  ").append(Core.getInstance().getCheckedFiles().size()).append(" Valid files\n  ").append(Core.getInstance().getNotFoundFiles().size()).append(" Files missing\n\n Total memory: ").append(Runtime.getRuntime().maxMemory() == Long.MAX_VALUE ? "No Limit" : Runtime.getRuntime().maxMemory() / 1_000_000).append("Mb").append("\n Free memory: ").append(Runtime.getRuntime().freeMemory() / 1_000_000).append("Mb\n\n Press CTRL+C to abort!");
        System.out.println(stringBuilder);
    }

    private String calculatePercentage() {
        int total = Core.getInstance().getAllFiles().size();
        int checked = Core.getInstance().getCheckedFiles().size();
        int invalid = Core.getInstance().getInvalidFiles().size();
        int missing = Core.getInstance().getNotFoundFiles().size();
        return String.valueOf((checked + invalid + missing) * 100 / total);
    }

    private <K, V> List<SortedMap<K, V>> splitMap(final SortedMap<K, V> map, final int size) {
        List<K> keys = new ArrayList<>(map.keySet());
        List<SortedMap<K, V>> parts = new ArrayList<>();
        final int listSize = map.size();
        for (int i = 0; i < listSize; i += size) {
            if (i + size < listSize) {
                parts.add(map.subMap(keys.get(i), keys.get(i + size)));
            } else {
                parts.add(map.tailMap(keys.get(i)));
            }
        }
        return parts;
    }

    @Override
    public void run() {
        if (isHostThread) {
            TimerTask pasteStatus = new TimerTask() {
                public void run() {
                    pasteStatus();
                    time++;
                }
            };
            timer = new Timer("printStatusTimer");
            timer.scheduleAtFixedRate(pasteStatus, 1000, 1000);
        } else {
            List<File> list = sortListByFilesSize(new ArrayList<>(checkList.keySet()));
            list.forEach(file -> {
                String s = checkList.get(file);
                checkFile = file;
                file = new File(Core.getInstance().getConfigManager().getCurrentConfig().inputFolder().getPath() + "\\" + file.getPath());
                if (!file.exists()) {
                    Core.getInstance().getNotFoundFiles().add(file.getPath());
                    return;
                }
                if (s.equalsIgnoreCase(getFileChecksum(file, Core.getInstance().getConfigManager().getCurrentConfig().algorithm()))) {
                    Core.getInstance().getCheckedFiles().add(file.getPath());
                } else {
                    Core.getInstance().getInvalidFiles().add(file.getPath());
                }
            });
        }
    }

    private List<File> sortListByFilesSize(List<File> files) {
        files.sort((o1, o2) -> {
            o1 = new File(Core.getInstance().getConfigManager().getCurrentConfig().inputFolder().getPath() + "\\" + o1.getPath());
            o2 = new File(Core.getInstance().getConfigManager().getCurrentConfig().inputFolder().getPath() + "\\" + o2.getPath());
            return Long.compare(o1.length(), o2.length());
        });
        return files;
    }


    private String getFileChecksum(File file, Algorithm algorithm) {
        MessageDigest messageDigest = null;
        CRC32 crc32 = new CRC32();
        Adler32 adler32 = new Adler32();
        try {
            if (algorithm != Algorithm.CRC32 && algorithm != Algorithm.ADLER32) {
                messageDigest = MessageDigest.getInstance(algorithm.name());
            }

            //Get file input stream for reading the file content
            FileInputStream fis = new FileInputStream(file);

            //Create byte array to read data in chunks
            byte[] byteArray = new byte[1024];
            int bytesCount = 0;

            //Read file data and update in message digest
            while ((bytesCount = fis.read(byteArray)) != -1) {
                if (algorithm == Algorithm.CRC32) {
                    crc32.update(byteArray, 0, bytesCount);
                } else if (algorithm == Algorithm.ADLER32) {
                    adler32.update(byteArray, 0, bytesCount);
                } else {
                    messageDigest.update(byteArray, 0, bytesCount);
                }
            }

            //close the stream; We don't need it now.
            fis.close();

            //Get the hash's bytes
            if (algorithm == Algorithm.CRC32) {
                return String.format(Locale.US, "%08X", crc32.getValue());
            } else if (algorithm == Algorithm.ADLER32) {
                return String.format(Locale.US, "%08X", adler32.getValue());
            } else {
                byte[] bytes = messageDigest.digest();
                //This bytes[] has bytes in decimal format;
                //Convert it to hexadecimal format
                StringBuilder sb = new StringBuilder();
                for (byte aByte : bytes) {
                    sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
                }
                return sb.toString();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error";
        }
    }

}
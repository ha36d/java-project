// filename: App.java

package com.schibsted.quiz;


import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;


public class App {
    /**
     * @param args
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        String inputFile, outputFile;
        inputFile = args[0];
        outputFile = args[1];
        //String inputFile = "file.txt", outputFile = "result.txt";
        int maxTempFiles = MaxTempFileNumber;
        Comparator<String> comparator = defaultComparator;

        BufferedInputStream fbr = new BufferedInputStream(new FileInputStream(inputFile));

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile)));

        try {
            boolean lineStatus = false;
            try {
                StringBuilder sb = new StringBuilder();
                StringBuilder sbr = new StringBuilder();
                String strData;
                final int CR = 13;
                final int LF = 10;
                final int S = 32;
                int currentCharVal;
                File newLineRead = File.createTempFile("Workingfile", "flatfile");
                BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(newLineRead)));
                while ((currentCharVal = fbr.read()) != -1) {
                    fbr.mark(1);
                    if (fbr.read() == -1) {
                        if (sb.length() > 0) {
                            sb.append((char) currentCharVal);
                            strData = sb.toString();
                            sb.setLength(0);
                            fbw.write(strData);
                            fbw.newLine();
                        }
                        fbw.close();
                        lineStatus = true;
                    } else
                        fbr.reset();

                    if (currentCharVal == S) {
                        strData = sb.toString();
                        sb.setLength(0);
                        fbw.write(strData);
                        fbw.newLine();

                    } else if (currentCharVal == LF) {
                        if (sb.length() > 0) {
                            strData = sb.toString();
                            sb.setLength(0);
                            fbw.write(strData);
                            fbw.newLine();
                        }
                        fbw.close();
                        lineStatus = true;

                    } else if (currentCharVal != CR) {
                        sb.append((char) currentCharVal);
                    }

                    if (lineStatus) {
                        List<File> l = sortInFile(newLineRead, comparator,
                                maxTempFiles);

                        File newLineWrite = File.createTempFile("Workingresult", "flatfile");
                        mergeSortedFiles(l, newLineWrite, comparator, false);

                        BufferedInputStream br = new BufferedInputStream(new FileInputStream(newLineWrite));

                        while ((currentCharVal = br.read()) != -1) {
                            br.mark(1);
                            if (br.read() == -1) {
                                if (sbr.length() > 0) {
                                    sbr.append((char) currentCharVal);
                                    strData = sbr.toString();
                                    sbr.setLength(0);
                                    bw.write(strData);
                                }
                            } else
                                br.reset();
                            if (currentCharVal == S) {
                                if (sbr.length() > 0) {
                                    strData = sbr.toString();
                                    sbr.setLength(0);
                                    bw.write(strData + " ");
                                }
                            } else if (currentCharVal == LF) {
                                if (sbr.length() > 0) {
                                    strData = sbr.toString();
                                    sbr.setLength(0);
                                    bw.write(strData + " ");
                                }
                            } else if (currentCharVal != CR) {
                                sbr.append((char) currentCharVal);
                            }
                        }
                        try {
                            bw.newLine();
                        } finally {
                            lineStatus = false;
                            newLineRead = File.createTempFile("Workingfile", "flatfile");
                            fbw = new BufferedWriter(new OutputStreamWriter(
                                    new FileOutputStream(newLineRead)));
                        }

                    }
                }


            } catch (EOFException oef) {
                System.err.println("Unable to close reader: " + oef.getMessage());
            }
        } finally {
            bw.close();
            fbr.close();
        }


    }

    /**
     * compute free memory
     * @return
     */
    public static long getFreeMemory() {
        System.gc();
        return Runtime.getRuntime().freeMemory();
    }

    /**
     * size of each block
     * @param fileSize
     * @param maxTempFiles
     * @param maxMemory
     * @return
     */
    public static long sizeOfBlocks(final long fileSize,
                                    final int maxTempFiles, final long maxMemory) {

        long blockSize = fileSize / maxTempFiles
                + (fileSize % maxTempFiles == 0 ? 0 : 1);
        if (blockSize < maxMemory / 2) {
            blockSize = maxMemory / 2;
        }
        return blockSize;
    }

    /**
     * merge and sort files by N-way merge by buffer
     * @param fbw
     * @param cmp
     * @param buffers
     * @return
     * @throws IOException
     */
    public static int mergeSortedFiles(BufferedWriter fbw,
                                       final Comparator<String> cmp,
                                       List<BinaryFileBuffer> buffers) throws IOException {
        PriorityQueue<BinaryFileBuffer> pq = new PriorityQueue<BinaryFileBuffer>(
                11, new Comparator<BinaryFileBuffer>() {
            @Override
            public int compare(BinaryFileBuffer i,
                               BinaryFileBuffer j) {
                return cmp.compare(i.peek(), j.peek());
            }
        });
        for (BinaryFileBuffer bfb : buffers)
            if (!bfb.empty())
                pq.add(bfb);
        int rowcounter = 0;
        try {
            while (pq.size() > 0) {
                BinaryFileBuffer bfb = pq.poll();
                String r = bfb.pop();
                fbw.write(r);
                fbw.newLine();
                ++rowcounter;
                if (bfb.empty()) {
                    bfb.fbr.close();
                } else {
                    pq.add(bfb);
                }
            }
        } finally {
            fbw.close();
            for (BinaryFileBuffer bfb : pq)
                bfb.close();
        }
        return rowcounter;

    }

    /**
     * merge and sort files by N-way merge by file
     * @param files
     * @param outputFile
     * @param cmp
     * @param append
     * @return
     * @throws IOException
     */
    public static int mergeSortedFiles(List<File> files, File outputFile,
                                       final Comparator<String> cmp,
                                       boolean append) throws IOException {
        ArrayList<BinaryFileBuffer> bfbs = new ArrayList<BinaryFileBuffer>();
        for (File f : files) {
            InputStream in = new FileInputStream(f);
            BufferedReader br;
            br = new BufferedReader(new InputStreamReader(
                    in));

            BinaryFileBuffer bfb = new BinaryFileBuffer(br);
            bfbs.add(bfb);
        }
        BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outputFile, append)));
        int rowcounter = mergeSortedFiles(fbw, cmp, bfbs);
        for (File f : files)
            f.delete();
        return rowcounter;
    }

    /**
     * Sort and Save in file
     * @param tmpList
     * @param cmp
     * @return
     * @throws IOException
     */
    public static File sortAndSave(List<String> tmpList,
                                   Comparator<String> cmp) throws IOException {
        Collections.sort(tmpList, cmp);
        File newTempFile = File.createTempFile("sortInFile",
                "flatfile");
        newTempFile.deleteOnExit();
        OutputStream out = new FileOutputStream(newTempFile);
        BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(
                out));
        try {
            for (String r : tmpList) {
                fbw.write(r);
                fbw.newLine();
            }
        } finally {
            fbw.close();
        }
        return newTempFile;
    }

    /**
     * Sort in Files by Buffer
     * @param fbr
     * @param dataLength
     * @param cmp
     * @param maxTempFiles
     * @param maxMemory
     * @return
     * @throws IOException
     */
    public static List<File> sortInFile(final BufferedReader fbr,
                                        final long dataLength, final Comparator<String> cmp,
                                        final int maxTempFiles, long maxMemory) throws IOException {
        List<File> files = new ArrayList<File>();
        long blockSize = sizeOfBlocks(dataLength,
                maxTempFiles, maxMemory);

        try {
            List<String> tmpList = new ArrayList<String>();
            String line = "";
            try {
                while (line != null) {
                    long currentBlockSize = 0;
                    while ((currentBlockSize < blockSize)
                            && ((line = fbr.readLine()) != null)) {
                        tmpList.add(line);
                        currentBlockSize += StringSize
                                .sizeOf(line);
                    }
                    files.add(sortAndSave(tmpList, cmp));
                    tmpList.clear();
                }
            } catch (EOFException oef) {
                if (tmpList.size() > 0) {
                    files.add(sortAndSave(tmpList, cmp));
                    tmpList.clear();
                }
            }
        } finally {
            fbr.close();
        }
        return files;
    }

    /**
     * Sort in Files by files
     * @param file
     * @param cmp
     * @param maxTempFiles
     * @return
     * @throws IOException
     */
    public static List<File> sortInFile(File file, Comparator<String> cmp,
                                        int maxTempFiles)
            throws IOException {
        BufferedReader fbr = new BufferedReader(new InputStreamReader(
                new FileInputStream(file)));
        return sortInFile(fbr, file.length(), cmp, maxTempFiles,
                getFreeMemory());
    }

    /**
     * Comparator Engine (here is for string comparing)
     */
    public static Comparator<String> defaultComparator = new Comparator<String>() {
        @Override
        public int compare(String r1, String r2) {
            return r1.toLowerCase().compareTo(r2.toLowerCase());
        }
    };

    // Maximum of temporary files that can be generated. Less uses Memory more.
    public static final int MaxTempFileNumber = 1024;

}

// Binary File Buffer
final class BinaryFileBuffer {
    public BinaryFileBuffer(BufferedReader r) throws IOException {
        this.fbr = r;
        reload();
    }

    public void close() throws IOException {
        this.fbr.close();
    }

    public boolean empty() {
        return this.cache == null;
    }

    public String peek() {
        return this.cache;
    }

    public String pop() throws IOException {
        String answer = peek().toString();
        reload();
        return answer;
    }

    private void reload() throws IOException {
        this.cache = this.fbr.readLine();
    }

    public BufferedReader fbr;

    private String cache;

}

// String Size
final class StringSize {

    private static int OBJ_OVERHEAD;

    private StringSize() {
    }

    static {
        boolean IS_64_BIT_JVM = true;
        String arch = System.getProperty("sun.arch.data.model");
        if (arch != null) {
            if (arch.contains("32")) {
                IS_64_BIT_JVM = false;
            }
        }
        int OBJ_HEADER = IS_64_BIT_JVM ? 16 : 8;
        int ARR_HEADER = IS_64_BIT_JVM ? 24 : 12;
        int OBJ_REF = IS_64_BIT_JVM ? 8 : 4;
        int INT_FIELDS = 12;
        OBJ_OVERHEAD = OBJ_HEADER + INT_FIELDS + OBJ_REF + ARR_HEADER;

    }

    public static long sizeOf(String s) {
        return (s.length() * 2) + OBJ_OVERHEAD;
    }

}

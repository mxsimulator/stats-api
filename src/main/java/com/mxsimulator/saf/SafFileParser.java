package com.mxsimulator.saf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SafFileParser {
    private static final String FILE_SEPARATOR = System.getProperty("os.name").contains("Windows") ? "\\" : "/";

    public static void extract(String sourcePath, String destinationPath) throws IOException {
        extract(Files.readAllBytes(Path.of(sourcePath)), destinationPath);
    }

    public static void extract(byte[] rawBytes, String destinationPath) throws IOException {
        int index = 0;
        StringBuilder headerContent = new StringBuilder();
        // Build headers for file size/paths
        while ((char) rawBytes[index] != "-".charAt(0)) {
            headerContent.append((char) rawBytes[index]);
            index += 1;
        }
        // now building content of files
        // skip - and \n
        index += 2;
        List<SafFile> safFileList = new ArrayList<>();
        String[] headerLines = headerContent.toString().split("\n");
        for (String line : headerLines) {
            // Determine filesize/path
            String[] pieces = line.replace("\n", "").split(" ");
            int byteCount = Integer.parseInt(pieces[0]);
            String path = String.join("", Arrays.asList(Arrays.copyOfRange(pieces, 1, pieces.length)));
            // Read bytes based on filesize
            byte[] bytes = Arrays.copyOfRange(rawBytes, index, index + byteCount);
            SafFile safFile = SafFile.builder()
                    .byteCount(byteCount)
                    .bytes(bytes)
                    .path(path)
                    .build();
            safFileList.add(safFile);
            index += byteCount;
        }

        for (SafFile safFile : safFileList) {
            Files.write(Path.of(destinationPath + "/" + safFile.getPath()), safFile.getBytes());
        }
    }

    public static void create(String sourcePath, String destinationFile) throws IOException {
        List<SafFile> safFileList = new ArrayList<>();
        List<String> files = Files.walk(Path.of(sourcePath))
                .filter(path -> !path.equals(sourcePath))
                .filter(path -> !path.toFile().isDirectory())
                .map(path -> path.toString().replace(sourcePath + FILE_SEPARATOR, ""))
                .collect(Collectors.toList());

        for (String filepath : files) {
            File file = new File(filepath);
            String normalizedPath = file.getPath()
                    .replace(FILE_SEPARATOR, "/")
                    .replace(sourcePath.replace(FILE_SEPARATOR, "/"), "")
                    .substring(1);
            byte[] bytes = Files.readAllBytes(Path.of(file.getAbsolutePath()));
            SafFile safFile = SafFile.builder()
                    .byteCount(bytes.length)
                    .bytes(bytes)
                    .path(normalizedPath)
                    .build();
            safFileList.add(safFile);
        }

        // Build saf headers
        StringBuilder stringBuilder = new StringBuilder();
        for (SafFile safFile : safFileList) {
            stringBuilder.append(safFile.getByteCount());
            stringBuilder.append(" ");
            stringBuilder.append(safFile.getPath());
            stringBuilder.append("\n");
        }
        stringBuilder.append("-\n");

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(stringBuilder.toString().getBytes());

            for (SafFile safFile : safFileList) {
                outputStream.write(safFile.getBytes());
            }

            Files.write(Path.of(destinationFile), outputStream.toByteArray());
        }
    }
}

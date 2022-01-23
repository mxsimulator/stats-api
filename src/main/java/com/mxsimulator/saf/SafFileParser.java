package com.mxsimulator.saf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.*;

public class SafFileParser {
    public static byte[] parse(byte[] rawBytes, boolean inflate) {
        Inflater inflater = new Inflater();
        inflater.setInput(rawBytes);
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(rawBytes.length)) {
            if (inflate) {
                byte[] buffer = new byte[1024];
                while (!inflater.finished()) {
                    int count = inflater.inflate(buffer);
                    outputStream.write(buffer, 0, count);
                }
            }
            byte[] fileBytes = inflate ? outputStream.toByteArray() : rawBytes;

            int index = 0;
            StringBuilder headerContent = new StringBuilder();
            // Build headers for file size/paths
            while ((char) fileBytes[index] != "-".charAt(0)) {
                headerContent.append((char) fileBytes[index]);
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
                byte[] bytes = Arrays.copyOfRange(fileBytes, index, index + byteCount);
                SafFile safFile = SafFile.builder()
                        .byteCount(byteCount)
                        .bytes(bytes)
                        .path(path)
                        .build();
                safFileList.add(safFile);
                index += byteCount;
            }

            try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                    for (SafFile safFile : safFileList) {
                        ZipEntry zipEntry = new ZipEntry(safFile.getPath());
                        zipOutputStream.putNextEntry(zipEntry);
                        zipOutputStream.write(safFile.getBytes());
                        zipOutputStream.closeEntry();
                    }
                }
                return byteArrayOutputStream.toByteArray();
            }
        } catch (IOException | DataFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] create(byte[] fileBytes, boolean deflate) {
        List<SafFile> safFileList = new ArrayList<>();
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);
             ZipInputStream zipInputStream = new ZipInputStream(byteArrayInputStream)) {
            ZipEntry zipEntry;

            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                ByteArrayOutputStream streamBuilder = new ByteArrayOutputStream();
                int bytesRead;
                byte[] tempBuffer = new byte[8192 * 2];

                while ((bytesRead = zipInputStream.read(tempBuffer)) != -1) {
                    streamBuilder.write(tempBuffer, 0, bytesRead);
                }

                SafFile safFile = SafFile.builder()
                        .byteCount((int) zipEntry.getSize())
                        .bytes(streamBuilder.toByteArray())
                        .path(zipEntry.getName())
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

                if (deflate) {
                    Deflater deflater = new Deflater();
                    deflater.setInput(outputStream.toByteArray());
                    try (ByteArrayOutputStream deflateOutputStream = new ByteArrayOutputStream(outputStream.toByteArray().length)) {
                        deflater.finish();
                        byte[] buffer = new byte[1024];
                        while (!deflater.finished()) {
                            int count = deflater.deflate(buffer);
                            deflateOutputStream.write(buffer, 0, count);
                        }
                        return deflateOutputStream.toByteArray();
                    }
                } else {
                    return outputStream.toByteArray();
                }
            }
        } catch (IOException e) {
            return null;
        }
    }
}

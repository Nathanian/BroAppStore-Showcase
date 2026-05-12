package com.example.appstoredemo.utils;

import java.io.*;

/**
 * File related helper utilities.
 */
public class FileUtils {
    /**
     * Copies a file from {@code source} to {@code dest} using a buffered stream.
     *
     * @param source file to read from.
     * @param dest file to write to.
     * @return {@code true} when the copy succeeds, otherwise {@code false}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    public static boolean copyFile(File source, File dest) {
        if (source == null || dest == null) {
            throw new IllegalArgumentException("Source and destination must not be null");
        }
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {

            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
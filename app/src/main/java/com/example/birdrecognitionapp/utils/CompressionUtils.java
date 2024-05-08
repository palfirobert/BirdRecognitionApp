package com.example.birdrecognitionapp.utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPOutputStream;

public class CompressionUtils {

    public static String compressString(String data) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length());
             GZIPOutputStream gzip = new GZIPOutputStream(bos)) {
            gzip.write(data.getBytes());
            gzip.close();
            byte[] compressed = bos.toByteArray();
            return Base64.getEncoder().encodeToString(compressed);
        } catch (IOException e) {
            throw new RuntimeException("Failed to compress data", e);
        }
    }

}
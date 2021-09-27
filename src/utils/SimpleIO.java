package utils;

import java.io.*;
import java.util.Scanner;
import java.util.StringJoiner;

public class SimpleIO {
    @FunctionalInterface
    public interface OutputHandler<U> {
        String handle(U o);
    }

    public static String input(String filename) throws IOException {
        final InputStream stream = new FileInputStream(filename);
        final Scanner scanner = new Scanner(stream);
        final StringJoiner stringJoiner = new StringJoiner("\n");
        while (scanner.hasNextLine()) {
            stringJoiner.add(scanner.nextLine());
        }
        scanner.close();
        stream.close();
        return stringJoiner.toString();
    }

    public static <T> void output(String filename, T obj, OutputHandler<T> handler) throws FileNotFoundException {
        final PrintWriter writer = new PrintWriter(filename);
        writer.println(handler.handle(obj));
        writer.close();
    }
}

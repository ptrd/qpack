package net.luminis.qpack;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Runs decoding tests by processing sample qif files.
// See https://github.com/qpackers/qifs
public class QifTestRunner {

    private List<Long> streamIds = new ArrayList<>();


    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Expected arguments: <qif file> <output file>");
            System.exit(1);
        }

        File encodedFile = new File(args[0]);
        if (!encodedFile.exists() || !encodedFile.canRead()) {
            System.err.println("Cannot find/read file '" + encodedFile.getAbsolutePath() + "'");
            System.exit(1);
        }

        File qifFile = new File(args[1]);
        if (!qifFile.getParentFile().exists()) {
            qifFile.getParentFile().mkdir();
        }
        qifFile.createNewFile();
        if (!qifFile.canWrite()) {
            System.err.println("Cannot write file '" + qifFile.getAbsolutePath() + "'");
            System.exit(1);
        }

        byte[] bytes = Files.readAllBytes(Path.of(args[0]));
        QifTestRunner runner = new QifTestRunner();
        runner.parseAndProcessQif(bytes, new PrintWriter(new FileWriter(qifFile)));

        System.out.println("Wrote '" + qifFile + "'");
    }

    private void parseAndProcessQif(byte[] bytes, PrintWriter out) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        DecoderImpl decoder = new DecoderImpl();

        while (buffer.remaining() > 0) {
            long streamId = buffer.getLong();
            int length = buffer.getInt();
            byte[] qpackData = new byte[length];
            buffer.get(qpackData);
            if (streamId == 0) {
                decoder.decodeEncoderStream(new ByteArrayInputStream(qpackData));
            }
            else {
                streamIds.add(streamId);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(qpackData);
                List<Map.Entry<String, String>> headers;
                try {
                    headers = decoder.decodeStream(inputStream);
                    headers.forEach(entry -> out.println(entry.getKey() + "\t" + entry.getValue()));
                } catch (IOException e) {
                    // Impossible
                }

                out.println();
                out.flush();
            }
        }
        out.close();

        System.out.println("Read " + streamIds.size() + " streams");
    }

}

package net.luminis.qpack;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

// Runs decoding tests by processing sample qif files.
// See https://github.com/qpackers/qifs
public class QifTestRunner {

    private Map<Long, byte[]> streamData = new HashMap<>();
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
        qifFile.createNewFile();
        if (!qifFile.canWrite()) {
            System.err.println("Cannot write file '" + qifFile.getAbsolutePath() + "'");
            System.exit(1);
        }

        byte[] bytes = Files.readAllBytes(Path.of(args[0]));
        QifTestRunner runner = new QifTestRunner();
        runner.parseQif(bytes);
        runner.parseStreams(new PrintWriter(new FileWriter(qifFile)));
        System.out.println("Wrote '" + qifFile + "'");
    }

    private void parseQif(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        while (buffer.remaining() > 0) {
            long streamId = buffer.getLong();
            streamIds.add(streamId);
            int length = buffer.getInt();
            byte[] qpackData = new byte[length];
            buffer.get(qpackData);
            streamData.put(streamId, qpackData);
        }
        System.out.println("Read " + streamIds.size() + " streams");
    }

    private void parseStreams(PrintWriter out) {
        Decoder decoder = new Decoder();

        streamIds.stream().forEach(id -> {
            ByteArrayInputStream inputStream = new ByteArrayInputStream(streamData.get(id));
            List<Map.Entry<String, String>> headers = null;
            try {
                headers = decoder.decodeStream(inputStream);
                headers.forEach(entry -> out.println(entry.getKey() + "\t" + entry.getValue()));
            } catch (IOException e) {
                // Impossible
            }
            out.println();
            out.flush();
        });

        out.close();
    }

}

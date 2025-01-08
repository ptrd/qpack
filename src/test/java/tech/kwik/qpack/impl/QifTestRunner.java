/*
 * Copyright © 2019, 2020, 2021, 2022, 2023, 2024, 2025 Peter Doornbosch
 *
 * This file is part of Flupke, a HTTP3 client Java library
 *
 * Flupke is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Flupke is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package tech.kwik.qpack.impl;

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

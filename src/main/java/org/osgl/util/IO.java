/* 
 * Copyright (C) 2013 The Java Tool project
 * Gelin Luo <greenlaw110(at)gmail.com>
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.osgl.util;

import org.osgl.$;
import org.osgl.exception.NotAppliedException;
import org.osgl.storage.ISObject;
import org.osgl.storage.impl.SObject;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.osgl.Lang.requireNotNull;

/**
 * IO utilities
 */
// Some code come from Play!Framework IO.java, under Apache License 2.0
public class IO {

    private static class BlackHole extends OutputStream {

        static final BlackHole INSTANCE = new BlackHole();

        @Override
        public void write(byte[] b) throws IOException {
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }

        @Override
        public void write(int b) throws IOException {
        }
    }

    /**
     * An output stream that anything written into it is lost
     */
    public static final OutputStream BLACK_HOLE = BlackHole.INSTANCE;

    /**
     * Alias of {@link #BLACK_HOLE}
     */
    public static final OutputStream NULL_OS = BLACK_HOLE;

    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            // ignore
        }
    }

    public static void flush(Flushable flushable) {
        if (null == flushable) {
            return;
        }
        try {
            flushable.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    public static void flush(ImageInputStream flushable) {
        if (null == flushable) {
            return;
        }
        try {
            flushable.flush();
        } catch (IOException e) {
            // ignore
        }
    }

    public static File child(File file, String fn) {
        return new File(file, fn);
    }

    public static List<File> children(File file) {
        return Arrays.asList(file.listFiles());
    }

    public static File parent(File file) {
        return file.getParentFile();
    }

    public static File tmpFile() {
        return tmpFile(S.random(3), null, null);
    }

    public static File tmpFile(String prefix, String suffix) {
        return tmpFile(prefix, suffix, null);
    }

    public static File tmpFile(String prefix, String suffix, File dir) {
        if (null == prefix) {
            prefix = S.random(3);
        }
        try {
            return File.createTempFile(prefix, suffix, dir);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    /**
     * This API is obsolete. Please use {@link #baos()} instead
     *
     * Returns a byte array output stream
     *
     * @return an output stream
     */
    @Deprecated
    public static OutputStream outputStream() {
        return baos();
    }

    /**
     * Returns a byte array output stream
     *
     * @return an output stream
     */
    public static ByteArrayOutputStream baos() {
        return new ByteArrayOutputStream();
    }

    /**
     * Alias of {@link #BLACK_HOLE}
     *
     * @return {@link #BLACK_HOLE}
     */
    public static OutputStream nullOutputStream() {
        return NULL_OS;
    }

    /**
     * Returns an output stream that anything write into it disappear (lost)
     *
     * @return a black hole output stream
     */
    public static OutputStream blackHole() {
        return BlackHole.INSTANCE;
    }

    /**
     * Returns a file output stream
     *
     * @param file the file to which the returned output stream can be used to write to
     * @return an output stream that can be used to write to file specified
     */
    public static OutputStream outputStream(File file) {
        try {
            return new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    public static ImageOutputStream imgOs(File file) {
        return Img.os(file);
    }

    public static ImageOutputStream imgOs(OutputStream os) {
        return Img.os(os);
    }

    public static BufferedImage readImg(InputStream is) {
        return Img.read(is);
    }

    public static BufferedImage readImg(URL url) {
        return Img.read(url);
    }

    public static BufferedImage readImg(File file) {
        return Img.read(file);
    }

    /**
     * Returns a string writer
     *
     * @return an writer that write to string
     */
    public static Writer writer() {
        return new StringWriter();
    }

    /**
     * Convert an output stream into Writer using default charset
     * @param output the output stream
     * @return a writer backed by the outputstream
     */
    public static Writer writer(OutputStream output) {
        return new OutputStreamWriter(output);
    }

    /**
     * Convert an output stream into Writer with charset specified
     * @param output the output stream
     * @param charset the charset
     * @return the writer
     */
    public static Writer writer(OutputStream output, Charset charset) {
        return new OutputStreamWriter(output, charset);
    }

    /**
     * Returns a file writer
     *
     * @param file the file to be written
     * @return a writer
     */
    public static Writer writer(File file) {
        try {
            return new FileWriter(file);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Returns an empty input stream
     *
     * @return an empty input stream
     */
    public static InputStream inputStream() {
        byte[] ba = {};
        return new ByteArrayInputStream(ba);
    }

    /**
     * Returns a file input stream
     *
     * @param file the file to be read
     * @return inputstream that read the file
     */
    public static InputStream inputStream(File file) {
        // workaround http://stackoverflow.com/questions/36880692/java-file-does-not-exists-but-file-getabsolutefile-exists
        if (!file.exists()) {
            file = file.getAbsoluteFile();
        }
        if (!file.exists()) {
            throw E.ioException("File does not exists: %s", file.getPath());
        }
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    public static InputStream inputStream(byte[] ba) {
        return new ByteArrayInputStream(ba);
    }

    /**
     * Returns an input stream from a string which will be encoded with
     * CharSet.defaultCharset()
     *
     * @param content the content to be read
     * @return input stream instance that read the content
     */
    public static InputStream inputStream(String content) {
        return inputStream(content.getBytes());
    }

    public static InputStream inputStream(URL url) {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Returns an empty reader
     *
     * @return a reader that reads empty string ""
     */
    public static Reader reader() {
        return new StringReader("");
    }

    /**
     * Returns a file reader
     *
     * @param file the file to be read
     * @return a reader that reads the file specified
     */
    public static FileReader reader(File file) {
        try {
            return new FileReader(file);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Create a reader from byte array with default charset
     * @param input the byte array
     * @return the reader
     */
    public static StringReader reader(byte[] input) {
        return new StringReader(new String(input));
    }

    /**
     * Create a reader from byte array with charset specified
     * @param input the byte array
     * @param charset the charset to encode byte array to String
     * @return the reader
     */
    public static StringReader reader(byte[] input, Charset charset) {
        return new StringReader(new String(input, charset));
    }

    /**
     * Convert an input stream into a reader using default charset
     * @param input the input stream
     * @return the Reader
     */
    public static Reader reader(InputStream input) {
        return new InputStreamReader(input);
    }

    /**
     * Convert an input stream into a reader with charset specified
     * @param input the input stream
     * @param charset the charset
     * @return the reader
     */
    public static Reader reader(InputStream input, Charset charset) {
        return new InputStreamReader(input, charset);
    }

    /**
     * Get a reader from a URL
     * @param url the URL
     * @return the reader reads the URL input stream
     */
    public static Reader reader(URL url) {
        try {
            return reader(url.openStream());
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Returns a string reader from a content specified
     *
     * @param content the content to be read
     * @return a string reader instance
     */
    public static Reader reader(String content) {
        return new StringReader(content);
    }

    public static BufferedOutputStream buffered(OutputStream os) {
        if (os instanceof BufferedOutputStream) {
            return (BufferedOutputStream) os;
        } else {
            return new BufferedOutputStream(os);
        }
    }

    public static BufferedInputStream buffered(InputStream is) {
        if (is instanceof BufferedInputStream) {
            return (BufferedInputStream) is;
        } else {
            return new BufferedInputStream(is);
        }
    }

    public static BufferedWriter buffered(Writer w) {
        if (w instanceof BufferedWriter) {
            return (BufferedWriter) w;
        } else {
            return new BufferedWriter(w);
        }
    }

    public static BufferedReader buffered(Reader r) {
        if (r instanceof BufferedReader) {
            return (BufferedReader) r;
        } else {
            return new BufferedReader(r);
        }
    }

    public static void delete(File file) {
        delete(file, false);
    }

    public static void delete(File f, boolean deleteChildren) {
        if (null == f || !f.exists()) {
            return;
        }
        if (f.isDirectory()) {
            String[] sa = f.list();
            if (null != sa && sa.length > 0) {
                if (!deleteChildren) {
                    return;
                } else {
                    for (File f0 : f.listFiles()) {
                        delete(f0, true);
                    }
                }
            }
        }
        if (!f.delete()) {
            f.deleteOnExit();
        }
    }

    /**
     * Load properties from a file
     *
     * @param file the properties file
     * @return the properties loaded from the file specified or `null` if exception encountered
     */
    public static Properties loadProperties(File file) {
        return loadProperties(IO.inputStream(file));
    }

    /**
     * Load properties from an inputStream
     *
     * @param inputStream the input stream to property source
     * @return the properties loaded from the input stream specified or `null` if exception encountered
     */
    public static Properties loadProperties(InputStream inputStream) {
        Properties prop = new Properties();
        try {
            prop.load(inputStream);
        } catch (IOException e) {
            return null;
        } finally {
            IO.close(inputStream);
        }
        return prop;
    }

    /**
     * Load properties from an inputStream
     *
     * @param reader the reader to property source
     * @return the properties loaded from the reader specified or `null` if exception encountered
     */
    public static Properties loadProperties(Reader reader) {
        Properties prop = new Properties();
        try {
            prop.load(reader);
        } catch (IOException e) {
            return null;
        } finally {
            IO.close(reader);
        }
        return prop;
    }

    /**
     * Load properties from an URL
     *
     * @param url the URL to read the properties
     * @return the properties loaded or `null` if exception encountered
     */
    public static Properties loadProperties(URL url) {
        return loadProperties(inputStream(url));
    }

    /**
     * Load properties from a string content
     *
     * @param content the content of a properties file
     * @return the properties loaded or `null` if exception encountered
     */
    public static Properties loadProperties(String content) {
        return loadProperties(new StringReader(content));
    }

    /**
     * Read binary content of a file (warning does not use on large file !)
     *
     * @param file The file te read
     * @return The binary data
     */
    public static byte[] readContent(File file) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            copy(new BufferedInputStream(new FileInputStream(file)), baos);
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
        return baos.toByteArray();
    }

    public static byte[] readContent(InputStream is) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copy(is, baos);
        return baos.toByteArray();
    }

    /**
     * Read file content to a String (always use utf-8)
     *
     * @param file The file to read
     * @return The String content
     */
    public static String readContentAsString(File file) {
        return readContentAsString(file, "utf-8");
    }

    /**
     * Read file content to a String
     *
     * @param url      The url resource to read
     * @param encoding encoding used to read the file into string content
     * @return The String content
     */
    public static String readContentAsString(URL url, String encoding) {
        try {
            return readContentAsString(url.openStream(), encoding);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Read file content to a String (always use utf-8)
     *
     * @param url the url resource to read
     * @return The String content
     */
    public static String readContentAsString(URL url) {
        return readContentAsString(url, "utf-8");
    }

    /**
     * Read file content to a String
     *
     * @param file     The file to read
     * @param encoding encoding used to read the file into string content
     * @return The String content
     */
    public static String readContentAsString(File file, String encoding) {
        try {
            return readContentAsString(new FileInputStream(file), encoding);
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    public static String readContentAsString(InputStream is) {
        return readContentAsString(is, "utf-8");
    }

    public static String readContentAsString(InputStream is, String encoding) {
        try {
            StringWriter result = new StringWriter();
            PrintWriter out = new PrintWriter(result);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, encoding));
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                if (lineNo++ > 0) out.println();
                out.print(line);
            }
            return result.toString();
        } catch (IOException e) {
            throw E.ioException(e);
        } finally {
            close(is);
        }
    }

    public static List<String> readLines(File file) {
        return readLines(file, 0);
    }

    public static List<String> readLines(File file, int limit) {
        return readLines(file, "utf-8", limit);
    }

    public static List<String> readLines(File file, String encoding) {
        return readLines(file, encoding, 0);
    }

    public static List<String> readLines(File file, String encoding, int limit) {
        List<String> lines = null;
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            lines = readLines(is, encoding, limit);
        } catch (IOException ex) {
            throw E.ioException(ex);
        } finally {
            close(is);
        }
        return lines;
    }

    public static List<String> readLines(InputStream is, String encoding) {
        return readLines(is, encoding, 0);
    }

    public static List<String> readLines(InputStream is, String encoding, int limit) {
        if (encoding == null) {
            return readLines(is, limit);
        } else {
            InputStreamReader r;
            try {
                r = new InputStreamReader(is, encoding);
            } catch (UnsupportedEncodingException e) {
                throw E.encodingException(e);
            }
            return readLines(r, limit);
        }
    }

    public static List<String> readLines(InputStream inputStream) {
        return readLines(inputStream, 0);
    }

    public static List<String> readLines(InputStream inputStream, int limit) {
        InputStreamReader r = new InputStreamReader(inputStream);
        return readLines(r, limit);
    }

    public static List<String> readLines(Reader input) {
        return readLines(input, 0);
    }

    public static List<String> readLines(Reader input, int limit) {
        BufferedReader reader = new BufferedReader(input);
        List<String> list = new ArrayList<String>();
        if (limit < 1) {
            limit = Integer.MAX_VALUE;
        }
        try {
            int n = 0;
            String line = reader.readLine();
            while ((n++ < limit) && line != null) {
                list.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            throw E.ioException(e);
        }
        return list;
    }

    public static List<String> readLines(URL url) {
        return readLines(url, 0);
    }

    public static List<String> readLines(URL url, int limit) {
        try {
            return readLines(url.openStream(), limit);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static List<String> readLines(URL url, String encode) {
        return readLines(url, encode, 0);
    }

    public static List<String> readLines(URL url, String encode, int limit) {
        try {
            return readLines(url.openStream(), encode, limit);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Write String content to a file (always use utf-8)
     *
     * @param content The content to write
     * @param file    The file to write
     */
    public static void writeContent(CharSequence content, File file) {
        writeContent(content, file, "utf-8");
    }

    /**
     * Write String content to a file (always use utf-8)
     *
     * @param content The content to write
     * @param file    The file to write
     * @param charset charset name
     */
    public static void writeContent(CharSequence content, File file, String charset) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter(os, charset));
            printWriter.println(content);
            printWriter.flush();
            os.flush();
        } catch (IOException e) {
            throw E.ioException(e);
        } finally {
            close(os);
        }
    }

    /**
     * Write string content to an output stream.
     *
     * This method relies on {@link #writeContent(CharSequence, Writer)}
     *
     * @param content the content
     * @param os      the outputstream
     */
    public static int writeContent(CharSequence content, OutputStream os) {
        return writeContent(content, new PrintWriter(os));
    }

    /**
     * Write string content to an output stream with encoding specified
     *
     * @param content the content
     * @param os      the output stream
     * @param charset charset name
     */
    public static void writeContent(CharSequence content, OutputStream os, String charset) {
        try {
            writeContent(content, new PrintWriter(new OutputStreamWriter(os, charset)));
        } catch (UnsupportedEncodingException e) {
            throw E.ioException(e);
        }
    }


    /**
     * Write string content to a writer
     *
     * Note this will print the content plus a `line.separator` e.g. `\n`
     *
     * @param content the content to write
     * @param writer  the writer
     */
    public static int writeContent(CharSequence content, Writer writer) {
        try {
            PrintWriter printWriter = new PrintWriter(writer);
            printWriter.println(content);
            printWriter.flush();
            writer.flush();
            return content.length();
        } catch (IOException e) {
            throw E.ioException(e);
        } finally {
            close(writer);
        }
    }

    /**
     * Copy content from input stream to output stream without closing the output stream
     *
     * @param is input stream
     * @param os output stream
     * @return number of bytes appended
     */
    public static int append(InputStream is, OutputStream os) {
        return copy(is, os, false);
    }

    public static int copy(InputStream is, OutputStream os) {
        return copy(is, os, true);
    }

    /**
     * Copy an stream to another one. It close the input stream anyway.
     *
     * If the param closeOs is true then close the output stream
     *
     * @param input      input stream
     * @param output      output stream
     * @param closeOutput specify whether it shall close output stream after operation
     * @return number of bytes copied
     */
    public static int copy(InputStream input, OutputStream output, boolean closeOutput) {
        try {
            int read, total = 0;
            byte[] buffer = new byte[8096];
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
                total += read;
            }
            return total;
        } catch (IOException e) {
            throw E.wrap(e);
        } finally {
            close(input);
            if (closeOutput) {
                close(output);
            }
        }
    }

    /**
     * Copy chars from input reader into output writer. Close input and output at the end of the
     * operation
     * @param input the input reader
     * @param output the output writer
     * @return the number of chars been copied
     */
    public static int copy(Reader input, Writer output) {
        return copy(input, output, true);
    }

    /**
     * Copy chars from input reader into output writer. It will close reader anyway
     *
     * @param input the input reader
     * @param output the output writer
     * @param closeOutput specify if it shall close output writer in the end
     * @return the number of chars been copied
     */
    public static int copy(Reader input, Writer output, boolean closeOutput) {
        char[] buffer = new char[1024];
        int count = 0;
        int n;
        try {
            while (-1 != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
                count += n;
            }
        } catch (IOException e) {
            throw E.wrap(e);
        } finally {
            close(input);
            if (closeOutput) {
                close(output);
            }
        }
        return count;
    }

    /**
     * An API stage supports fluent coding with IO writing.
     *
     * A general way to use this stage:
     *
     * ```java
     * IO.write(byteArray).to(outputStream);
     * IO.write(textString).to(writer);
     * IO.write(inputSteam).to(file);
     * IO.write(reader).withCharset("utf-8").to(outputstream);
     * ...
     * ```
     */
    public static class _WriteStage {
        private InputStream is;
        private byte[] ba;
        private CharSequence text;
        private File file;
        private Reader reader;
        private Charset charset = Charset.defaultCharset();

        private _WriteStage(InputStream is) {
            this.is = requireNotNull(is);
        }

        private _WriteStage(Reader reader) {
            this.reader = requireNotNull(reader);
        }

        private _WriteStage(byte[] ba) {
            this.ba = requireNotNull(ba);
        }

        private _WriteStage(CharSequence text) {
            this.text = requireNotNull(text);
        }

        private _WriteStage(File file) {
            this.file = requireNotNull(file);
        }

        /**
         * set charset of this write stage
         * @param charset the charset must not be null
         * @return this stage instance
         */
        public _WriteStage with(Charset charset) {
            this.charset = requireNotNull(charset);
            return this;
        }

        /**
         * Set charset of this write stage
         * @param charset the charset string, must not be null
         * @return this stage instance
         */
        public _WriteStage withCharset(String charset) {
            this.charset = Charset.forName(charset);
            return this;
        }

        /**
         * Write this stage source to output stream
         * @param target the target output stream
         * @return the number of bytes been written
         */
        public long to(OutputStream target) {
            if (null != is) {
                return write(is, target);
            } else if (null != reader) {
                return write(reader, writer(target, charset));
            } else if (null != ba) {
                return write(ba, target);
            } else if (null != text) {
                return writeContent(text, target);
            } else {
                E.unsupportedIf(file.isDirectory(), "cannot write directory into an output stream");
                return write(inputStream(file), target);
            }
        }

        /**
         * Write this stage source to a writer target
         * @param target the writer target
         * @return number of chars been written
         */
        public long to(Writer target) {
            if (null != is) {
                return write(reader(is, charset), target);
            } else if (null != reader) {
                return write(reader, target);
            } else if (null != ba) {
                return write(reader(ba), target);
            } else if (null != text) {
                return writeContent(text, target);
            } else {
                E.unsupportedIf(file.isDirectory(), "cannot write directory into an output stream");
                return write(reader(file), target);
            }
        }

        /**
         * Write this stage source to a file target
         * @param file the file target
         * @return number of bytes been written
         * @see #copy(File, File)
         */
        public long to(File file) {
            if (null != this.file) {
                return copy(this.file, file);
            }
            return to(outputStream(file));
        }
    }

    /**
     * Create a {@link _WriteStage write stage} using input stream
     * @param source the input stream
     * @return a write stage
     */
    public static _WriteStage write(InputStream source) {
        return new _WriteStage(source);
    }

    /**
     * Alias of {@link #write(InputStream)}
     *
     * @param source the input stream
     * @return a write stage
     */
    public static _WriteStage copy(InputStream source) {
        return write(source);
    }

    /**
     * Create a {@link _WriteStage write stage} using byte array
     * @param source the byte array
     * @return a write stage
     */
    public static _WriteStage write(byte[] source) {
        return new _WriteStage(source);
    }

    /**
     * Create a {@link _WriteStage write stage} with text content
     * @param source the text source
     * @return a write stage
     */
    public static _WriteStage write(CharSequence source) {
        return new _WriteStage(source);
    }

    /**
     * Create a {@link _WriteStage write stage} using file
     * @param source the file source
     * @return a write stage
     */
    public static _WriteStage write(File source) {
        return new _WriteStage(source);
    }

    /**
     * Alias of {@link #write(File)}
     *
     * @param source the file source
     * @return a write stage
     */
    public static _WriteStage copy(File source) {
        return write(source);
    }

    /**
     * Alias of {@link #copy(java.io.InputStream, java.io.OutputStream)}
     *
     * @param is input stream
     * @param os output stream
     */
    public static long write(InputStream is, OutputStream os) {
        return copy(is, os);
    }

    public static long write(Reader reader, Writer writer) {
        return copy(reader, writer);
    }

    public static long write(InputStream is, File f) {
        try {
            return copy(is, new BufferedOutputStream(new FileOutputStream(f)));
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Write binary data to a file
     *
     * @param data The binary data to write
     * @param file The file to write
     */
    public static long write(byte[] data, File file) {
        try {
            return write(new ByteArrayInputStream(data), new BufferedOutputStream(new FileOutputStream(file)));
        } catch (FileNotFoundException e) {
            throw E.ioException(e);
        }
    }

    /**
     * Write binary data to an output steam
     *
     * @param data the binary data to write
     * @param os   the output stream
     */
    public static long write(byte[] data, OutputStream os) {
        return write(new ByteArrayInputStream(data), os);
    }

    /**
     * Copy source file to target file.
     *
     * If source file is a directory, then
     * * If target file is not a directory raise {@link org.osgl.exception.UnsupportedException}
     * * If cannot create target dir, it will raise {@link org.osgl.exception.UnexpectedIOException}
     * * Recursively copy all files and dirs into target dir
     *
     * If source file is a file, then
     * * if target file is a directory then write into the new file with the same name of source file in the directory
     * * otherwise write into the the file directly
     *
     * @param source the source file
     * @param target the target file
     * @return total bytes been copied
     * @throws org.osgl.exception.UnexpectedIOException if any IO exception encountered
     */
    public static long copy(File source, File target) {
        if (S.eq(source.getAbsolutePath(), target.getAbsolutePath())) {
            return 0;
        }
        if (source.isDirectory()) {
            E.unsupportedIfNot(target.isDirectory(), "Cannot copy a directory into a file");
            E.ioExceptionIfNot((target.exists() || target.mkdirs()), "Cannot create target dir");
            File[] files = source.listFiles();
            if (null == files) {
                return 0;
            }
            long copied = 0;
            for (File file : files) {
                copied += copy(file, new File(target, file.getName()));
            }
            return copied;
        } else {
            if (target.isDirectory()) {
                E.ioExceptionIfNot(target.exists() || target.mkdirs(), "Cannot create target dir");
                target = new File(target, source.getName());
            }
            try {
                return write(new FileInputStream(source), new FileOutputStream(target));
            } catch (IOException e) {
                throw E.ioException(e);
            }
        }
    }

    public static ISObject zip(ISObject... objects) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        try {
            for (ISObject obj : objects) {
                ZipEntry entry = new ZipEntry(obj.getAttribute(SObject.ATTR_FILE_NAME));
                InputStream is = obj.asInputStream();
                zos.putNextEntry(entry);
                copy(is, zos, false);
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw E.ioException(e);
        } finally {
            close(zos);
        }
        return SObject.of(Codec.encodeUrl(S.random()), baos.toByteArray());
    }

    public static File zip(File... files) {
        try {
            File temp = File.createTempFile("osgl", ".zip");
            zipInto(temp, files);
            return temp;
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static void zipInto(File target, File... files) {
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(target)));
            byte[] buffer = new byte[128];
            for (File f : files) {
                ZipEntry entry = new ZipEntry(f.getName());
                InputStream is = new BufferedInputStream(new FileInputStream(f));
                zos.putNextEntry(entry);
                int read = 0;
                while ((read = is.read(buffer)) != -1) {
                    zos.write(buffer, 0, read);
                }
                zos.closeEntry();
                IO.close(is);
            }
        } catch (IOException e) {
            throw E.ioException(e);
        } finally {
            IO.close(zos);
        }
    }

    public static final class F {
        public static <T> $.Function<?, T> println() {
            return PRINTLN;
        }

        public static $.Function PRINTLN = print("", "\n", System.out);

        public static <T> $.Function<?, T> print() {
            return PRINT;
        }

        public static $.Function PRINT = print("", "", System.out);

        public static <T> $.Function<T, ?> print(String prefix, String suffix) {
            return print(prefix, suffix, System.out);
        }

        public static <T> $.Function<T, ?> print(String prefix, String suffix, PrintStream ps) {
            return new $.F4<T, String, String, PrintStream, Void>() {
                @Override
                public Void apply(T t, String prefix, String suffix, PrintStream ps) {
                    StringBuilder sb = new StringBuilder(prefix).append(t).append(suffix);
                    ps.print(sb);
                    return null;
                }
            }.curry(prefix, suffix, ps);
        }

        public static final $.Function<File, InputStream> FILE_TO_IS = new $.F1<File, InputStream>() {
            @Override
            public InputStream apply(File file) throws NotAppliedException, $.Break {
                try {
                    return new BufferedInputStream(new FileInputStream(file));
                } catch (IOException e) {
                    throw E.ioException(e);
                }
            }
        };
    }
}

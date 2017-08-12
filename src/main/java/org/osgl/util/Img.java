package org.osgl.util;

import org.osgl.$;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import static org.osgl.util.N.assertNonNegative;
import static org.osgl.util.N.assert_;

/**
 * Image utilities
 *
 * Disclaim: some of the logic in this class comes from
 * https://github.com/playframework/play1/blob/master/framework/src/play/libs/Images.java
 */
public enum Img {
    ;

    public static final String DEF_MIME_TYPE = "image/jpeg";

    /**
     * Base class for image operator function which provides source width, height, ratio parameters
     * on demand
     */
    public abstract static class Processor extends $.Producer<BufferedImage> {
        protected BufferedImage source;
        protected int sourceWidth;
        protected int sourceHeight;
        protected double sourceRatio;

        protected Processor() {
        }

        @Override
        public BufferedImage produce() {
            return run();
        }

        protected abstract BufferedImage run();

        public Processor source(BufferedImage source) {
            this.source = $.assertNotNull(source);
            this.sourceWidth = source.getWidth();
            this.sourceHeight = source.getHeight();
            this.sourceRatio = (double) this.sourceWidth / this.sourceHeight;
            return this;
        }
    }

    public static class _Operator {
        BufferedImage source;
        volatile BufferedImage target;
        Processor worker;

        private _Operator(BufferedImage source) {
            this.source = $.assertNotNull(source);
        }

        private _Operator(InputStream source) {
            this.source = read(source);
        }

        public _Operator transform(Processor operator) {
            this.worker = $.assertNotNull(operator);
            return this;
        }

        public void writeTo(String fileName) {
            writeTo(new File(fileName));
        }

        public void writeTo(File file, String mimeType) {
            writeTo(IO.os(file), mimeType);
        }

        public void writeTo(File file) {
            writeTo(IO.os(file), mimeType(file));
        }

        public void writeTo(OutputStream os, String mimeType) {
            BufferedImage target = target();
            ImageWriter writer = ImageIO.getImageWritersByMIMEType(mimeType(mimeType)).next();
            ImageWriteParam params = writer.getDefaultWriteParam();

            ImageOutputStream ios = os(os);
            writer.setOutput(ios);
            IIOImage image = new IIOImage(target, null, null);
            try {
                writer.write(null, image, params);
            } catch (IOException e) {
                throw E.ioException(e);
            }
            IO.flush(ios);
            writer.dispose();
        }

        private BufferedImage target() {
            if (null == target) {
                doJob();
            }
            return target;
        }

        private synchronized void doJob() {
            preTransform();
            target = worker.source(source).produce();
        }

        protected void preTransform() {
        }
    }

    public static class _Resize extends _Operator {
        private int w;
        private int h;
        private boolean keepRatio;

        private _Resize(int w, int h, InputStream is) {
            super(is);
            this.w = assertNonNegative(w);
            this.h = assertNonNegative(h);
        }

        public _Resize keepRatio() {
            this.keepRatio = true;
            return this;
        }

        @Override
        protected void preTransform() {
            transform(new Resizer(w, h, keepRatio));
        }
    }

    public static class _Crop extends _Operator {
        private int x1;
        private int y1;
        private int x2;
        private int y2;

        private _Crop(InputStream is) {
            super(is);
        }

        private _Crop(int x1, int y1, int x2, int y2, InputStream is) {
            super(is);
            this.x1 = assertNonNegative(x1);
            this.y1 = assertNonNegative(y1);
            this.x2 = assertNonNegative(x2);
            this.y2 = assertNonNegative(y2);
        }

        public _Crop from(int x, int y) {
            x1 = assertNonNegative(x);
            y1 = assertNonNegative(y);
            return this;
        }

        public _Crop to(int x, int y) {
            x2 = assert_(x).gte(x1);
            y2 = assert_(y).gte(y1);
            return this;
        }

        public _Crop from($.Tuple<Integer, Integer> leftTop) {
            return from(leftTop._1, leftTop._2);
        }

        @Override
        protected void preTransform() {
            transform(new Cropper(x1, y1, x2, y2));
        }
    }

    public static class _Watermarker extends _Operator {

        Color color = Color.LIGHT_GRAY;
        Font font = new Font("Arial", Font.BOLD, 28);
        float alpha = 0.8f;
        String text;
        int offsetX;
        int offsetY;

        private _Watermarker(String text, InputStream inputStream) {
            super(inputStream);
            this.text = S.assertNotBlank(text);
        }

        public _Watermarker color(Color color) {
            this.color = $.assertNotNull(color);
            return this;
        }

        public _Watermarker font(Font font) {
            this.font = $.assertNotNull(font);
            return this;
        }

        public _Watermarker alpha(float alpha) {
            this.alpha = N.assertAlpha(alpha);
            return this;
        }

        public _Watermarker offset(int offsetX, int offsetY) {
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            return this;
        }

        public _Watermarker offsetY(int offsetY) {
            this.offsetY = offsetY;
            return this;
        }

        public _Watermarker offsetX(int offsetX) {
            this.offsetX = offsetX;
            return this;
        }

        @Override
        protected void preTransform() {
            transform(new WaterMarker(text, offsetX, offsetY, color, font, alpha));
        }
    }

    public static class _Load {
        private InputStream is;

        private _Load(InputStream is) {
            this.is = $.assertNotNull(is);
        }

        public _Resize resize(int w, int h) {
            return new _Resize(w, h, is());
        }

        public _Resize resize($.Tuple<Integer, Integer> dimension) {
            return new _Resize(dimension.left(), dimension.right(), is());
        }

        public _Resize resize(Dimension dimension) {
            return new _Resize(dimension.width, dimension.height, is());
        }

        public _Crop crop(int x1, int y1, int x2, int y2) {
            return new _Crop(x1, y1, x2, y2, is());
        }

        public _Crop crop($.Tuple<Integer, Integer> leftTop, $.Tuple<Integer, Integer> rightBottom) {
            return crop(leftTop._1, leftTop._2, rightBottom._1, rightBottom._2);
        }

        public _Watermarker watermark(String text) {
            return new _Watermarker(text, is());
        }

        public _Operator transform(Processor processor) {
            return new _Operator(is).transform(processor);
        }

        private InputStream is() {
            E.illegalStateIf(null == this.is, "already consumed");
            InputStream is = this.is;
            this.is = null;
            return is;
        }
    }

    public static _Load source(InputStream is) {
        return new _Load(is);
    }

    public static _Load source(File file) {
        return new _Load(IO.is(file));
    }

    /**
     * Encode an image to base64 using a data: URI
     *
     * @param image The image file
     * @return The base64 encoded value
     */
    public static String toBase64(File image) throws IOException {
        return toBase64(IO.is(image), mimeType(image));
    }

    /**
     * Encode an image to base64 using a data: URI
     *
     * @param inputStream The image input stream
     * @param mimeType    the mime type, if not specified then default to {@link #DEF_MIME_TYPE}
     * @return The base64 encoded value
     */
    public static String toBase64(InputStream inputStream, String mimeType) {
        return "data:" + mimeType(mimeType) + ";base64," + Codec.encodeBase64(IO.readContent(inputStream));
    }

    private static String mimeType(File target) {
        return mimeType(target.getName());
    }

    private static String mimeType(String hint) {
        String mimeType = DEF_MIME_TYPE;
        if (S.blank(hint)) {
            return mimeType;
        }
        if (1 == S.count(hint, "/", false) && !hint.contains(".")) {
            // this is a mime type string
            return hint;
        }
        if (hint.endsWith("png")) {
            mimeType = "image/png";
        }
        if (hint.endsWith("gif")) {
            mimeType = "image/gif";
        }
        return mimeType;
    }

    public static ImageOutputStream os(File file) {
        try {
            return new FileImageOutputStream(file);
        } catch (IOException e) {
            throw E.ioException(e);
        }
    }

    public static ImageOutputStream os(OutputStream os) {
        return new MemoryCacheImageOutputStream(os);
    }

    public static BufferedImage read(InputStream is) {
        try {
            return ImageIO.read(is);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public static BufferedImage read(File file) {
        try {
            return ImageIO.read(file);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    public static BufferedImage read(URL url) {
        try {
            return ImageIO.read(url);
        } catch (Exception e) {
            throw E.unexpected(e);
        }
    }

    // -- Image operators

    /**
     * Resize an image
     */
    private static class Resizer extends Processor {
        int w;
        int h;
        boolean keepRatio;

        public Resizer(int w, int h, boolean keepRatio) {
            this.w = N.assertNonNegative(w);
            this.h = N.assertNonNegative(h);
            this.keepRatio = keepRatio;
        }

        @Override
        protected BufferedImage run() {
            int maxWidth = w;
            int maxHeight = h;

            if (w < 0 && h < 0) {
                w = sourceWidth;
                h = sourceHeight;
            }

            double ratio = sourceRatio;

            if (w < 0 && h > 0) {
                w = (int) (h * ratio);
            }
            if (w > 0 && h < 0) {
                h = (int) (w / ratio);
            }

            if (keepRatio) {
                h = (int) (w / ratio);
                if (h > maxHeight) {
                    h = maxHeight;
                    w = (int) (h * ratio);
                }
                if (w > maxWidth) {
                    w = maxWidth;
                    h = (int) (w / ratio);
                }
            }

            // out
            BufferedImage target;
            Graphics g;
            if (source.getColorModel().hasAlpha()) {
                target = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                g = target.getGraphics();
            } else {
                target = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                // Create a white background if not transparency define
                g = target.getGraphics();
                g.setColor(Color.BLUE);
                g.fillRect(0, 0, w, h);
            }
            Image srcSized = source.getScaledInstance(w, h, Image.SCALE_SMOOTH);

            g.drawImage(srcSized, 0, 0, null);
            g.dispose();
            return target;
        }
    }

    private static class Cropper extends Processor {

        private int x1;
        private int y1;
        private int x2;
        private int y2;

        Cropper(int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
        }

        @Override
        protected BufferedImage run() {
            int width = x2 - x1;
            int height = y2 - y1;

            // out
            BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Image croppedImage = source.getSubimage(x1, y1, width, height);
            Graphics g = target.getGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(croppedImage, 0, 0, null);
            g.dispose();
            return target;
        }
    }

    private static class WaterMarker extends Processor {

        Color color;
        Font font;
        float alpha;
        String text;
        int offsetX;
        int offsetY;

        WaterMarker(String text, int offsetX, int offsetY, Color color, Font font, float alpha) {
            this.text = text;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.color = color;
            this.font = font;
            this.alpha = alpha;
        }

        @Override
        protected BufferedImage run() {
            int w = sourceWidth;
            int h = sourceHeight;
            BufferedImage target = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            g.drawImage(source, 0, 0, w, h, null);
            g.setColor(color);
            g.setFont(font);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));

            FontMetrics fontMetrics = g.getFontMetrics();
            Rectangle2D rect = fontMetrics.getStringBounds(text, g);
            int centerX = (w - (int) rect.getWidth() + offsetX) / 2;
            int centerY = (h - (int) rect.getHeight() + offsetY) / 2;
            g.drawString(text, centerX, centerY);
            g.dispose();
            return target;
        }

    }

}

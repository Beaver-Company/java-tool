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
import java.io.*;
import java.net.URL;

import static org.osgl.Lang.assertNotNull;
import static org.osgl.util.N.assertNonNegative;
import static org.osgl.util.N.assertNotNaN;
import static org.osgl.util.N.assertPositive;

/**
 * Image utilities
 *
 * Disclaim: some of the logic in this class comes from
 * https://github.com/playframework/play1/blob/master/framework/src/play/libs/Images.java
 */
public enum Img {
    ;

    /**
     * The default image MIME type when not specified
     *
     * The value is `image/png`
     */
    public static final String DEF_MIME_TYPE = "image/png";

    public static final String GIF_MIME_TYPE = "image/gif";

    public static final String PNG_MIME_TYPE = "image/png";

    public static final String JPG_MIME_TYPE = "image/jpeg";

    /**
     * Byte array of a tracking pixel image in gif format
     */
    public static final byte[] TRACKING_PIXEL_BYTES = new _Processor(F.TRACKING_PIXEL).toByteArray(GIF_MIME_TYPE);

    /**
     * Base64 string of a tracking pixel image in gif format
     */
    public static final String TRACKING_PIXEL_BASE64 = toBase64(TRACKING_PIXEL_BYTES, GIF_MIME_TYPE);

    /**
     * Base class for image operator function which provides source width, height, ratio parameters
     * on demand
     */
    public abstract static class Processor extends $.Producer<BufferedImage> {
        /**
         * The source image
         */
        protected BufferedImage source;
        /**
         * The source image width
         */
        protected int sourceWidth;
        /**
         * The source image height
         */
        protected int sourceHeight;
        /**
         * The source image width/height ratio
         */
        protected double sourceRatio;

        protected Processor() {
        }

        @Override
        public BufferedImage produce() {
            return run();
        }

        /**
         * Sub class shall implement the image process logic in
         * this method
         *
         * @return the processed image from {@link #source source image}
         */
        protected abstract BufferedImage run();

        /**
         * Set source image. This method will calculate and cache the following
         * parameters about the source image:
         *
         * * {@link #sourceWidth}
         * * {@link #sourceHeight}
         * * {@link #sourceRatio}
         *
         * @param source the source image
         * @return this Processor instance
         */
        public Processor source(BufferedImage source) {
            this.source = assertNotNull(source);
            this.sourceWidth = source.getWidth();
            this.sourceHeight = source.getHeight();
            this.sourceRatio = (double) this.sourceWidth / this.sourceHeight;
            return this;
        }

        public _Processor process(InputStream is) {
            return new _Processor(is).transform(this);
        }

        public _Processor process(BufferedImage source) {
            return new _Processor(source).transform(this);
        }
    }

    public static class _Processor<T extends _Processor> {
        BufferedImage source;
        volatile BufferedImage target;
        Processor worker;
        float compressionQuality = Float.NaN;

        /**
         * Construct a _Processor using a function that when applied will
         * return the source image for the processor
         *
         * @param source the function that generate a BufferedImage instance
         */
        public _Processor($.Func0<BufferedImage> source) {
            this.source = assertNotNull(source.apply());
        }

        private _Processor(BufferedImage source) {
            this.source = assertNotNull(source);
        }

        private _Processor(InputStream source) {
            this.source = read(source);
        }

        private _Processor(InputStream is, BufferedImage source) {
            this.source = null != source ? source : read(is);
        }

        protected _Processor() {}

        public _Processor transform(Processor operator) {
            assertNotNull(operator);
            if (null == this.worker) {
                this.worker = operator;
                return me();
            } else {
                return pipeline().transform(operator);
            }
        }

        public T compressionQuality(float compressionQuality) {
            this.compressionQuality = N.assertAlpha(compressionQuality);
            return me();
        }

        public T source(InputStream is) {
            this.source = read(is);
            return me();
        }

        public T source(BufferedImage source) {
            this.source = assertNotNull(source);
            return me();
        }

        public _Load pipeline() {
            return new _Load(target());
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
            ImageWriter writer = ImageIO.getImageWritersByMIMEType(mimeType(mimeType)).next();
            dropAlphaChannelIfJPEG(writer);
            ImageWriteParam params = writer.getDefaultWriteParam();

            if (!Float.isNaN(compressionQuality) && params.canWriteCompressed()) {
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionType(params.getCompressionTypes()[0]);
                params.setCompressionQuality(compressionQuality);
            }

            ImageOutputStream ios = os(os);
            writer.setOutput(ios);
            IIOImage image = new IIOImage(target(), null, null);
            try {
                writer.write(null, image, params);
            } catch (IOException e) {
                throw E.ioException(e);
            }
            IO.flush(ios);
            writer.dispose();
        }

        public byte[] toByteArray() {
            return toByteArray(DEF_MIME_TYPE);
        }

        public byte[] toByteArray(String mimeType) {
            ByteArrayOutputStream baos = IO.baos();
            writeTo(baos, mimeType(mimeType));
            return baos.toByteArray();
        }

        public String toBase64() {
            return toBase64(DEF_MIME_TYPE);
        }

        public String toBase64(String mimeType) {
            return Img.toBase64(toByteArray(mimeType), mimeType);
        }

        private BufferedImage target() {
            if (null == target) {
                doJob();
            }
            return target;
        }

        public void dropAlphaChannelIfJPEG(ImageWriter writer) {
            if (writer.getClass().getSimpleName().toUpperCase().contains("JPEG")) {
                BufferedImage src = source;
                BufferedImage convertedImg = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
                convertedImg.getGraphics().drawImage(src, 0, 0, null);
                source = convertedImg;
            }
        }

        private synchronized void doJob() {
            preTransform();
            target = null == worker ? source : worker.source(source).produce();
        }

        protected void preTransform() {
        }

        private T me() {
            return $.cast(this);
        }
    }

    public static class _Resize extends _Processor<_Resize> {
        private int w;
        private int h;
        private boolean keepRatio;
        private float scale = Float.NaN;

        private _Resize(int w, int h, InputStream is, BufferedImage source) {
            super(is, source);
            this.w = assertPositive(w);
            this.h = assertPositive(h);
        }

        private _Resize(float scale, InputStream is, BufferedImage source) {
            super(is, source);
            this.scale = assertPositive(scale);
        }

        public _Resize keepRatio() {
            this.keepRatio = true;
            return this;
        }

        @Override
        protected void preTransform() {
            transform(resizer());
        }

        private Resizer resizer() {
            return Float.isNaN(scale) ? new Resizer(w, h, keepRatio) : new Resizer(scale);
        }
    }

    public static class _Crop extends _Processor<_Crop> {
        private int x1;
        private int y1;
        private int x2;
        private int y2;

        private _Crop(InputStream is, BufferedImage source) {
            super(is, source);
        }

        private _Crop(int x1, int y1, int x2, int y2, InputStream is, BufferedImage source) {
            super(is, source);
            this.x1 = assertNonNegative(x1);
            this.y1 = assertNonNegative(y1);
            this.x2 = x2;
            this.y2 = y2;
        }

        public _Crop from(int x, int y) {
            x1 = assertNonNegative(x);
            y1 = assertNonNegative(y);
            return this;
        }

        public _Crop to(int x, int y) {
            x2 = x;
            y2 = y;
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

    public static class _Watermarker extends _Processor<_Watermarker> {

        Color color = Color.LIGHT_GRAY;
        Font font = new Font("Arial", Font.BOLD, 28);
        float alpha = 0.8f;
        String text;
        int offsetX;
        int offsetY;

        private _Watermarker(String text, InputStream inputStream, BufferedImage source) {
            super(inputStream, source);
            this.text = S.assertNotBlank(text);
        }

        public _Watermarker color(Color color) {
            this.color = assertNotNull(color);
            return this;
        }

        public _Watermarker font(Font font) {
            this.font = assertNotNull(font);
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
        private BufferedImage source;

        private _Load(InputStream is) {
            this.is = assertNotNull(is);
        }

        private _Load(BufferedImage source) {
            this.source = assertNotNull(source);
        }

        public _Resize resize(float scale) {
            return new _Resize(scale, is, source);
        }

        public _Resize resize(int w, int h) {
            return new _Resize(w, h, is, source);
        }

        public _Resize resize($.Tuple<Integer, Integer> dimension) {
            return new _Resize(dimension.left(), dimension.right(), is, source);
        }

        public _Resize resize(Dimension dimension) {
            return new _Resize(dimension.width, dimension.height, is, source);
        }

        public _Crop crop(int x1, int y1, int x2, int y2) {
            return new _Crop(x1, y1, x2, y2, is, source);
        }

        public _Crop crop($.Tuple<Integer, Integer> leftTop, $.Tuple<Integer, Integer> rightBottom) {
            return crop(leftTop._1, leftTop._2, rightBottom._1, rightBottom._2);
        }

        public _Watermarker watermark(String text) {
            return new _Watermarker(text, is, source);
        }

        public _Processor compress(float compressionQuality) {
            return new _Processor(is, source).compressionQuality(compressionQuality).transform(COPIER);
        }

        public _Processor copy() {
            return new _Processor(is, source).transform(COPIER);
        }

        public _Processor transform(Processor processor) {
            return new _Processor(is, source).transform(processor);
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
     * @param mimeType The mime type, if not specified then default to {@link #DEF_MIME_TYPE}
     * @return The base64 encoded value
     */
    public static String toBase64(InputStream inputStream, String mimeType) {
        return toBase64(IO.readContent(inputStream), mimeType);
    }

    /**
     * Encode an image to base64 using a data: URI
     *
     * @param bytes The image byte array
     * @param mimeType the mime type, if not specified then default to {@link #DEF_MIME_TYPE}
     * @return The base64 encoded value
     */
    public static String toBase64(byte[] bytes, String mimeType) {
        return "data:" + mimeType(mimeType) + ";base64," + Codec.encodeBase64(bytes);
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
        if (hint.endsWith("jpeg") || hint.endsWith("jpg")) {
            mimeType = JPG_MIME_TYPE;
        }
        if (hint.endsWith("gif")) {
            mimeType = GIF_MIME_TYPE;
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
        float scale = Float.NaN;
        boolean keepRatio;

        public Resizer(int w, int h, boolean keepRatio) {
            this.w = assertNonNegative(w);
            this.h = assertNonNegative(h);
            this.keepRatio = keepRatio;
        }

        public Resizer(float scale) {
            this.scale = assertNotNaN(scale);
            this.keepRatio = true;
        }

        @Override
        protected BufferedImage run() {
            int w = this.w;
            int h = this.h;
            final int maxWidth = w;
            final int maxHeight = h;

            if (Float.isNaN(scale)) {
                if (w < 0 && h < 0) {
                    w = sourceWidth;
                    h = sourceHeight;
                }

                final double ratio = this.sourceRatio;

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
            } else {
                w = (int) (sourceWidth * scale);
                h = (int) (sourceHeight * scale);
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
            int x2 = this.x2;
            x2 = x2 < 0 ? sourceWidth + x2 : x2;
            int y2 = this.y2;
            y2 = y2 < 0 ? sourceHeight + y2 : y2;

            int width = x2 - x1;
            int height = y2 - y1;

            if (width < 0) {
                x1 = x2;
                width = -width;
            }
            if (height < 0) {
                y1 = y2;
                height = -height;
            }

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

    private static Processor COPIER = new Processor() {
        @Override
        protected BufferedImage run() {
            return source;
        }
    };

    public enum F {
        ;
        /**
         * A function that generate a transparent tracking pixel
         */
        public static $.Producer<BufferedImage> TRACKING_PIXEL = new $.Producer<BufferedImage>() {
            @Override
            public BufferedImage produce() {
                BufferedImage trackPixel = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
                Color transparent = new Color(0, 0, 0, 0);
                trackPixel.setRGB(0, 0, transparent.getRGB());
                return trackPixel;
            }
        };
    }
}

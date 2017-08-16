package org.osgl.util;

import org.osgl.$;
import org.osgl.util.Img.BinarySourceProcessor.ScaleFix;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.*;
import java.net.URL;

import static org.osgl.Lang.requireNotNull;
import static org.osgl.util.E.*;
import static org.osgl.util.N.*;
import static org.osgl.util.S.requireNotBlank;

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

    public static final Color COLOR_TRANSPARENT = new Color(0, 0, 0, 0);

    /**
     * Byte array of a tracking pixel image in gif format
     */
    public static final byte[] TRACKING_PIXEL_BYTES = new _ProcessorStage(F.TRACKING_PIXEL).toByteArray(GIF_MIME_TYPE);

    /**
     * Base64 string of a tracking pixel image in gif format
     */
    public static final String TRACKING_PIXEL_BASE64 = toBase64(TRACKING_PIXEL_BYTES, GIF_MIME_TYPE);

    /**
     * The direction used to process image
     */
    public enum Direction {
        HORIZONTAL, VERTICAL;

        public boolean isHorizontal() {
            return HORIZONTAL == this;
        }

        public boolean isVertical() {
            return VERTICAL == this;
        }

        public N.WH concatenate(N.Dimension d1, N.Dimension d2) {
            return isHorizontal() ? N.dimension(d1.w() + d2.w(), N.max(d1.h(), d2.h()))
                    : N.dimension(N.max(d1.w(), d2.w()), d1.h() + d2.h());
        }

        public void drawImage(Graphics2D g, BufferedImage source1, BufferedImage source2) {
            g.drawImage(source1, 0, 0, source1.getWidth(), source1.getHeight(), null);
            int x2 = 0, y2 = 0;
            if (isHorizontal()) {
                x2 = source1.getWidth();
            } else {
                y2 = source1.getHeight();
            }
            g.drawImage(source2, x2, y2, source2.getWidth(), source2.getHeight(), null);
        }
    }

    /**
     * Base class for image operator function which provides source width, height, ratio parameters
     * on demand
     */
    public abstract static class Processor extends $.Producer<java.awt.image.BufferedImage> {
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

        /**
         * The target image
         */
        protected BufferedImage target;

        /**
         * The graphics
         */
        protected Graphics2D g;

        protected Processor() {
        }

        @Override
        public BufferedImage produce() {
            try {
                return run();
            } finally {
                if (null != g) {
                    g.dispose();
                }
            }
        }

        /*
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
            this.source = requireNotNull(source);
            this.sourceWidth = source.getWidth();
            this.sourceHeight = source.getHeight();
            this.sourceRatio = (double) this.sourceWidth / this.sourceHeight;
            return this;
        }

        public _ProcessorStage process(InputStream is) {
            return new _ProcessorStage(read(is)).transform(this);
        }

        public _ProcessorStage process(BufferedImage source) {
            return new _ProcessorStage(source).transform(this);
        }

        /**
         * Get {@link Graphics2D} instance. If it is not created yet
         * then call {@link #createGraphics2D()} to create the instance
         *
         * @return the g instance
         */
        protected Graphics2D g() {
            if (null == g) {
                g = createGraphics2D();
            }
            return g;
        }

        /**
         * Create the {@link Graphics2D}. This method will trigger
         * {@link #createTarget()} method if target has not been
         * created yet
         *
         * @return an new Graphics2D
         */
        protected Graphics2D createGraphics2D() {
            if (null == target) {
                createTarget();
            }
            return target.createGraphics();
        }

        /**
         * Create {@link #target} image using source width/height. It will
         * use source color model to check if alpha channel should be be
         * added or not
         */
        protected void createTarget() {
            setTargetSpec(sourceWidth, sourceHeight, source.getColorModel().hasAlpha());
        }

        /**
         * Create {@link #target} image using specified width and height.
         *
         * This method will use source code model it check if alpha channel should be
         * added or not
         *
         * @param w the width of target image
         * @param h the height of target image
         */
        protected void setTargetSpec(int w, int h) {
            setTargetSpec(w, h, source.getColorModel().hasAlpha());
        }

        /**
         * Create {@link #target} image using specified width, height and alpha channel flag
         *
         * @param w                the width of target image
         * @param h                the height of target image
         * @param withAlphaChannel whether it shall be created with alpha channel
         */
        protected void setTargetSpec(int w, int h, boolean withAlphaChannel) {
            target = new BufferedImage(w, h, withAlphaChannel ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        }
    }

    /**
     * The base class that process two image sources and produce result image
     */
    public abstract static class BinarySourceProcessor extends Processor {

        /**
         * How to handle two image sources scale mismatch
         */
        public enum ScaleFix {
            /**
             * Scale smaller image to larger one
             */
            SCALE_TO_MAX() {
                @Override
                public int targetScale(int scale1, int scale2) {
                    return N.max(scale1, scale2);
                }
            },

            /**
             * Shrink larger image to smaller one
             */
            SHRINK_TO_MIN() {
                @Override
                public int targetScale(int scale1, int scale2) {
                    return N.min(scale1, scale2);
                }

            },

            /**
             * Do not fix the scale mismatch
             */
            NO_FIX() {
                @Override
                public boolean shouldFix() {
                    return false;
                }
            };

            public int targetScale(int scale1, int scale2) {
                throw unsupport();
            }

            public boolean shouldFix() {
                return true;
            }
        }

        /**
         * The second source image
         */
        protected BufferedImage source2;
        /**
         * The second source image width
         */
        protected int source2Width;
        /**
         * The second source image height
         */
        protected int source2Height;
        /**
         * The second source image width/height ratio
         */
        protected double source2Ratio;

        /**
         * Set second source image. This method will calculate and cache the following
         * parameters about the source image:
         *
         * * {@link #source2Width}
         * * {@link #source2Height}
         * * {@link #source2Ratio}
         *
         * @param source the second source image
         * @return this Processor instance
         */
        public Processor secondSource(BufferedImage source) {
            this.source2 = requireNotNull(source);
            this.source2Width = source.getWidth();
            this.source2Height = source.getHeight();
            this.source2Ratio = (double) this.sourceWidth / this.sourceHeight;
            return this;
        }

    }


    public static class _ProcessorStage<BUILDER extends _ProcessorStage, PROCESSOR extends Processor> extends $.Provider<BufferedImage> {
        BufferedImage source;
        volatile BufferedImage target;
        PROCESSOR worker;
        float compressionQuality = Float.NaN;

        /**
         * Construct a _Processor using a function that when applied will
         * return the source image for the processor
         *
         * @param source the function that generate a BufferedImage instance
         */
        public _ProcessorStage($.Func0<BufferedImage> source) {
            this.source = requireNotNull(source.apply());
        }

        private _ProcessorStage(BufferedImage source) {
            this.source = requireNotNull(source);
        }

        protected _ProcessorStage() {
        }

        @Override
        public BufferedImage get() {
            return target();
        }

        public _ProcessorStage transform(Processor operator) {
            requireNotNull(operator);
            if (null == this.worker) {
                this.worker = (PROCESSOR) operator;
                return me();
            } else {
                return pipeline().transform(operator);
            }
        }

        public BUILDER compressionQuality(float compressionQuality) {
            this.compressionQuality = N.requireAlpha(compressionQuality);
            return me();
        }

        public BUILDER source(InputStream is) {
            this.source = read(is);
            return me();
        }

        public BUILDER source(BufferedImage source) {
            this.source = requireNotNull(source);
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
                throw ioException(e);
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

        private BUILDER me() {
            return $.cast(this);
        }
    }

    public static class _Resize extends _ProcessorStage<_Resize, Resizer> {

        private _Resize(int w, int h, BufferedImage source) {
            super(source);
            this.worker = resizer(w, h, Float.NaN, false);
        }

        private _Resize(float scale, BufferedImage source) {
            super(source);
            this.worker = resizer(0, 0, requirePositive(scale), true);
        }

        public _Resize keepRatio() {
            this.worker.keepRatio = true;
            return this;
        }

        private Resizer resizer(int w, int h, float scale, boolean keepRatio) {
            return Float.isNaN(scale) ? new Resizer(w, h, keepRatio) : new Resizer(scale);
        }
    }

    public static class _Crop extends _ProcessorStage<_Crop, Cropper> {

        private _Crop(BufferedImage source) {
            super(source);
        }

        private _Crop(int x1, int y1, int x2, int y2, BufferedImage source) {
            super(source);
            this.worker = new Cropper(x1, y1, x2, y2);
        }

        public _Crop from(int x, int y) {
            this.worker.x1 = x;
            this.worker.y1 = y;
            return this;
        }

        public _Crop to(int x, int y) {
            this.worker.x2 = x;
            this.worker.y2 = y;
            return this;
        }

        public _Crop from($.Tuple<Integer, Integer> leftTop) {
            return from(leftTop._1, leftTop._2);
        }
    }

    public static class _Watermarker extends _ProcessorStage<_Watermarker, WaterMarker> {

        private _Watermarker(String text, BufferedImage source) {
            super(source);
            this.worker = new WaterMarker(requireNotBlank(text));
        }

        public _Watermarker color(Color color) {
            this.worker.color = requireNotNull(color);
            return this;
        }

        public _Watermarker font(Font font) {
            this.worker.font = requireNotNull(font);
            return this;
        }

        public _Watermarker alpha(float alpha) {
            this.worker.alpha = N.requireAlpha(alpha);
            return this;
        }

        public _Watermarker offset(int offsetX, int offsetY) {
            this.worker.offsetX = offsetX;
            this.worker.offsetY = offsetY;
            return this;
        }

        public _Watermarker offsetY(int offsetY) {
            this.worker.offsetY = offsetY;
            return this;
        }

        public _Watermarker offsetX(int offsetX) {
            this.worker.offsetX = offsetX;
            return this;
        }
    }

    public static class _Blur extends _ProcessorStage<_Blur, Blur> {

        private _Blur(BufferedImage source) {
            this(Blur.DEFAULT_LEVEL, source);
        }

        private _Blur(int level, BufferedImage source) {
            super(source);
            this.worker = new Blur(level);
        }

    }

    public static class _Concatenate extends _ProcessorStage<_Concatenate, Concatenater> {

        protected _Concatenate(BufferedImage secondSource, BufferedImage source) {
            super(source);
            this.worker = new Concatenater(secondSource);
        }

        public _Concatenate dir(Direction dir) {
            this.worker.dir = requireNotNull(dir);
            return this;
        }

        public _Concatenate horizontally() {
            this.worker.dir = Direction.HORIZONTAL;
            return this;
        }

        public _Concatenate vertically() {
            this.worker.dir = Direction.VERTICAL;
            return this;
        }

        public _Concatenate shinkToSmall() {
            this.worker.scaleFix = ScaleFix.SHRINK_TO_MIN;
            return this;
        }

        public _Concatenate scaleToMax() {
            this.worker.scaleFix = ScaleFix.SCALE_TO_MAX;
            return this;
        }

        public _Concatenate noScaleFix() {
            this.worker.scaleFix = ScaleFix.NO_FIX;
            return this;
        }

        public _Concatenate scaleFix(ScaleFix scaleFix) {
            this.worker.scaleFix = requireNotNull(scaleFix);
            return this;
        }

        public _Concatenate background(Color backgroundColor) {
            this.worker.background = requireNotNull(backgroundColor);
            return this;
        }

        public _Concatenate reverse() {
            this.worker.reversed = !this.worker.reversed;
            return this;
        }

    }


    public static class _Load extends $.Provider<BufferedImage> {

        private BufferedImage source;

        private _Load(InputStream is) {
            this.source = read(is);
        }

        private _Load(BufferedImage source) {
            this.source = requireNotNull(source);
        }

        @Override
        public BufferedImage get() {
            return source;
        }

        public _Resize resize(float scale) {
            return new _Resize(scale, source);
        }

        public _Resize resize(int w, int h) {
            return new _Resize(w, h, source);
        }

        public _Resize resize($.Tuple<Integer, Integer> dimension) {
            return new _Resize(dimension.left(), dimension.right(), source);
        }

        public _Resize resize(Dimension dimension) {
            return new _Resize(dimension.width, dimension.height, source);
        }

        public _Crop crop(int x1, int y1, int x2, int y2) {
            return new _Crop(x1, y1, x2, y2, source);
        }

        public _Crop crop($.Tuple<Integer, Integer> leftTop, $.Tuple<Integer, Integer> rightBottom) {
            return crop(leftTop._1, leftTop._2, rightBottom._1, rightBottom._2);
        }

        public _Watermarker watermark(String text) {
            return new _Watermarker(text, source);
        }

        public _Blur blur() {
            return new _Blur(source);
        }

        public _Blur blur(int level) {
            return new _Blur(level, source);
        }

        public _ProcessorStage flip() {
            return flip(Direction.HORIZONTAL);
        }

        public _ProcessorStage flipVertial() {
            return flip(Direction.VERTICAL);
        }

        public _ProcessorStage flip(Direction dir) {
            return new _ProcessorStage(source).transform(new Flip(dir));
        }

        public _ProcessorStage compress(float compressionQuality) {
            return new _ProcessorStage(source).compressionQuality(compressionQuality).transform(COPIER);
        }

        public _ProcessorStage copy() {
            return new _ProcessorStage(source).transform(COPIER);
        }

        public _ProcessorStage transform(Processor processor) {
            return new _ProcessorStage(source).transform(processor);
        }

        public _Concatenate appendWith($.Provider<BufferedImage> secondImange) {
            return new _Concatenate(secondImange.apply(), source);
        }

        public _Concatenate appendTo($.Provider<BufferedImage> firstImage) {
            return appendWith(firstImage).reverse();
        }

        public _Concatenate appendWith(BufferedImage secondImange) {
            return new _Concatenate(secondImange, source);
        }

        public _Concatenate appendTo(BufferedImage firstImage) {
            return appendWith(firstImage).reverse();
        }

    }

    public static _Load source(InputStream is) {
        return new _Load(is);
    }

    public static _Load source(File file) {
        return new _Load(IO.is(file));
    }

    public static _Load source($.Func0<BufferedImage> imageProducer) {
        return new _ProcessorStage<>(imageProducer).pipeline();
    }

    public static _Load source(BufferedImage image) {
        return new _ProcessorStage<>(image).pipeline();
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
     * @param mimeType    The mime type, if not specified then default to {@link #DEF_MIME_TYPE}
     * @return The base64 encoded value
     */
    public static String toBase64(InputStream inputStream, String mimeType) {
        return toBase64(IO.readContent(inputStream), mimeType);
    }

    /**
     * Encode an image to base64 using a data: URI
     *
     * @param bytes    The image byte array
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
            throw ioException(e);
        }
    }

    public static ImageOutputStream os(OutputStream os) {
        return new MemoryCacheImageOutputStream(os);
    }

    public static BufferedImage read(InputStream is) {
        try {
            return ImageIO.read(is);
        } catch (Exception e) {
            throw unexpected(e);
        }
    }

    public static BufferedImage read(File file) {
        try {
            return ImageIO.read(file);
        } catch (Exception e) {
            throw unexpected(e);
        }
    }

    public static BufferedImage read(URL url) {
        try {
            return ImageIO.read(url);
        } catch (Exception e) {
            throw unexpected(e);
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
            this.w = requireNonNegative(w);
            this.h = requireNonNegative(h);
            this.keepRatio = keepRatio;
        }

        public Resizer(float scale) {
            this.scale = requireNotNaN(scale);
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
            setTargetSpec(w, h);
            Graphics g = g();
            if (!source.getColorModel().hasAlpha()) {
                // Create a white background if not transparency define
                g = target.getGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w, h);
            }
            Image srcResized = source.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            g.drawImage(srcResized, 0, 0, null);
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

            int w = x2 - x1;
            int h = y2 - y1;

            if (w < 0) {
                x1 = x2;
                w = -w;
            }
            if (h < 0) {
                y1 = y2;
                h = -h;
            }

            // out
            setTargetSpec(w, h);
            Image croppedImage = source.getSubimage(x1, y1, w, h);
            Graphics g = g();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.drawImage(croppedImage, 0, 0, null);
            return target;
        }
    }

    private static class Flip extends Processor {
        Direction dir;

        Flip(Direction dir) {
            this.dir = requireNotNull(dir);
        }

        @Override
        protected BufferedImage run() {
            Graphics2D g = g();
            if (dir.isHorizontal()) {
                g.drawImage(source, sourceWidth, 0, -sourceWidth, sourceHeight, null);
            } else {
                g.drawImage(source, 0, sourceHeight, sourceWidth, -sourceHeight, null);
            }
            return target;
        }
    }


    private static class Blur extends Processor {
        static final int DEFAULT_LEVEL = 3;

        float[] matrix;
        int level;

        Blur(int level) {
            this.level = requirePositive(level);
            int max = level * level;
            matrix = new float[requirePositive(max)];
            for (int i = 0; i < max; ++i) {
                matrix[i] = (float) 1 / (float) max;
            }
        }

        @Override
        protected BufferedImage run() {
            Graphics2D g = g();
            g.drawImage(source, 0, 0, null);
            BufferedImageOp op = new ConvolveOp(new Kernel(level, level, matrix), ConvolveOp.EDGE_NO_OP, null);
            target = op.filter(target, null);
            return target;
        }
    }

    private static class WaterMarker extends Processor {

        Color color = Color.LIGHT_GRAY;
        Font font = new Font("Arial", Font.BOLD, 28);
        float alpha = 0.8f;
        String text;
        int offsetX;
        int offsetY;

        WaterMarker(String text) {
            this.text = text;
        }

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
            Graphics2D g = g();
            g.drawImage(source, 0, 0, w, h, null);
            g.setColor(color);
            g.setFont(font);
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, alpha));

            FontMetrics fontMetrics = g.getFontMetrics();
            Rectangle2D rect = fontMetrics.getStringBounds(text, g);
            int centerX = (w - (int) rect.getWidth() + offsetX) / 2;
            int centerY = (h - (int) rect.getHeight() + offsetY) / 2;
            g.drawString(text, centerX, centerY);
            return target;
        }

    }

    private static class Concatenater extends BinarySourceProcessor {

        /**
         * Define the direction to concatenate two image sources
         *
         * Default value is {@link Direction#VERTICAL}
         */
        Direction dir = Direction.HORIZONTAL;

        /**
         * Define the stategy to handle scale mismatch of two image sources
         *
         * Default value is {@link ScaleFix#SCALE_TO_MAX}
         */
        ScaleFix scaleFix = ScaleFix.SCALE_TO_MAX;

        /**
         * The background color
         */
        Color background = COLOR_TRANSPARENT;

        boolean reversed = false;

        Concatenater(BufferedImage secondImage) {
            this.secondSource(secondImage);
        }

        Concatenater(BufferedImage secondImage, Direction dir, ScaleFix scaleFix, Color background) {
            this.secondSource(secondImage);
            this.dir = requireNotNull(dir);
            this.scaleFix = requireNotNull(scaleFix);
            this.background = requireNotNull(background);
        }

        @Override
        protected BufferedImage run() {
            if (dir.isHorizontal()) {
                fixScale(sourceHeight, source2Height);
            } else {
                fixScale(sourceWidth, source2Width);
            }
            N.Dimension d = dir.concatenate(N.wh(sourceWidth, sourceHeight), N.wh(source2Width, source2Height));
            int w = d.w(), h = d.h();
            setTargetSpec(w, h);
            Graphics2D g = g();
            g.setColor(background);
            g.fillRect(0, 0, w, h);
            if (!reversed) {
                dir.drawImage(g, source, source2);
            } else {
                dir.drawImage(g, source2, source);
            }
            return target;
        }

        private void fixScale(int scale1, int scale2) {
            if (scale1 != scale2 && scaleFix.shouldFix()) {
                int targetScale = scaleFix.targetScale(scale1, scale2);
                float r1 = (float) targetScale / (float) scale1;
                float r2 = (float) targetScale / (float) scale2;
                if (N.neq(r1, 1.0f)) {
                    source(new Resizer(r1).source(source).run());
                }
                if (N.neq(r2, 1.0f)) {
                    secondSource(new Resizer(r2).source(source2).run());
                }
            }
        }
    }


    private static int randomColorValue() {
        int a = N.randInt(256);
        int r = N.randInt(256);
        int g = N.randInt(256);
        int b = N.randInt(256);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static Processor COPIER = new Processor() {
        @Override
        protected BufferedImage run() {
            return source;
        }
    };

    /**
     * The namespace for functions
     */
    public enum F {
        ;

        public static $.Producer<Integer> RANDOM_COLOR_VALUE = new $.Producer<Integer>() {
            @Override
            public Integer produce() {
                return randomColorValue();
            }
        };

        /**
         * A function that generates a transparent tracking pixel
         */
        public static $.Producer<BufferedImage> TRACKING_PIXEL = new $.Producer<java.awt.image.BufferedImage>() {
            @Override
            public BufferedImage produce() {
                BufferedImage trackPixel = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR);
                trackPixel.setRGB(0, 0, COLOR_TRANSPARENT.getRGB());
                return trackPixel;
            }
        };

        /**
         * A function that generates a transparent background in rectangular area
         *
         * @param w the width
         * @param h the height
         * @return a function as described above
         */
        public static $.Producer<BufferedImage> background(final int w, final int h) {
            return background(w, h, $.val(COLOR_TRANSPARENT.getRGB()));
        }

        /**
         * A function that generates a background image with pixels with random color
         *
         * @param w the width
         * @param h the height
         * @return a function as described above
         */
        public static $.Producer<BufferedImage> randomPixels(final int w, final int h) {
            return background(w, h, RANDOM_COLOR_VALUE);
        }

        /**
         * A function that generates a background in rectangular area with color specified
         *
         * @param w the width
         * @param h the height
         * @return a function as described above
         */
        public static $.Producer<BufferedImage> background(final int w, final int h, final $.Func0<Integer> colorValueProvider) {
            $.NPE(colorValueProvider);
            requirePositive(w);
            requirePositive(h);
            return new $.Producer<BufferedImage>() {
                @Override
                public BufferedImage produce() {
                    BufferedImage b = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
                    for (int i = 0; i < w; ++i) {
                        for (int j = 0; j < h; ++j) {
                            b.setRGB(i, j, colorValueProvider.apply());
                        }
                    }
                    return b;
                }
            };
        }
    }

}

package org.osgl.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.osgl.util.N.XY;

public class ImgTest {

    private static InputStream img1() {
        URL url = ImgTest.class.getResource("/img/img1.png");
        return IO.is(url);
    }

    private static InputStream img2() {
        URL url = ImgTest.class.getResource("/img/img2.jpg");
        return IO.is(url);
    }

    static void testCrop() {
        Img.source(img1()).crop(XY(30, 30), XY(100, 100)).writeTo(new File("/tmp/img1_crop.gif"));
    }

    static void testResize() {
        Img.source(img1()).resize(XY(100, 200)).writeTo(new File("/tmp/img1_resize.png"));
    }

    static void testResizeKeepRatio() {
        Img.source(img1()).resize(100, 200).keepRatio().writeTo(new File("/tmp/img1_resize_keep_ratio.png"));
    }

    private static void testIllegalArguments() {
        try {
            Img.source(img2()).resize(0.0f).writeTo("/tmp/img2_resize_zero.png");
            E.unexpected("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    static void testWatermarkWithDefSetting() {
        Img.source(img1()).watermark("CONFIDENTIAL").writeTo("/tmp/img1_watermark_def.png");
    }

    static void testWatermark() {
        Img.source(img1()).watermark("CONFIDENTIAL").offsetY(-200).color(Color.DARK_GRAY).writeTo("/tmp/img1_watermark.png");
    }

    private static void testCompress() {
        Img.source(img1()).compress(0.01f).writeTo("/tmp/img1_compress.jpeg");
    }

    private static void testCopy() {
        Img.source(img1()).copy().writeTo("/tmp/img1_copy.jpeg");
    }

    private static void testPipeline() {
        Img.source(img1())
                .resize(300, 400)
                .pipeline()
                .crop(50, 50, 250, 350)
                .pipeline()
                .watermark("HELLO OSGL")
                .writeTo("/tmp/img1_pipeline.png");
    }


    private static void testResizeByScale() {
        Img.source(img2()).resize(0.5f).writeTo("/tmp/img2_resize_scale.png");
    }

    private static void testProcessJPEGfile() {
        Img.source(img2())
                .resize(640, 480)
                .pipeline()
                .crop(50, 50, -50, -50)
                .pipeline()
                .watermark("HELLO OSGL")
                .writeTo("/tmp/img2_pipeline.jpg");
    }

    private static void testGenerateTrackingPixel() {
        IO.write(Img.TRACKING_PIXEL_BYTES, new File("/tmp/tracking_pixel.gif"));
    }

    private static class Sunglass extends Img.Processor {
        private float alpha = 0.3f;

        Sunglass() {}
        Sunglass(float alpha) {this.alpha = alpha;}

        @Override
        protected BufferedImage run() {
            int w = sourceWidth;
            int h = sourceHeight;
            BufferedImage target = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g.drawImage(source, 0, 0, w, h, null);
            return target;
        }
    }
    private static void testCustomizedProcessor() {
        // style A
        new Sunglass(0.7f).process(img2()).pipeline().resize(0.5f).writeTo("/tmp/img2_sunglass_style_a.jpg");
        // style B
        Img.source(img2()).resize(0.3f).transform(new Sunglass()).writeTo("/tmp/img2_sunglass_style_b.jpg");
    }

    public static void main(String[] args) {
        testResize();
        testResizeByScale();
        testResizeKeepRatio();
        testCrop();
        testWatermarkWithDefSetting();
        testWatermark();
        testCompress();
        testCopy();
        testPipeline();
        testProcessJPEGfile();
        testGenerateTrackingPixel();
        testCustomizedProcessor();
        testIllegalArguments();
    }

}

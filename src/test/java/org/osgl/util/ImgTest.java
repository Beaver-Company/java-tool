package org.osgl.util;

import java.awt.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

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
        Img.source(img1()).crop(N.XY(30, 30), N.XY(100, 100)).writeTo(new File("/tmp/img1_crop.gif"));
    }

    static void testResize() {
        Img.source(img1()).resize(N.XY(100, 200)).writeTo(new File("/tmp/img1_resize.png"));
    }

    static void testResizeKeepRatio() {
        Img.source(img1()).resize(100, 200).keepRatio().writeTo(new File("/tmp/img1_resize_keep_ratio.png"));
    }

    private static void testResizeByScale() {
        Img.source(img2()).resize(0.5f).writeTo("/tmp/img2_resize_scale.png");
    }

    private static void testResizeToZero() {
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
        new Img._Processor(Img.F.TRACKING_PIXEL).writeTo("/tmp/tracking_pixel.gif");
    }

    public static void main(String[] args) {
        testResizeToZero();
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
    }

}

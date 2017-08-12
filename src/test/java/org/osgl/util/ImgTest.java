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

    static void testCrop() {
        Img.source(img1()).crop(N.XY(30, 30), N.XY(100, 100)).writeTo(new File("/tmp/img1_crop.gif"));
    }

    static void testResize() {
        Img.source(img1()).resize(N.XY(100, 200)).writeTo(new File("/tmp/img1_resize.png"));
    }

    static void testResizeKeepRatio() {
        Img.source(img1()).resize(100, 200).keepRatio().writeTo(new File("/tmp/img1_resize_keep_ratio.png"));
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

    public static void main(String[] args) {
        testResize();
        testResizeKeepRatio();
        testCrop();
        testWatermarkWithDefSetting();
        testWatermark();
        testCompress();
        testCopy();
        testPipeline();
    }

}

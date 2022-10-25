package top.zouhd;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.io.File;
import java.net.URL;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class BMPLoader {

    private static final Log log = LogFactory.get();
    static int width;
    static int height;

    static String[][] list;

    BMPLoader(String url) {
        try {
            BufferedImage image = ImageIO.read(new URL(url));
            width = image.getWidth();
            height = image.getHeight();

            list = new String[width][height];
            int minx = image.getMinX();
            int miny = image.getMinY();
            int[] rgb = new int[3];
            for (int i = minx; i < width; i++) {
                for (int j = miny; j < height; j++) {
                    int pixel = image.getRGB(i, j);
                    rgb[0] = (pixel & 0xff0000) >> 16;
                    rgb[1] = (pixel & 0xff00) >> 8;
                    rgb[2] = (pixel & 0xff);
                    list[i][j] = rgb[0] + "," + rgb[1] + "," + rgb[2];

                }
            }
        } catch (Exception e) {
            log.error("签到图片加载失败");
        }
    }

    public void writeImg(String img) {
        try {
            BufferedImage image = ImageIO.read(new URL(img));
            BufferedImage t = new BufferedImage(width, image.getHeight() * width / image.getWidth(), BufferedImage.TYPE_INT_RGB);
            t.getGraphics().drawImage(image.getScaledInstance(t.getWidth(), t.getHeight(), BufferedImage.SCALE_SMOOTH), 0, 0, null);
            ImageIO.write(t, "jpg", new File("/Users/zouhd/Desktop/best.jpg"));
        } catch (Exception e) {
            log.error("签到图片写入失败");
        }
    }
    // 改变成二进制码
    public String[][] getPX(String img) {
        int[] rgb = new int[3];
        BufferedImage bi = null;
        try {
            URL url = new URL(img);
            BufferedImage image = ImageIO.read(url);
            if (width == 0 && height == 0) {
                width = image.getWidth();
                height = image.getHeight();
            }
            bi = new BufferedImage(width, image.getHeight() * width / image.getWidth(), BufferedImage.TYPE_INT_RGB);
            bi.getGraphics().drawImage(image.getScaledInstance(bi.getWidth(), bi.getHeight(), BufferedImage.SCALE_SMOOTH), 0, 0, null);
        } catch (Exception e) {
            log.error("图片加载失败: {}", img);
            return null;
        }
        int width = bi.getWidth();
        int height = bi.getHeight();
        int minx = bi.getMinX();
        int miny = bi.getMinY();
        String[][] list = new String[width][height];
        for (int i = minx; i < width; i++) {
            for (int j = miny; j < height; j++) {
                int pixel = bi.getRGB(i, j);
                rgb[0] = (pixel & 0xff0000) >> 16;
                rgb[1] = (pixel & 0xff00) >> 8;
                rgb[2] = (pixel & 0xff);
                list[i][j] = rgb[0] + "," + rgb[1] + "," + rgb[2];

            }
        }
        return list;

    }

    public int compareImage(String img) {

        if (null == img) {
            return 0;
        }
        String[][] list1 = list;
        String[][] list2 = getPX(img);
        int samePixel = 0;
        int notSamePixel = 0;
        int i = 0, j = 0;
        for (String[] strings : list1) {
            if ((i + 1) == list1.length) {
                continue;
            }
            for (int m = 0; m < strings.length; m++) {
                try {
                    String[] value1 = list1[i][j].split(",");
                    String[] value2 = list2[i][j].split(",");
                    int k = 0;
                    for (int n = 0; n < value2.length; n++) {
                        if (Math.abs(Integer.parseInt(value1[k]) - Integer.parseInt(value2[k])) < 5) {
                            samePixel++;
                        } else {
                            notSamePixel++;
                        }
                    }
                } catch (RuntimeException e) {
                    continue;
                }
                j++;
            }
            i++;
        }
        double score;
        try {
            score = (double) samePixel / (samePixel + notSamePixel) * 100;
        } catch (Exception e) {
            score = Double.parseDouble("0");
        }
        return (int) score;
    }
}

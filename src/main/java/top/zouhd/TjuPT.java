package top.zouhd;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.*;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static cn.hutool.http.HttpUtil.createGet;

public class TjuPT {
    private static final Log log = LogFactory.get();

    public static final int SUCCESS = 0;
    public static final int ALREADY_CHECKED_IN = 1;
    public static final int NOT_FOUND_LOCAL_IMAGE = 2;
    public static final int NETWORK_ERROR = 3;
    String tjuptUrl;
    Douban douban;

    TjuPT() {
        tjuptUrl = "https://tjupt.org/";
        douban = new Douban();
    }

    public int checkIn() {
        HttpCookie cookie = connect();
        if (cookie == null) {
            return NETWORK_ERROR;
        }
        String html = Objects.requireNonNull(getRespose(cookie)).body();
        // 已签到
        if (ReUtil.contains("今日已签到", html)) {
            String text = ReUtil.extractMulti("<p>今日已签到，已累计签到 <b>(\\d+)</b> 次，已连续签到 <b>(\\d+)</b> 天，今日获得了 <b>(\\d+)</b> 个魔力值。</p></td></tr></table>", html,
                    "今日已签到，已累计签到$1次，已连续签到$2天，今日获得了$3个魔力值。");
            log.info(text);
            return ALREADY_CHECKED_IN;
        }
        String captchaUrl = "https://tjupt.org" + ReUtil.get("src='(/pic/attend/\\d+-\\d+-\\d+/.*.jpg)'", html, 1);
        List<String> options = ReUtil.findAll("<input type='radio' name='answer' value='\\d+-\\d+-\\d+.*?>(.*?)<", html, 1);
        for (String option : options) {
            log.info("选项：{}", option);
        }
        int max_score = 0;
        String answer = "";
        InputStream in = HttpUtil.createGet(captchaUrl).cookie(cookie).execute().bodyStream();
        BufferedImage image;
        try {
            image = ImageIO.read(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BMPLoader bmpLoader = new BMPLoader(image);
        for (String option : options) {

//            List<String> doubanImgs = douban.getPics(option);
            File file = new File("src/main/resources/images/" + option + ".jpg");
            if (!file.exists()) {
                log.error("本地图片{}不存在", option);
                continue;
            }
            BufferedImage localImag = null;
            try {
                localImag = ImageIO.read(file);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int score = bmpLoader.compareImage(localImag);
            if (score > max_score) {
                max_score = score;
                answer = option;
            }
            log.info("{}分。正在识别豆瓣电影《{}》海报，地址：{}", score, option);
        }
        // 最高分分数过低则退出
        if (max_score < 60) {
            log.error("找不到匹配的图片，刷新重试中......");
            return NOT_FOUND_LOCAL_IMAGE;
        }
        log.info("签到图片：{}", captchaUrl);
        log.info("最佳匹配：{}, 分数：{}", answer, max_score);

        String answerId = ReUtil.get("name='answer' value='(\\d+-\\d+-\\d+ \\d+:\\d+:\\d+&\\d+)'>" + answer + "<", html, 1);


        HttpRequest request = HttpUtil.createRequest(Method.POST, tjuptUrl + "attendance.php");
        request.setFollowRedirects(true)
                .cookie(cookie)
                .keepAlive(true)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                .disableCache()
                .form("answer", answerId);
        HttpResponse response = request.execute();
        String finishCaptcha = response.body();
        // TODO 判断是否签到成功 修改正则表达式
        if (ReUtil.contains("签到成功", finishCaptcha)) {
            String text = ReUtil.extractMulti("<p>签到成功，这是您的第 <b>(\\d+)</b> 次签到，已连续签到 <b>(\\d+)</b> 天，本次签到获得 <b>(\\d+)</b> 个魔力值。", html,
                    "签到成功，已累计签到$1次，已连续签到$2天，今日获得了$3个魔力值。");
            log.info(text);
        } else {
            log.error("签到失败");
            return NETWORK_ERROR;

        }
        return SUCCESS;
    }

    private HttpResponse getRespose(HttpCookie cookie) {
        HttpRequest request = HttpUtil.createRequest(Method.GET, tjuptUrl + "attendance.php");
        request.setFollowRedirects(true)
                .cookie(cookie)
                .keepAlive(true)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                .disableCache();
        HttpResponse response = request.execute();
        if (response.getStatus() != 200) {
            log.error("attendance登录失败, 状态码: {}", response.getStatus());
            return null;
        }
        return response;
    }

    private HttpCookie connect() {
        Properties properties = new Properties();
        try {
            InputStream in = App.class.getClassLoader().getResourceAsStream("src/main/resources/config.properties");
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("读取配置文件失败");
            return null;
        }
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");
        HttpRequest request = HttpUtil.createRequest(Method.POST, tjuptUrl + "takelogin.php");
        request.setFollowRedirects(false)
                .form("username", username)
                .form("password", password)
                .keepAlive(true)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                .disableCache();
        HttpResponse response = request.execute();
        if (response.getStatus() != 302) {
            if (response.getStatus() == 200) {
                log.error("登录失败，可能是账号密码错误");
            } else {
                log.error("登录失败，可能是网站出现问题。状态码：{}", response.getStatus());
            }
            return null;
        }
        log.info("登录成功");
        return response.getCookie("access_token");

    }

}

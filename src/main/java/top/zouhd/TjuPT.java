package top.zouhd;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.*;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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
    public static final int UNKNOWN_ERROR = 4;
    String tjuptUrl;

    Properties properties;

    TjuPT() {
        tjuptUrl = "https://tjupt.org/";
        properties = new Properties();
        try {
            InputStream in = App.class.getClassLoader().getResourceAsStream("src/main/resources/config.properties");
            properties.load(in);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("读取配置文件失败");
        }
    }

    public int checkIn() {
        HttpCookie cookie;
        if (properties.getProperty("cookie_hasExpired") != null && properties.getProperty("cookie_hasExpired").equals("false")) {
            cookie = new HttpCookie(properties.getProperty("cookie_name"), properties.getProperty("cookie_value"));
            cookie.setPath(properties.getProperty("cookie_path"));
            cookie.setDomain(properties.getProperty("cookie_domain"));
            cookie.setVersion(Integer.parseInt(properties.getProperty("cookie_version")));
            cookie.setDomain(properties.getProperty("cookie_domain"));
            cookie.setHttpOnly(Boolean.parseBoolean(properties.getProperty("cookie_http_only")));
            cookie.setSecure(Boolean.parseBoolean(properties.getProperty("cookie_secure")));
        } else {
            cookie = connect();
        }
        if (cookie == null) {
            expireCookie(properties);
            return NETWORK_ERROR;
        }
        HttpResponse resp = getRespose(cookie);
        if (resp == null) {
            expireCookie(properties);
            return NETWORK_ERROR;
        }
        String html = resp.body();
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
        if (ReUtil.contains("签到成功", finishCaptcha)) {
            String text = ReUtil.extractMulti("<p>签到成功，这是您的第 <b>(\\d+)</b> 次签到，已连续签到 <b>(\\d+)</b> 天，本次签到获得 <b>(\\d+)</b> 个魔力值。", html,
                    "签到成功，已累计签到$1次，已连续签到$2天，今日获得了$3个魔力值。");
            log.info(text);
        } else {
            log.error("签到失败");
            expireCookie(properties);
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
                .form("logout", "90days")
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
        HttpCookie cookie = response.getCookie("access_token");
        properties.setProperty("cookie_name", cookie.getName());
        properties.setProperty("cookie_value", cookie.getValue());
        properties.setProperty("cookie_domain", cookie.getDomain());
        properties.setProperty("cookie_path", cookie.getPath());
        properties.setProperty("cookie_expires", cookie.getMaxAge() + "");
        properties.setProperty("cookie_secure", cookie.getSecure() + "");
        properties.setProperty("cookie_httpOnly", cookie.isHttpOnly() + "");
        properties.setProperty("cookie_version", cookie.getVersion() + "");
        properties.setProperty("cookie_hasExpired", cookie.hasExpired() + "");
        try {
            properties.store(new BufferedOutputStream(Files.newOutputStream(Paths.get("src/main/resources/config.properties"))), "Store Cookie");
            log.info("access_token已保存，有效期：{}天", cookie.getMaxAge() / 60 / 60 / 24);
        } catch (IOException e) {
            log.error("access_token保存失败");
        }
        return cookie;

    }

    private void expireCookie(Properties properties) {
        properties.setProperty("cookie_hasExpired", "true");
        try {
            properties.store(new BufferedWriter(new FileWriter("src/main/resources/config.properties")), "cookie");
            log.info("Cookie已手动过期");
        } catch (IOException e) {
            log.info("Cookie过期失败");
        }
    }

}

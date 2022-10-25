package top.zouhd;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.*;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.HttpCookie;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class TjuPT {
    private static final Log log = LogFactory.get();
    String tjuptUrl;
    Douban douban;

    TjuPT() {
        tjuptUrl = "https://tjupt.org/";
        douban = new Douban();
    }

    public void checkIn() {
        HttpCookie cookie = connect();
        if (cookie == null) {
            return;
        }
        String html = Objects.requireNonNull(getRespose(cookie)).body();
        if (ReUtil.contains("今日已签到", html)) {
            String text = ReUtil.extractMulti("<p>今日已签到，已累计签到 <b>(\\d+)</b> 次，已连续签到 <b>(\\d+)</b> 天，今日获得了 <b>(\\d+)</b> 个魔力值。</p></td></tr></table>", html,
                    "今日已签到，已累计签到$1次，已连续签到$2天，今日获得了$3个魔力值。");
            log.info(text);
            return;
        }
        // TODO 根据html获取签到链接
        String captchaUrl = ReUtil.get("src=\"(captcha.php\\?\\d+)\"", html, 1);
        List<String> options = ReUtil.findAll("value=\"(\\d+)\"", html, 1);
//        String captchaUrl = "https://zouhd.top/images/ttt.jpeg";
//        List<String> options = new ArrayList<>();
//        options.add("独行月球");
//        options.add("无间道");
//        options.add("少年的你");
//        options.add("大鱼海棠");
        int max_score = 0;
        int count = 0;
//        String ansUrl = "";
        String answer = "";
        BMPLoader bmpLoader = new BMPLoader(captchaUrl);
        for (String option : options) {

            List<String> doubanImgs = douban.getPics(option);

            for (String doubanImg : doubanImgs) {

                int score = bmpLoader.compareImage(doubanImg);
                if (score > max_score) {
                    max_score = score;
                    answer = option;
//                    ansUrl = doubanImg;
                }
//                log.info("{}分。{}正在识别豆瓣电影《{}》海报，地址：{}", score, ++count, option, doubanImg);
            }
        }
        // TODO 分数过低则退出
        if (max_score < 20) {
            log.error("找不到匹配的图片，请手动签到！");
            return;
        }
        // 测试用，将文件写入本地
//        bmpLoader.writeImg(ansUrl);
        log.info("最佳匹配：" + answer + "，分数：" + max_score);


        HttpRequest request = HttpUtil.createRequest(Method.POST, tjuptUrl + "attendance.php");
        request.setFollowRedirects(true)
                .cookie(cookie)
                .keepAlive(true)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
                .disableCache()
                .form("captcha", answer);
        HttpResponse response = request.execute();
        String finishCaptcha = response.body();
        // TODO 判断是否签到成功 修改正则表达式
        if (ReUtil.contains("签到成功", finishCaptcha)) {
            String text = ReUtil.extractMulti("<p>签到成功，已累计签到 <b>(\\d+)</b> 次，已连续签到 <b>(\\d+)</b> 天，今日获得了 <b>(\\d+)</b> 个魔力值。</p></td></tr></table>", html,
                    "签到成功，已累计签到$1次，已连续签到$2天，今日获得了$3个魔力值。");
            log.info(text);
        } else {
            log.error("签到失败");
        }
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
            // idea中运行时，使用路径 src/main/resources/config.properties
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

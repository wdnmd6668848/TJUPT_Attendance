package top.zouhd;

import cn.hutool.core.util.ReUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.util.List;

public class Douban {


    private static final Log log = LogFactory.get();
    private static final String DOUBAN_URL = "https://movie.douban.com/j/subject_suggest?q=";


    public List<String> getPics(String name) {
        String searchUrl = DOUBAN_URL + name;
        String result = HttpUtil.get(searchUrl);
        if (result.length() < 10) {
            log.error("豆瓣搜索结果为空");
            return null;
        }
        String id = JSONUtil.parseArray(result).getJSONObject(0).getStr("id");
        String url = "https://movie.douban.com/subject/" + id + "/photos?type=R";
        String html = HttpUtil.get(url);
        if (html.length() < 10) {
            log.error("豆瓣电影海报页面为空");
            return null;
        }
        return ReUtil.findAll("src=\"(https://img\\d.doubanio.com/view/photo/m/public/p\\d+.jpg)\"", html, 1);
    }
}

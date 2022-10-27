package top.zouhd;

import cn.hutool.http.HttpUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.io.InputStream;
import java.util.Properties;

public class PushDeer {

    private static final Log log = LogFactory.get();
    private String URL;
    private String PUSHDEER_KEY;

    PushDeer () {
        Properties properties = new Properties();
        try {
            InputStream in = App.class.getClassLoader().getResourceAsStream("src/main/resources/config.properties");
            properties.load(in);
            URL = properties.getProperty("pushDeerUrl");
            PUSHDEER_KEY = properties.getProperty("pushDeerKey");
        } catch (Exception e) {
            log.error("加载PushDeer配置失败");
        }
    }

    public void send(String message) {
        HttpUtil.get(URL + "?pushkey=" + PUSHDEER_KEY + "&text=【北洋园】" + message);
    }


}

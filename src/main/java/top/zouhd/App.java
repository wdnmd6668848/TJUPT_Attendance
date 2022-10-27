package top.zouhd;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.io.InputStream;
import java.util.Properties;

/**
 * Hello world!
 */
public class App {
    private static final Log log = LogFactory.get();

    public static void main(String[] args) {
        TjuPT pt = new TjuPT();
        int code = 4;
        int retry = 0;
        for (; retry < 10; retry++) {
            // 0 为签到成功，1 为已签到，2 为未找到本地图片，3 为网络故障，4 为未知错误
            code = pt.checkIn();
            if (code == TjuPT.NOT_FOUND_LOCAL_IMAGE || code == TjuPT.NETWORK_ERROR) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                break;
            }
        }
        if (code != 0 && code != 1) {
            PushDeer pushDeer = new PushDeer();
            pushDeer.send("签到失败\n重试次数" + (retry - 1) + "，请查看具体日志");
        }
    }


}

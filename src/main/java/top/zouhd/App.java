package top.zouhd;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        TjuPT pt = new TjuPT();

        for (int i = 0; i < 10; i++) {
            // 0 为签到成功，1 为已签到，2 为未找到本地图片，3 为网络故障，4 为未知错误
            int code = pt.checkIn();
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
    }




}

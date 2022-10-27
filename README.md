# TJUPT_Attendance 北洋园自动签到

## 介绍
实现北洋园自动签到，可通过定时任务每天定时签到，签到成功后发送PushDeer通知。

## 使用方法
1. 复制`src/main/resources/config.template.properties`文件到相同的目录，并命名为`config.properties`
2. 添加TJUPT的用户名和密码
3. 添加PushDeer的pushkey
4. Maven 打包
   ```sh
    mvn clean package
    ```
5. 复制target目录下的jar包和`/lib`, `/src`到服务器(或本地)
6. 运行
   ```sh
    java -jar tjupt-attendance-0.1.jar
    ```

## 注意事项
* 如果`images`文件夹中没有找到签到图片，脚本会自动刷新重试（最多10次）。签到失败，日志输出失败原因为匹配不到图片，可能是因为TJUPT的验证码图片更新了，需要去签到页面下载最新的验证码图片添加到`src/main/resources/images/****.jpg`(使用正确答案命名文件。刷新几次把没保存的尽可能都下载下来)
* 如果签到成功或失败，但是没有收到PushDeer通知，可能是PushDeer的pushkey填写错误，或者PushDeer的服务器出现问题。
* Linux使用`Crontab`定时任务：
    ```sh
     0 0 * * * java -jar /path/to/tjupt-attendance-0.1.jar >> checkin.log 2>&1 
     ```
* MacOS可以使用`launchd`定时任务。

## 声明
本项目仅供学习交流使用，不得用于商业用途，如有侵权，请联系删除。
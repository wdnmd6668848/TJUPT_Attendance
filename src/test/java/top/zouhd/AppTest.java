package top.zouhd;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest 
{
    /**
     * Rigorous Test :-)
     */
    PushDeer pushDeer = new PushDeer();
    @Test
    public void shouldAnswerWithTrue()
    {
        pushDeer.send("【北洋园】测试发送消息！%0A 你好");
    }
}

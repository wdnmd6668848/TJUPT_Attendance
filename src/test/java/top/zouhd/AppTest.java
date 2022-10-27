package top.zouhd;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

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
        pushDeer.send("测试发送消息！%0A 你好");
    }
}

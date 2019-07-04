package gyuhwan;

import android.os.Handler;
import android.os.Message;

/**
 * Created by Myown on 2017-08-09.
 */

public class CustomHandler extends Handler {

    // 데이터 통신용 핸들러
    // String 값 으로 쓰레드간의 메시지를 전달한다.

    private String msg;

    public CustomHandler(){
        this.msg = null;
    }

    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);
        this.msg = (String)msg.obj;
    }

    public String getMsg(){
        return this.msg;
    }
}

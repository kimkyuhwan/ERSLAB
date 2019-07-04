package gyuhwan;

import android.os.Message;

import org.json.JSONObject;

/**
 * Created by Myown on 2017-08-09.
 */

public class ReqHTTPJSONThread extends Thread {

    // Http 통신을 하는 쓰레드
    // 이 클래스의 객체를 생성할 때 생성자의 arg로 url과 JSON 객체를 전달하고
    // 쓰레드를 실행시키면 통신이 완료

    private JSONObject json_data;
    private String send_url;
    private String conn_result;

    public CustomHandler handler;

    public ReqHTTPJSONThread(String send_url, JSONObject json_data){
        this.json_data = json_data;
        this.send_url = send_url;
        this.conn_result = null;
        this.handler = new CustomHandler();
    }

    @Override
    public void run() {
        SendJSONHttp http = new SendJSONHttp();
        this.conn_result = http.sendJSONObj(this.send_url, this.json_data);
        Message msg = this.handler.obtainMessage();
        msg.obj = (Object)this.conn_result;
        this.handler.handleMessage(msg);
    }
}

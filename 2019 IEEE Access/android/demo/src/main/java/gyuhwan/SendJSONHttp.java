package gyuhwan;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Myown on 2017-08-09.
 */

public class SendJSONHttp {

    // JSON 데이터를 Http를 통해서 Url로 전송하는 메소드를 구현한 클래스
    // sendJSONObj 함수를 이용해서 JSON 객체를 해당 URL로 전송 가능
    // sendJSONObj 함수의 리턴 값으로 전송 결과 String 을 받음

    public SendJSONHttp(){
        // default constructor
    }

    public String sendJSONObj(String send_url, JSONObject send_data){
        String ret = "";
        InputStream is = null;

        try {
            URL url = new URL(send_url);
            HttpURLConnection httpConn = (HttpURLConnection)url.openConnection();

            String json_data = send_data.toString();

            httpConn.setRequestProperty("Accept", "application/json");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("charset","utf-8");

            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);

            OutputStream os = httpConn.getOutputStream();
            os.write(json_data.getBytes("UTF-8"));
            os.flush();

            try{
                is = httpConn.getInputStream();
                if (is != null){
                    ret = convertInputStreamToString(is);
                }else{
                    ret = "failed";
                }
            }catch (IOException ioex){
                ioex.printStackTrace();
            }
            finally {
                httpConn.disconnect();
            }
        }
        catch (MalformedURLException murlex){
        murlex.printStackTrace();
        }
        catch (IOException ioex){
        ioex.printStackTrace();
        }
        return ret;
    }

    private static String convertInputStreamToString(InputStream inputStream) throws IOException {
        BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream));
        String line = "";
        String result = "";
        while((line = bufferedReader.readLine()) != null)
            result += line;

        inputStream.close();
        return result;
    }
}

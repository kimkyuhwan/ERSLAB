package gyuhwan;

/**
 * Created by erslab-gh on 2017-12-12.
 */

public class Constants {
    public static String DEFAULT_HTTP_ADDRESS="http://165.246.43.96/";
    public static final String REQ_MOUSE_POSTION=DEFAULT_HTTP_ADDRESS+"mouse";
    public static final String REQ_START=DEFAULT_HTTP_ADDRESS+"start";
    public static final String REQ_END=DEFAULT_HTTP_ADDRESS+"end";
    public static final String REQ_SAVE=DEFAULT_HTTP_ADDRESS+"save";
    public static final int RESULT_CODE_SUCCESS=200;
    public static final int RESULT_CODE_FAILED=500;

    public static final int MODE_NORMAL=0;
    public static final int MODE_AUTO=1;
    public static final int PLAY_MODE_STATE=MODE_NORMAL;
    public static final int FAKE_NETWORK_TYPE = 3;
    public static final int PLAYMODE_SSIM=0;
    public static final int PLAYMODE_KNAPSACK=1;
    public static final int PLAYMODE_HEURISTIC=2;
    public static final int DATASPEC_REFERENCE_CONSTANT=2000;

}
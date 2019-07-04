package gyuhwan;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.exoplayer2.demo.R;

/**
 * Created by erslab-gh on 2017-07-10.
 */

public class SettingDialog extends Dialog {
    private EditText mEditText[];
    private Button Ok_Btn;
    private Spinner ssimSpinner;
    private Spinner resolutionSpinner;
    private String[] ssimValue={"0.99","0.98","0.97"};
    private String[] knapsackValue={"85","90","95","100"};

    private String[] resolutionValue={"240p","360p","480p","720p","1080p","ERS","DASH"};
    private CheckBox ssim_checkbox, knapsack_checkbox, heuristic_checkbox;
    private View.OnClickListener OkClickListener;
    private TextView video_resolution_text, mode_select_text;

    private final int MODE_SSIM=0;
    private final int MODE_KNAPSACK=1;
    private final int MODE_HEURISTIC=2;



    private int modeSelect;


    // 클릭버튼이 확인과 취소 두개일때 생성자 함수로 이벤트를 받는다
    public SettingDialog(Context context, ISettingDialogEventListener onSettingDialogEventListener) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        this.OkClickListener = OkClickListener;
        this.onSettingDialogEventListener=onSettingDialogEventListener;
    }

    public interface ISettingDialogEventListener {
        public void settingDialogEvent(int minbuffervalue,int maxbuffervalue,int playbackbuffervalue,int playbackrebuffervalue);
    }

    private ISettingDialogEventListener onSettingDialogEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEditText=new EditText[4];
        // 다이얼로그 외부 화면 흐리게 표현
        WindowManager.LayoutParams lpWindow = new WindowManager.LayoutParams();
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        lpWindow.dimAmount = 0.8f;
        getWindow().setAttributes(lpWindow);

        setContentView(R.layout.setting_dialog);

        mEditText[0]=(EditText)findViewById(R.id.EditText_min_buffer_ms);
        mEditText[1]=(EditText)findViewById(R.id.EditText_max_buffer_ms);
        mEditText[2]=(EditText)findViewById(R.id.EditText_playback_buffer_ms);
        mEditText[3]=(EditText)findViewById(R.id.EditText_playback_rebuffer_ms);
        video_resolution_text=(TextView)findViewById(R.id.text_setting_resolution);
        mode_select_text=(TextView)findViewById(R.id.text_mode_select);
        ssimSpinner =(Spinner)findViewById(R.id.Spinner_SSIM);
        resolutionSpinner = (Spinner)findViewById(R.id.Spinner_Resolution);
        ArrayAdapter<String> adapter=new ArrayAdapter<String>(getContext(),R.layout.listitem_ssim,ssimValue);
        ArrayAdapter<String> adapterR=new ArrayAdapter<String>(getContext(),R.layout.listitem_ssim,resolutionValue);
        ssimSpinner.setAdapter(adapter);
        ssimSpinner.setSelection(0);
        resolutionSpinner.setAdapter(adapterR);
        resolutionSpinner.setSelection(0);
        Ok_Btn=(Button)findViewById(R.id.Gokbtn);
        Ok_Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSettingDialogEventListener.settingDialogEvent(Integer.parseInt(mEditText[0].getText().toString()),Integer.parseInt(mEditText[1].getText().toString()),Integer.parseInt(mEditText[2].getText().toString()),Integer.parseInt(mEditText[3].getText().toString()));
                PlayerClock.getInstance().setMaxBufferTime(Long.parseLong(mEditText[1].getText().toString()));
                PlayerClock.getInstance().setPlayMode(modeSelect);
                Log.d("DEBUGYU","MODE SELECT "+modeSelect);
                switch (modeSelect){
                    case MODE_SSIM:
                        PlayerClock.getInstance().setSsimStd(ssimValue[ssimSpinner.getSelectedItemPosition()]);
                        int mode_idx=resolutionSpinner.getSelectedItemPosition();
                        if(Constants.PLAY_MODE_STATE==Constants.MODE_AUTO) mode_idx--;
                        PlayerClock.getInstance().setMacro_mode_index(mode_idx);

                        break;
                    case MODE_KNAPSACK:
                    case MODE_HEURISTIC:
                        PlayerClock.getInstance().setSelectKnapsackEnergyRate(knapsackValue[ssimSpinner.getSelectedItemPosition()]);
                        break;

                }
                dismiss();
            }
        });

        ssim_checkbox=(CheckBox)findViewById(R.id.mode_select_ssim);
        knapsack_checkbox=(CheckBox)findViewById(R.id.mode_select_knapsack);
        heuristic_checkbox=(CheckBox)findViewById(R.id.mode_select_heuristic);
        ssim_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    ArrayAdapter<String> adapter=new ArrayAdapter<String>(getContext(),R.layout.listitem_ssim,ssimValue);
                    ssimSpinner.setAdapter(adapter);
                    ssimSpinner.setSelection(0);
                    resolutionSpinner.setVisibility(View.VISIBLE);
                    video_resolution_text.setVisibility(View.VISIBLE);
                    mode_select_text.setText("SSIM Standard");
                    knapsack_checkbox.setChecked(false);
                    heuristic_checkbox.setChecked(false);
                    modeSelect=MODE_SSIM;
                }
            }
        });

        knapsack_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    ArrayAdapter<String> adapter=new ArrayAdapter<String>(getContext(),R.layout.listitem_ssim,knapsackValue);
                    ssimSpinner.setAdapter(adapter);
                    ssimSpinner.setSelection(0);
                    resolutionSpinner.setVisibility(View.INVISIBLE);
                    video_resolution_text.setVisibility(View.INVISIBLE);
                    mode_select_text.setText("Energy Limit Percent");
                    ssim_checkbox.setChecked(false);
                    heuristic_checkbox.setChecked(false);
                    modeSelect=MODE_KNAPSACK;
                }
            }
        });

        heuristic_checkbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    ArrayAdapter<String> adapter=new ArrayAdapter<String>(getContext(),R.layout.listitem_ssim,knapsackValue);
                    ssimSpinner.setAdapter(adapter);
                    ssimSpinner.setSelection(0);
                    resolutionSpinner.setVisibility(View.INVISIBLE);
                    video_resolution_text.setVisibility(View.INVISIBLE);
                    mode_select_text.setText("Energy Limit Percent");
                    ssim_checkbox.setChecked(false);
                    knapsack_checkbox.setChecked(false);
                    modeSelect=MODE_HEURISTIC;
                }
            }
        });

    }

}

package cn.blue16.waveformcreator_spi;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * 函数信号发生器主控制界面
 * 功能：通过蓝牙连接STM32设备，控制函数信号发生器的波形参数
 */
public class MainActivity extends AppCompatActivity {

    // 常量定义
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String DEVICE_NAME = "JDY-31-SPP";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    
    // 参数范围常量
    private static final int MIN_FREQUENCY = 1;
    private static final int MAX_FREQUENCY = 1000000;
    private static final int MIN_AMPLITUDE = 1;
    private static final int MAX_AMPLITUDE = 3300;
    private static final int MIN_PHASE = 0;
    private static final int MAX_PHASE = 360;

    // 蓝牙相关
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private boolean isConnected = false;

    // UI组件
    private AutoCompleteTextView spinnerWaveform;
    private TextInputEditText editFrequency;
    private TextInputEditText editAmplitude;
    private TextInputEditText editPhase;
    private MaterialButton btnSend;
    private MaterialButton btnConnect;
    private ImageView ivConnectionStatus;
    private TextView tvConnectionStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupWaveformSpinner();
        setupClickListeners();
        requestBluetoothPermissions();
    }

    /**
     * 初始化UI组件
     */
    private void initializeViews() {
        spinnerWaveform = findViewById(R.id.spinner_waveform);
        editFrequency = findViewById(R.id.edit_frequency);
        editAmplitude = findViewById(R.id.edit_amplitude);
        editPhase = findViewById(R.id.edit_phase);
        btnSend = findViewById(R.id.btn_send);
        btnConnect = findViewById(R.id.btn_connect);
        ivConnectionStatus = findViewById(R.id.iv_connection_status);
        tvConnectionStatus = findViewById(R.id.tv_connection_status);
        
        // 初始状态设置
        updateConnectionStatus(false);
        btnSend.setEnabled(false);
    }

    /**
     * 设置波形选择器
     */
    private void setupWaveformSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.waveforms_array, android.R.layout.simple_dropdown_item_1line);
        spinnerWaveform.setAdapter(adapter);
        
        // 设置默认选择
        if (adapter.getCount() > 0) {
            spinnerWaveform.setText(adapter.getItem(0).toString(), false);
        }
    }

    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        btnSend.setOnClickListener(v -> {
            if (validateInputs()) {
                sendData();
            }
        });

        btnConnect.setOnClickListener(v -> {
            if (isConnected) {
                disconnectBluetooth();
            } else {
                connectBluetooth();
            }
        });
    }

    /**
     * 请求蓝牙权限
     */
    private void requestBluetoothPermissions() {
        String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
        };

        boolean needRequest = false;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                needRequest = true;
                break;
            }
        }

        if (needRequest) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                showToast(getString(R.string.bluetooth_permission_denied));
            }
        }
    }

    /**
     * 连接蓝牙设备
     */
    private void connectBluetooth() {
        if (!checkBluetoothSupport()) {
            return;
        }

        updateConnectionStatus(false, getString(R.string.status_connecting));
        
        new Thread(() -> {
            try {
                BluetoothDevice targetDevice = findTargetDevice();
                if (targetDevice == null) {
                    runOnUiThread(() -> {
                        updateConnectionStatus(false);
                        showToast(getString(R.string.device_not_found));
                    });
                    return;
                }

                connectToDevice(targetDevice);
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateConnectionStatus(false);
                    showToast(getString(R.string.connection_failed));
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 检查蓝牙支持
     */
    private boolean checkBluetoothSupport() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            showToast(getString(R.string.bluetooth_not_supported));
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                showToast(getString(R.string.bluetooth_permission_denied));
                return false;
            }
            bluetoothAdapter.enable();
        }
        
        return true;
    }

    /**
     * 查找目标设备
     */
    private BluetoothDevice findTargetDevice() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
            return null;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevices) {
            if (DEVICE_NAME.equals(device.getName())) {
                return device;
            }
        }
        return null;
    }

    /**
     * 连接到指定设备
     */
    private void connectToDevice(BluetoothDevice device) throws IOException {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
            throw new IOException("Bluetooth permission not granted");
        }

        bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
        bluetoothSocket.connect();
        outputStream = bluetoothSocket.getOutputStream();
        
        runOnUiThread(() -> {
            updateConnectionStatus(true);
            showToast(getString(R.string.connection_success));
        });
    }

    /**
     * 断开蓝牙连接
     */
    private void disconnectBluetooth() {
        try {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                bluetoothSocket.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            bluetoothSocket = null;
            outputStream = null;
            updateConnectionStatus(false);
        }
    }

    /**
     * 更新连接状态显示
     */
    private void updateConnectionStatus(boolean connected) {
        updateConnectionStatus(connected, null);
    }

    /**
     * 更新连接状态显示
     * @param connected 是否已连接
     * @param customMessage 自定义状态消息
     */
    private void updateConnectionStatus(boolean connected, String customMessage) {
        isConnected = connected;
        
        if (customMessage != null) {
            tvConnectionStatus.setText(customMessage);
            ivConnectionStatus.setImageResource(R.drawable.ic_bluetooth_disabled);
            ivConnectionStatus.setColorFilter(ContextCompat.getColor(this, R.color.warning_color));
        } else if (connected) {
            tvConnectionStatus.setText(getString(R.string.status_connected));
            ivConnectionStatus.setImageResource(R.drawable.ic_bluetooth);
            ivConnectionStatus.setColorFilter(ContextCompat.getColor(this, R.color.success_color));
            btnConnect.setText(getString(R.string.disconnect));
        } else {
            tvConnectionStatus.setText(getString(R.string.status_disconnected));
            ivConnectionStatus.setImageResource(R.drawable.ic_bluetooth_disabled);
            ivConnectionStatus.setColorFilter(ContextCompat.getColor(this, R.color.error_color));
            btnConnect.setText(getString(R.string.connect));
        }
        
        btnSend.setEnabled(connected);
    }

    /**
     * 验证输入参数
     */
    private boolean validateInputs() {
        // 验证波形选择
        if (TextUtils.isEmpty(spinnerWaveform.getText())) {
            showToast(getString(R.string.please_select_waveform));
            return false;
        }

        // 验证频率
        String frequencyStr = editFrequency.getText().toString().trim();
        if (TextUtils.isEmpty(frequencyStr)) {
            editFrequency.setError(getString(R.string.invalid_frequency));
            return false;
        }
        
        try {
            int frequency = Integer.parseInt(frequencyStr);
            if (frequency < MIN_FREQUENCY || frequency > MAX_FREQUENCY) {
                editFrequency.setError(getString(R.string.frequency_range));
                return false;
            }
        } catch (NumberFormatException e) {
            editFrequency.setError(getString(R.string.invalid_frequency));
            return false;
        }

        // 验证幅度
        String amplitudeStr = editAmplitude.getText().toString().trim();
        if (TextUtils.isEmpty(amplitudeStr)) {
            editAmplitude.setError(getString(R.string.invalid_amplitude));
            return false;
        }
        
        try {
            int amplitude = Integer.parseInt(amplitudeStr);
            if (amplitude < MIN_AMPLITUDE || amplitude > MAX_AMPLITUDE) {
                editAmplitude.setError(getString(R.string.amplitude_range));
                return false;
            }
        } catch (NumberFormatException e) {
            editAmplitude.setError(getString(R.string.invalid_amplitude));
            return false;
        }

        // 验证相位
        String phaseStr = editPhase.getText().toString().trim();
        if (TextUtils.isEmpty(phaseStr)) {
            editPhase.setError(getString(R.string.invalid_phase));
            return false;
        }
        
        try {
            int phase = Integer.parseInt(phaseStr);
            if (phase < MIN_PHASE || phase > MAX_PHASE) {
                editPhase.setError(getString(R.string.phase_range));
                return false;
            }
        } catch (NumberFormatException e) {
            editPhase.setError(getString(R.string.invalid_phase));
            return false;
        }

        return true;
    }

    /**
     * 发送数据到设备
     */
    private void sendData() {
        if (!isConnected || outputStream == null) {
            showToast(getString(R.string.status_disconnected));
            return;
        }

        // 构建命令字符串
        String command = buildCommand();
        
        // 显示发送进度
        AlertDialog progressDialog = createProgressDialog(getString(R.string.sending_data));
        progressDialog.show();

        new Thread(() -> {
            try {
                outputStream.write(command.getBytes());
                outputStream.flush();
                
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showToast(getString(R.string.data_send_success));
                });
                
            } catch (IOException e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    showToast(getString(R.string.data_send_failed));
                    updateConnectionStatus(false);
                });
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * 构建发送命令
     */
    private String buildCommand() {
        // 获取波形类型索引
        String[] waveforms = getResources().getStringArray(R.array.waveforms_array);
        int waveformIndex = 0;
        String selectedWaveform = spinnerWaveform.getText().toString();
        for (int i = 0; i < waveforms.length; i++) {
            if (waveforms[i].equals(selectedWaveform)) {
                waveformIndex = i;
                break;
            }
        }

        // 格式化参数
        String waveform = String.valueOf(waveformIndex);
        String frequency = String.format("%07d", Integer.parseInt(editFrequency.getText().toString()));
        String amplitude = String.format("%04d", Integer.parseInt(editAmplitude.getText().toString()));
        String phase = String.format("%03d", Integer.parseInt(editPhase.getText().toString()));

        return "CMD" + waveform + frequency + amplitude + phase;
    }

    /**
     * 创建进度对话框
     */
    private AlertDialog createProgressDialog(String message) {
        return new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .create();
    }

    /**
     * 显示Toast消息
     */
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBluetooth();
    }
}

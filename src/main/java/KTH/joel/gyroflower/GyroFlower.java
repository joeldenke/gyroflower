package KTH.joel.gyroflower;

import android.content.Context;
import android.content.Intent;

import android.app.Activity;
import android.graphics.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;

/**
 * @description Main activity, handling sensor events and animations
 * @author Joel Denke, Mathias Westman
 *
 */

enum AnimationType {ROTATE, WITHER}

public class GyroFlower extends Activity implements SensorEventListener
{
    private static final int RESULT_SETTINGS = 10;

    private SensorManager manager;
    private Sensor accSensor, magneticSensor;
    private float[] accValues, magneticValues;
    private float[] radValues = new float[3], degreeValues = new float[3], rotation = new float[9];

    private TextView azimuthView, pitchView, rollView, responseView;
    private Flower flower;
    private ImageView flowerImage;
    private long lastUpdate = System.currentTimeMillis(), shuffleStartTime = 0;
    private float previousAngle;
    private float[] acceleration = new float[3];
    private boolean startedShuffle = false;
    private double shuffleMargin = 0;

    /*
    * @description Alpha smooth constant for low pass filter.
    * See: http://en.wikipedia.org/wiki/Low-pass_filter#Discrete-time_realization
    */
    static final float ALPHA = 0.5f;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
	    super.onCreate(savedInstanceState);
        initUI();
    }

    /**
     * @description view message on the screen
     * @author Joel Denke, Mathias Westman
     *
     */
    public synchronized void viewMessage(String message, boolean flash)
    {
        setResponse(message);
        if (flash)
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * @description Set response text field
     * @author Joel Denke, Mathias Westman
     *
     */
    public synchronized void setResponse(String response)
    {
        responseView.setText(response);
    }


    /**
     * @description Initiaties GUI components
     * @author Joel Denke, Mathias Westman
     *
     */
    private void initUI()
    {
        // init the GUI
        setTitle(R.string.mainTitle);
        setContentView(R.layout.main);
        LinearLayout context = (LinearLayout)findViewById(R.id.context);
        context.setBackgroundColor(Color.WHITE);

        responseView = (TextView) findViewById(R.id.response);
        responseView.setTextColor(Color.BLACK);

        azimuthView = (TextView) findViewById(R.id.textViewAzimut);
        pitchView = (TextView) findViewById(R.id.textViewPitch);
        rollView = (TextView) findViewById(R.id.textViewRoll);
        flowerImage = (ImageView) findViewById(R.id.flower);
        flower = new Flower(this, flowerImage);

        manager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    /**
     * @description Apply rotation or wither animation for the flower
     * @author Joel Denke, Mathias Westman
     *
     */
    public synchronized void doAnimation(AnimationType type, float angle)
    {
            switch (type) {
                case WITHER:
                    flower.animateWither(flowerImage);
                    break;
                case ROTATE:
                    flower.rotateImage(flowerImage, angle);
                    break;
            }
    }

    /**
     * @description Make sure flower image is shown when window is in focus
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public void onWindowFocusChanged(boolean bool)
    {
        doAnimation(AnimationType.ROTATE, 0);
    }

    /**
     * @description Wither flower if shaking with larger or equals 2G.
     * @author Joel Denke, Mathias Westman
     *
     */
    private void shakeIt(SensorEvent event)
    {
        float[] values = event.values;
        // Movement
        float x = values[0];
        float y = values[1];
        float z = values[2];

        // Calculate absolute acceleration vector and divide by gravity "vector" (g = a^2)
        // This is the same as calculate g-force.
        float gForce = (x * x + y * y + z * z) / (SensorManager.GRAVITY_EARTH * SensorManager.GRAVITY_EARTH);

        if (gForce >= (2 - shuffleMargin))
        {
            if (!startedShuffle) {
                shuffleStartTime = System.currentTimeMillis();
                shuffleMargin = 1.5;
            }

            startedShuffle = true;

            long actualTime = System.currentTimeMillis();

            long diff = Math.abs(shuffleStartTime - actualTime);
            Log.d("force", String.format("Current force: %f", gForce));
            Log.d("force", String.format("Current time diff: %d", diff));

            if (startedShuffle && diff >= 1000) {
                    doAnimation(AnimationType.WITHER, 0);
                    startedShuffle = false;
                    shuffleStartTime = 0;
                    shuffleMargin = 0;
            }

            Log.d("animation", String.format("Device was shuffed %f", gForce));
        } else {
            if (startedShuffle) {
                startedShuffle = false;
            }
        }
    }

    /**
     * @description Assign new values when sensors changed
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public void onSensorChanged(SensorEvent event)
    {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                accValues = lowPass(event.values);
                Log.i("SensorListener", "got acc data");
                shakeIt(event);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                magneticValues = event.values;
                Log.i("SensorListener", "got magnetic data");
                break;
        }
        calculateOrientation();
    }

    /**
     * @description Apply a low pass filtering on acceleration
     * @author Joel Denke, Mathias Westman
     *
     * @see http://en.wikipedia.org/wiki/Low-pass_filter#Algorithmic_implementation
     * @see http://developer.android.com/reference/android/hardware/SensorEvent.html#values
     */
    protected float[] lowPass(float[] input)
    {
        for ( int i=0; i < 3; i++) {
            this.acceleration[i] = this.acceleration[i] + ALPHA * (input[i] - this.acceleration[i]);
        }

        return acceleration;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {}

    /**
     * @description Calculate current orientation
     * @author Joel Denke, Mathias Westman (Most parts is borrowed from Anders Lindström)
     *
     */
    private void calculateOrientation()
    {
        if (accValues != null && magneticValues != null) {
            // calculate the rotation of the device
            // ref: http://developer.android.com/guide/topics/sensors/sensors_position.html
            SensorManager.getRotationMatrix(rotation, null, accValues, magneticValues);
            SensorManager.getOrientation(rotation, radValues);
            // rad to degrees
            for (int i = 0; i < radValues.length; i++) {
                degreeValues[i] = (float) Math.toDegrees(radValues[i]);
            }
            azimuthView.setText("Azimut " + degreeValues[0]);
            pitchView.setText("Pitch " + degreeValues[1]);
            rollView.setText("Roll " + degreeValues[2]);

            float angleDiff = Math.abs(previousAngle - degreeValues[2]);
            if (angleDiff > 10) {
                doAnimation(AnimationType.ROTATE, degreeValues[2]);
            }

            previousAngle = degreeValues[2];
        }
    }

    /**
     * @description Unregister sensor listeners
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    protected void onPause()
    {
        super.onPause();
        manager.unregisterListener(this);
    }

    /**
     * @description View flower and register sensors again
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    protected void onResume()
    {
        super.onResume();
        initUI();
        doAnimation(AnimationType.ROTATE, 0);

        manager.registerListener(this, accSensor,
                SensorManager.SENSOR_DELAY_UI);
        manager.registerListener(this, magneticSensor,
               SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * @description When preference activity is finished
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RESULT_SETTINGS:
                // Maybe do something here later, when you get back from settings
                break;

        }

    }

    /**
     * @description Create menu from xml
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    /**
     * @description Menu actions
     * @author Joel Denke, Mathias Westman
     *
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
            case R.id.menu_settings:
                Intent i = new Intent(this, SettingsActivity.class);
                startActivityForResult(i, RESULT_SETTINGS);
                break;
        }

        return true;
    }
}

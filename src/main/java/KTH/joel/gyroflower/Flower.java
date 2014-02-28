package KTH.joel.gyroflower;

import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;

/**
 * @description Flower animation handler
 * @author Joel Denke, Mathias Westman
 *
 */
public class Flower
{
    private GyroFlower context;
    private AnimationDrawable animation = new AnimationDrawable();
    private boolean running = false;

    public Flower(GyroFlower context, ImageView flower)
    {
        this.context = context;
        //this.flower = flower;
    }

    /**
     * @description Get flower image depending on rotation angle.
     * @author Joel Denke, Mathias Westman
     *
     */
    private Drawable getFlowerImage(int angle)
    {
         int id = angle + 90;
        Resources resources = context.getResources();

         switch (id) {
             case 0   : return resources.getDrawable(R.drawable.flower_0);
             case 10  : return resources.getDrawable(R.drawable.flower_10);
             case 20  : return resources.getDrawable(R.drawable.flower_20);
             case 30  : return resources.getDrawable(R.drawable.flower_30);
             case 40  : return resources.getDrawable(R.drawable.flower_40);
             case 50  : return resources.getDrawable(R.drawable.flower_50);
             case 60  : return resources.getDrawable(R.drawable.flower_60);
             case 70  : return resources.getDrawable(R.drawable.flower_70);
             case 80  : return resources.getDrawable(R.drawable.flower_80);
             case 90  : return resources.getDrawable(R.drawable.flower_90);
             case 100 : return resources.getDrawable(R.drawable.flower_100);
             case 110 : return resources.getDrawable(R.drawable.flower_110);
             case 120 : return resources.getDrawable(R.drawable.flower_120);
             case 130 : return resources.getDrawable(R.drawable.flower_130);
             case 140 : return resources.getDrawable(R.drawable.flower_140);
             case 150 : return resources.getDrawable(R.drawable.flower_150);
             case 160 : return resources.getDrawable(R.drawable.flower_160);
             case 170 : return resources.getDrawable(R.drawable.flower_170);
             case 180 : return resources.getDrawable(R.drawable.flower_180);
             default  : return resources.getDrawable(R.drawable.flower_90);
         }
    }

    /**
     * @description Round up to closet factor of 10
     * @author Joel Denke, Mathias Westman
     *
     */
    private int trimRotationValue(float angle)
    {
        if (angle % 10 == 0) {
            return (int)angle;
        } else {
             // Round up here
            return Math.round(angle/10) * 10;
        }
    }

    /**
     * @description Animates flower withering
     * @author Joel Denke, Mathias Westman
     *
     */
    public void animateWither(ImageView flowerView)
    {
        if (running) {
            return;
        }
        /*
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN) {
            flowerView.setBackgroundDrawable(null);
        } else {
            flowerView.setBackground(null);
        } */

        //ImageView flower = (ImageView) context.findViewById(R.id.flower2);
        //flowerView.invalidate();
        flowerView.setImageDrawable(null);
        flowerView.setBackgroundResource(R.drawable.wither);

        animation = (AnimationDrawable) flowerView.getBackground();
        Log.d("animation", String.format("Number of frames %d", animation.getNumberOfFrames()));
        Log.d("animation", String.format("Duration %d", animation.getDuration(0)));

        animation.setOneShot(true);
        animation.setVisible(true, false);
        animationCompleteTask(animation);
        running = true;
        animation.start();
        Log.d("animation", String.format("Animation started"));
    }

    /**
     * @description Run a task in background of animation to check when it is complete
     * @author Joel Denke, Mathias Westman
     *
     */
    private void animationCompleteTask(AnimationDrawable anim)
    {
        final AnimationDrawable a = anim;
        Handler h = new Handler();
        h.postDelayed(new Runnable(){
            public void run() {
                if (a.getCurrent() != a.getFrame(a.getNumberOfFrames() - 1)){
                    animationCompleteTask(a);
                } else {
                    // Animation is complete, unlock
                    a.setVisible(false, false);
                    running = false;
                }
            }
        }, 100);
    }

    /**
     * @description "Rotate" flower depending on angle
     * @author Joel Denke, Mathias Westman
     *
     */
    public void rotateImage(ImageView view, float rotateAngle)
    {
        if (running) {
            return;
        }
        int angle = trimRotationValue(rotateAngle);
        view.setImageDrawable(getFlowerImage(angle));
        view.invalidate();
    }
}

package oscilloscope.mijaz.com.bluescope;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

public class WaveformView extends SurfaceView implements SurfaceHolder.Callback{

    private WaveformPlotThread plot_thread;
    private int time_setting = 1;

    private final int width = 512;
    private final int height = 255;

    private static int[] ch1_data = new int[1024];
    private static int ch1_pos = 127;

    private Paint ch1_color = new Paint();
    private Paint grid_paint = new Paint();
    private Paint cross_paint = new Paint();
    private Paint outline_paint = new Paint();

    public WaveformView(Context context, AttributeSet attrs) {

        super(context, attrs);
        //super(context);
        getHolder().addCallback(this);

        int i;
        for(i=0; i<width; i++){
            ch1_data[i] = ch1_pos;
        }

        plot_thread = new WaveformPlotThread(getHolder(), this);
        //setFocusable(true);
        ch1_color.setColor(Color.YELLOW);
        grid_paint.setColor(Color.rgb(100, 100, 100));
        cross_paint.setColor(Color.rgb(70, 100, 70));
        outline_paint.setColor(Color.GREEN);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
        // TODO Auto-generated method stub

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        plot_thread.setRunning(true);
        plot_thread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        plot_thread.setRunning(false);
        while (retry){
            try{
                plot_thread.join();
                retry = false;
            }catch(InterruptedException e){

            }
        }

    }

    @Override
    public void onDraw(Canvas canvas){
        PlotPoints(canvas);

    }

    public void set_data(int[] data1 ){
        int x;
        plot_thread.setRunning(false);
        x = 0;
        while(x<width){
            if(x<(data1.length)){
                //ch1_data[x] = data1[x];
                ch1_data[x] = height-data1[x]+1;
            }else{
                ch1_data[x] = ch1_pos;
            }
            x++;
        }


        plot_thread.setRunning(true);
    }

    public void setTimeBase(int val)
    {
        time_setting = val;
    }

    public void PlotPoints(Canvas canvas){
    try {
        // clear screen
        canvas.drawColor(Color.rgb(20, 20, 20));

        // draw grids
        for (int vertical = 1; vertical < 10; vertical++) {
            canvas.drawLine(
                    vertical * (width / 10) + 1, 1,
                    vertical * (width / 10) + 1, height + 1,
                    grid_paint);
        }
        for (int horizontal = 1; horizontal < 10; horizontal++) {
            canvas.drawLine(
                    1, horizontal * (height / 10) + 1,
                    width + 1, horizontal * (height / 10) + 1,
                    grid_paint);
        }

        // draw center cross
        canvas.drawLine(0, (height / 2) + 1, width + 1, (height / 2) + 1, cross_paint);
        canvas.drawLine((width / 2) + 1, 0, (width / 2) + 1, height + 1, cross_paint);

        // draw outline
        canvas.drawLine(0, 0, (width + 1), 0, outline_paint);    // top
        canvas.drawLine((width + 1), 0, (width + 1), (height + 1), outline_paint); //right
        canvas.drawLine(0, (height + 1), (width + 1), (height + 1), outline_paint); // bottom
        canvas.drawLine(0, 0, 0, (height + 1), outline_paint); //left

        // plot data
        for (int x = 0; x < (width - 1); x += time_setting) {
            canvas.drawLine(x + 1, ch1_data[x], x + 2, ch1_data[x + 1], ch1_color);
        }
    }
    catch (NullPointerException e){}
    }

}

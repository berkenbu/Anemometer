package com.printed.anemometer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import com.printed.anemometer.FFT;
import static java.lang.Thread.sleep;

public class MainActivity extends ActionBarActivity {

    private SensorManager mSensorManager;
    private MyView myView;

    private SensorThread thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        myView = new MyView(this);
        setContentView(myView);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        thread = new SensorThread(myView, mSensorManager);
        thread.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    static class SensorThread extends Thread implements SensorEventListener {
        private final double SMOOTHING = 0.75;
        private final int bufN = 5;
        private double[] sensorBuf = new double[bufN];
        private MyView mView;
        private SensorManager mSensorManager;
        private Sensor mMagnetometer;
        private double mCurrMagX, mCurrMagY, mCurrMagZ;
        private int N = 256;
//        private java.util.LinkedList<Double> rpmBuf = new java.util.LinkedList();
        private double[] rpmBuf = new double[N];
        private double[] mMagX = new double[N];
        private double[] mMagY = new double[N];
        private double[] mMagZ = new double[N];
        private double[] mPowerSpec = new double[N];
        private int currN = 0;
        private int rpmBufPos = 0;
        private double maxF = 0;
        private double mLastSign = 1.0;
        private long mLastMillis;
        private int bufPos = 0;

        private FFT fft;

        public SensorThread (MyView v, SensorManager sm) {
            mView = v;
            mSensorManager = sm;
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_FASTEST);
            fft = new FFT(N);
            mLastMillis = System.currentTimeMillis();
        }

        public static double [] winSmooth(double x[], int n, int wlen) {
            if (wlen > 0) {
                double[] res = new double[n];
                for (int i = 0; i < n; i++) {
                    int c = 0;
                    double r = 0.0;
                    for (int j = (i - wlen > 0 ? i - wlen : 0); j < (i + wlen < n ? i + wlen : n); j++) {
                        r += x[j];
                        c++;
                    }
                    res[i] = r / (float) c;
                }
                return res;
            }
            else return x;
        }

        public static int maxIndex(double[] a, int n) {
            int mi = 0;
            for (int i=1; i<n; i++)
                if (a[i] > a[mi]) mi = i;
            return mi;
        }

        public static double[] sumOfSquares (double x[], double y[], int n) {
            double[] ret = new double[n];
            for (int i=0; i<n; i++)
                ret[i] = x[i]*x[i] + y[i]*y[i];
            return ret;
        }

        private void plotSpectrum(Canvas c, int x, int y, int w, int h) {
            if (mPowerSpec!=null) {
                Paint p = new Paint();
                p.setColor(Color.RED);
                p.setStyle(Paint.Style.FILL);
                double maxS = mPowerSpec[maxIndex(mPowerSpec, N)];
                for (int i = 0; i < N; i++) {
                    c.drawRect((float)(x+w*i/N), (float)(y+h*(1-mPowerSpec[i]/maxS)), (float)(x+w*(i+1)/N), y+h, p);
                }
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Color.BLACK);
                c.drawRect(x, y, x + w, y + h, p);
            }
        }

        private void minMax (double x[], int n, double min, double max) {
            min = 1e40;
            max = -1e40;
            for (int i=0; i<n; ++i) {
                if (x[i] < min) min = x[i];
                if (x[i] > max) max = x[i];
            }
        }

        private double absMax (double x[], int n) {
            double max = 0;
            for (int i=0; i<n; ++i) {
                if (Math.abs(x[i]) > max) max = Math.abs(x[i]);
            }
            return max;
        }

        private void plotTS(Canvas c, int x, int y, int w, int h) {
            if (mMagX!=null) { // should really test all three, they all get filled at the same time
                Paint p = new Paint();
                double max = 2*Math.max(Math.max(absMax(mMagX, N), absMax(mMagY, N)), absMax(mMagZ, N));
                p.setStyle(Paint.Style.FILL);
                float ds = 2;
                for (int i=0; i<N; ++i) {
                    float xs = (float)x+w*i/N;
                    p.setColor(Color.RED);
                    c.drawRect(xs, (float)(y+h*(0.5-mMagX[i]/max)-ds/2), xs+ds, (float)(y+h*(0.5-mMagX[i]/max)+ds/2), p);
                    p.setColor(Color.GREEN);
                    c.drawRect(xs, (float)(y+h*(0.5-mMagY[i]/max)-ds/2), xs+ds, (float)(y+h*(0.5-mMagY[i]/max)+ds/2), p);
                    p.setColor(Color.BLUE);
                    c.drawRect(xs, (float)(y+h*(0.5-mMagZ[i]/max)-ds/2), xs+ds, (float)(y+h*(0.5-mMagZ[i]/max)+ds/2), p);
                }
                p.setStyle(Paint.Style.STROKE);
                p.setColor(Color.BLACK);
                c.drawRect(x, y, x + w, y + h, p);
            }
        }

        private void plotRpmTS(Canvas c, int x, int y, int w, int h) {
            Paint p = new Paint();
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.RED);
//            Double maxv = java.util.Collections.max(rpmBuf);
//            max = Math.pow(10, Math.ceil(Math.log10(max)));
//            double maxv = 800.0;
            double maxv = absMax(rpmBuf, N);
            maxv = Math.ceil(maxv/Math.pow(10, Math.floor(Math.log10(maxv))))*Math.pow(10, Math.floor(Math.log10(maxv)));
            String txt = String.format("%d", (int)maxv);
            double [] sBuf = new double[N];
            double sum = 0;
            for (int i=0; i<N; ++i) {
                sBuf[i] = rpmBuf[(i+rpmBufPos)%N];
                sum += sBuf[i];
            }
            sBuf = winSmooth(sBuf, N, 3);
//            float ds = (float)w/N;
//            for (int i=0; i<N; ++i) {
//                float xs = (float)x+w*i/N;
//                c.drawRect(xs, (float)(y+h*(1-rpmBuf[(i+rpmBufPos)%N]/maxv)), xs+ds, (float)(y+h*(1-rpmBuf[(i+rpmBufPos)%N]/maxv)+2), p);
//            }
            p.setStrokeWidth(4.0f);
            for (int i=0; i<N-1; i++) {
                c.drawLine((float)(x+w*i/N), (float)(y+h*(1-sBuf[i]/maxv)), (float)(x+w*(i+1)/N), (float)(y+h*(1-sBuf[i+1]/maxv)), p);
            }
            p.setTextSize(32);
            c.drawText(txt, x+1, y+33, p);
            txt = String.format("Avg: %.1f", sum/N);
            c.drawText(txt, x+w/2, y+33, p);
            p.setStrokeWidth(2.0f);
            p.setStyle(Paint.Style.STROKE);
            p.setColor(Color.BLACK);
            c.drawRect(x, y, x + w, y + h, p);
        }

        @Override
        public void run() {
            while(true) {
//                if (currN < N) {
//                    mMagX[currN] = mCurrMagX;
//                    mMagY[currN] = mCurrMagY;
//                    mMagZ[currN] = mCurrMagZ;
//                    currN++;
//                }
//                else { // do fft
 //                       double[] mMag_im = new double[N];
 //                       java.util.Arrays.fill(mMag_im, 0.0);
//                    double [] mMagSpecX = mMagX.clone();
//                    double [] mMagSpecY = mMagY.clone();
//                    double [] mMagSpecZ = mMagZ.clone();
//                    fft.fft(mMagSpecZ, mMagSpecY);
//                    mPowerSpec = winSmooth(sumOfSquares(mMagSpecZ, mMagSpecY, N), N, 0);
//                    //maxF = maxIndex(mPowerSpec, N/2);
//                    currN = 0;
//                }
                rpmBuf[rpmBufPos++] = 60.0*maxF;
                if (rpmBufPos>=N) rpmBufPos = 0;
                mView.setPlotsPending(true);
                try {
//                    sleep(4); // 256 measurements per second
                    sleep(50);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_MAGNETIC_FIELD)
                return;
            mCurrMagX = SMOOTHING*mCurrMagX + (1-SMOOTHING)*event.values[0];
            mCurrMagY = SMOOTHING*mCurrMagY + (1-SMOOTHING)*event.values[1];
            mCurrMagZ = SMOOTHING*mCurrMagZ + (1-SMOOTHING)*event.values[2];
            if (mLastSign != Math.signum(mCurrMagZ) || System.currentTimeMillis()-mLastMillis>500.0/maxF) {
                maxF = 500.0/(System.currentTimeMillis() - mLastMillis);
                if (mLastSign!=Math.signum(mCurrMagZ)) {
                    mLastMillis = System.currentTimeMillis();
                    mLastSign = Math.signum(mCurrMagZ);
                }
                if (maxF<0.25) maxF = 0;
                sensorBuf[bufPos++] = maxF;
                if (bufPos>=bufN) bufPos = 0;
                double[] sortedBuf = sensorBuf.clone();
                java.util.Arrays.sort(sortedBuf);
                maxF = sortedBuf[bufN/2];
            }
            String s = String.format("%.3f, %.3f, %.3f", event.values[0], event.values[1], event.values[2]);
            mView.setText(s, 0);
            s = String.format("RPM: %.1f", (double)60*maxF);
            mView.setText(s, 1);
            mView.postInvalidate();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    }

    public class MyView extends View {
        private String[] mOutText = new String[2];
        private double[] mSpectrum;
        private int mSpecN;
        private boolean mPlotsPending = true;

        public MyView(Context context) {
            super(context);
            mOutText[0] = "";
            mOutText[1] = "";
            // TODO Auto-generated constructor stub
        }

        public void setText(String s, int n) {
            mOutText[n] = s;
        }

        public void setPlotsPending(boolean b) {
            mPlotsPending = b;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            // TODO Auto-generated method stub
            super.onDraw(canvas);
            int x = getWidth();
            int y = getHeight();
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.WHITE);
//            canvas.drawPaint(paint);
            canvas.drawRect(0, 0, x, (int)(y*0.4-1), paint);
            Paint tpaint = new Paint();
            tpaint.setARGB(255,0, 0, 0);
            tpaint.setAntiAlias(true);
            tpaint.setTextSize(24);
            canvas.drawText(mOutText[0], 1, (float) (0.05 * y), tpaint);
            tpaint.setTextSize(80);
            canvas.drawText(mOutText[1], 1, (float)(0.4*y), tpaint);

            if (mPlotsPending || true) { // this is not quite working - canvas always gets cleared?
                canvas.drawRect(0, (int)(y*0.45), x, y, paint);
                thread.plotRpmTS(canvas, (int)(x*0.1), (int)(0.45*y), (int)(x*0.8), (int)(y*0.4));
//                thread.plotSpectrum(canvas, (int) (x * 0.05), (int) (0.65 * y), (int) (x * 0.425), (int) (y * 0.3));
//                thread.plotTS(canvas, (int) (x * 0.525), (int) (0.65 * y), (int) (x * 0.425), (int) (y * 0.3));
                mPlotsPending = false;
            }
        }
    }
}

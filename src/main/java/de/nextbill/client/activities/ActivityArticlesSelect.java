/*
 * NextBill Android client application
 *
 * @author Michael Roedel
 * Copyright (c) 2020 Michael Roedel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.nextbill.client.activities;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.nextbill.client.R;
import de.nextbill.client.breceiver.RequestListener;
import de.nextbill.client.breceiver.RequestServiceBroadcastReceiver;
import de.nextbill.client.database.IOHelper;
import de.nextbill.client.database.StatusDatabaseHandler;
import de.nextbill.client.enums.BroadcastMessageActionEnum;
import de.nextbill.client.enums.CorrectionStatus;
import de.nextbill.client.enums.InvoiceStatusEnum;
import de.nextbill.client.enums.LoginStatusEnum;
import de.nextbill.client.enums.OnlineStatusEnum;
import de.nextbill.client.helper.CostDistributionHelper;
import de.nextbill.client.interfaces.CurrencyTextWatcher;
import de.nextbill.client.interfaces.IPaymentItem;
import de.nextbill.client.model.ArticleDTO;
import de.nextbill.client.network.RequestManager;
import de.nextbill.client.network.RequestUpdateService;
import de.nextbill.client.utils.ArticleAnalysisData;

public class ActivityArticlesSelect extends AppCompatActivity {

    private TextView usrNameTv;
    private TextView sumTv;
    private Button submitBtn;
    private ImageView imageView;

    private RequestServiceBroadcastReceiver updateRequestReceiver;

    String payer;
    String paymentItemId;
    String paymentItemClazz;
    String invoiceId;
    List<ArticleDTO> articleDTOS = new ArrayList<>();

    BigDecimal sum = new BigDecimal(0);

    private static final String TAG = "ActivityArticlesSelect";
    @SuppressWarnings("unused")
    private static final float MIN_ZOOM = 1f,MAX_ZOOM = 1f;

    // These matrices will be used to scale points of the image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    Matrix resetMatrix = new Matrix();

    // The 3 states (events) which the user is trying to perform
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;

    // these PointF objects are used to record the point(s) the user is touching
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    long msLastMouseDown = 0;

    float imageWidth = 0;
    float imageHeight = 0;

    MenuItem closeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_select);

        usrNameTv = (TextView) findViewById(R.id.usrNameTv);
        sumTv = (TextView) findViewById(R.id.sumTv);
        submitBtn = (Button) findViewById(R.id.submitBtn);
        imageView = (ImageView) findViewById(R.id.imageView16);

        this.payer = getIntent().getStringExtra("payer");
        this.paymentItemId = getIntent().getStringExtra("paymentItemId");
        this.paymentItemClazz = getIntent().getStringExtra("paymentItemClazz");
        this.invoiceId = getIntent().getStringExtra("invoiceId");

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();
        Type listType = new TypeToken<List<ArticleDTO>>() {}.getType();

        List<ArticleDTO> articleDTOStmp = gson.fromJson(getIntent().getStringExtra("articleDTOS"), listType);
        this.articleDTOS = articleDTOStmp != null ? articleDTOStmp : new ArrayList<ArticleDTO>();
        for (ArticleDTO articleDTO : articleDTOS) {
            sum = sum.add(articleDTO.getPrice());
        }

        updateRequestReceiver = new RequestServiceBroadcastReceiver(new RequestListener() {

            @Override
            public void requestDone(String activityClassName) { }

            @Override
            public void generalMessage(String activityClassName, BroadcastMessageActionEnum broadcastMessageActionEnum, String message) {
                if (activityClassName.equals(ActivityArticlesSelect.class.getSimpleName())){
                    if (BroadcastMessageActionEnum.READY.equals(broadcastMessageActionEnum)) {

                        Type listType = new TypeToken<ArticleDTO>() {
                        }.getType();

                        GsonBuilder builder = new GsonBuilder();

                        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
                            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                                return new Date(json.getAsJsonPrimitive().getAsLong());
                            }
                        });

                        Gson gson = builder.create();

                        ArticleDTO articleDTO = gson.fromJson(message, listType);

                        if (articleDTO.getName() != null && articleDTO.getPrice() != null) {
                            articleDTOS.add(articleDTO);

                            sum = sum.add(articleDTO.getPrice());

                            drawArticles();
                        }

                        refreshViews();
                    }else if (BroadcastMessageActionEnum.ERROR.equals(broadcastMessageActionEnum)) {
                        Toast.makeText(getApplicationContext(), "Keine Summe erkannt! Bitte erneut versuchen.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void loginMessage(LoginStatusEnum loginStatusEnum) {

            }

            @Override
            public void onlineStatusMessage(OnlineStatusEnum onlineStatusEnum) {

            }

        });

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                GsonBuilder builder = new GsonBuilder();
                Gson gson = builder.create();

                Type listType = new TypeToken<List<ArticleDTO>>() {}.getType();

                String articlesAsJson = gson.toJson(articleDTOS, listType);

                Intent in = getIntent();
                in.putExtra("sum", sum.setScale(2, RoundingMode.HALF_EVEN).doubleValue());
                in.putExtra("paymentItemClass", paymentItemClazz);
                in.putExtra("paymentItemId", paymentItemId);
                in.putExtra("articleDTOS", articlesAsJson);
                setResult(RESULT_OK, in);
                finish();
                overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {

            Matrix inverse = new Matrix();

            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                imageView.setScaleType(ImageView.ScaleType.MATRIX);
                float scale;

                switch (event.getAction() & MotionEvent.ACTION_MASK)
                {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        Log.d(TAG, "mode=DRAG"); // write to LogCat
                        msLastMouseDown = new Date().getTime();
                        resetMatrix.set(matrix);
                        mode = DRAG;
                        break;

                    case MotionEvent.ACTION_UP:
                        if (new Date().getTime() - msLastMouseDown <= 200){

                            matrix.set(resetMatrix);

                            imageView.getImageMatrix().invert(inverse);
                            float[] pts = {event.getX(), event.getY()};
                            inverse.mapPoints(pts);

                            double x = Math.floor(pts[0]);
                            double y = Math.floor(pts[1]);

                            BigDecimal xValue = BigDecimal.valueOf(x);
                            BigDecimal yValue = BigDecimal.valueOf(y);

                            ArticleDTO articleDTOfound = isTouchOverlappingOnArticle(xValue, yValue);
                            if (articleDTOfound != null){
                                sum = sum.subtract(articleDTOfound.getPrice());

                                articleDTOS.remove(articleDTOfound);

                                drawArticles();
                                refreshViews();
                            }else{
                                analyzeArticle(x,y);
                            }
                        }
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        Log.d(TAG, "mode=NONE");
                        break;

                    case MotionEvent.ACTION_POINTER_DOWN:

                        oldDist = spacing(event);
                        Log.d(TAG, "oldDist=" + oldDist);
                        if (oldDist > 5f) {
                            savedMatrix.set(matrix);
                            midPoint(mid, event);
                            mode = ZOOM;
                            Log.d(TAG, "mode=ZOOM");
                        }
                        break;

                    case MotionEvent.ACTION_MOVE:

                        if (mode == DRAG){
                            matrix.set(savedMatrix);

                            float moveX = event.getX() - start.x;
                            float moveY = event.getY() - start.y;

                            matrix.postTranslate(moveX, moveY);
                        }else if (mode == ZOOM) {
                            float newDist = spacing(event);
                            Log.d(TAG, "newDist=" + newDist);
                            if (newDist > 5f){
                                matrix.set(savedMatrix);
                                scale = newDist / oldDist;

                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }
                        }

                        break;
                }

                imageView.setImageMatrix(matrix);

                return true;
            }

            private float spacing(MotionEvent event)
            {
                float x = event.getX(0) - event.getX(1);
                float y = event.getY(0) - event.getY(1);
                return (float) Math.sqrt(x * x + y * y);
            }

            private void midPoint(PointF point, MotionEvent event){
                float x = event.getX(0) + event.getX(1);
                float y = event.getY(0) + event.getY(1);
                point.set(x / 2, y / 2);
            }
        });

        refreshViews();
        refreshImageView();

        drawArticles();

        Drawable drawable = ContextCompat.getDrawable(getApplicationContext(), R.drawable.baseline_save_white_36);
        getSupportActionBar().setHomeAsUpIndicator(drawable);
    }

    public void refreshViews() {
        usrNameTv.setText("Transaktionspartner: " + payer);
        sumTv.setText("Summe: " + CurrencyTextWatcher.bigDecimalToString(sum.setScale(2, RoundingMode.HALF_EVEN)) + " €");
    }

    public ArticleDTO isTouchOverlappingOnArticle(BigDecimal xValue, BigDecimal yValue){
        BigDecimal widthImg = BigDecimal.valueOf(imageWidth);
        BigDecimal heightImg = BigDecimal.valueOf(imageHeight);

        ArticleDTO articleDTOfound = null;
        for (ArticleDTO articleDTO : articleDTOS) {

            BigDecimal startXtmp = articleDTO.getStartX().multiply(widthImg);
            BigDecimal endXtmp = articleDTO.getEndX().multiply(widthImg);
            BigDecimal startYtmp = articleDTO.getStartY().multiply(heightImg);
            BigDecimal endYtmp = articleDTO.getEndY().multiply(heightImg);

            BigDecimal overlapping = percentOfOverlapping(xValue, xValue.add(new BigDecimal(1)), yValue, yValue.add(new BigDecimal(1)), startXtmp, endXtmp, startYtmp, endYtmp);

            if (overlapping.compareTo(new BigDecimal(0)) > 0){
                articleDTOfound = articleDTO;
                break;
            }
        }

        return articleDTOfound;
    }

    @Override
    public void onResume() {
        super.onResume();
        IOHelper.getInstance().setCtx(this.getApplicationContext());

        IntentFilter filter = new IntentFilter();
        filter.addAction(RequestManager.BC_SEND_MESSAGE);
        registerReceiver(updateRequestReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(updateRequestReceiver);
    }

    @Override
    public void onBackPressed(){
        Intent in = getIntent();
        setResult(RESULT_CANCELED, in);
        finish();
        overridePendingTransition(R.anim.activity_out1, R.anim.activity_out2);
    }

    @SuppressLint("NewApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.close, menu);
        closeButton = menu.findItem(R.id.closeButton);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            default:
                onBackPressed();
        }

        return true;
    }

    private void analyzeArticle(Double xIn, Double yIn) {

        double x = BigDecimal.valueOf(xIn).divide(BigDecimal.valueOf(imageWidth), 8, RoundingMode.HALF_EVEN).setScale(8, RoundingMode.HALF_EVEN).doubleValue();
        double y = BigDecimal.valueOf(yIn).divide(BigDecimal.valueOf(imageHeight), 8, RoundingMode.HALF_EVEN).setScale(8, RoundingMode.HALF_EVEN).doubleValue();

        Log.d(TAG, "x=" + x);
        Log.d(TAG, "y=" + y);

        ArticleAnalysisData articleAnalysisData = new ArticleAnalysisData();
        articleAnalysisData.setInvoiceId(invoiceId);
        articleAnalysisData.setX(x);
        articleAnalysisData.setY(y);

        StatusDatabaseHandler handler = StatusDatabaseHandler.getInstance(getApplicationContext());
        Gson gson = new GsonBuilder()
                .setDateFormat("dd MMM yyyy HH:mm:ss").create();
        String jsonString = gson.toJson(articleAnalysisData);

        handler.addObject(jsonString, StatusDatabaseHandler.OBJECT_TYPE_INVOICE_IMAGE, StatusDatabaseHandler.UPDATE_STATUS_GET_VALUE, new Date().getTime(), 1);

        Intent sIntent = new Intent(getApplicationContext(), RequestUpdateService.class);
        sIntent.putExtra(RequestUpdateService.KEY_TYPE, RequestUpdateService.REQUEST_UPDATE_PENDING);
        getApplicationContext().startService(sIntent);
    }

    public void refreshImageView(){
        File file = imageExists();
        Bitmap myImg = BitmapFactory.decodeFile(file.getAbsolutePath());

        if (myImg != null){
            Matrix matrix = new Matrix();

            matrix.setScale(1.0f, 1.0f);

            Bitmap rotated = Bitmap.createBitmap(myImg, 0, 0, myImg.getWidth(), myImg.getHeight(),
                    matrix, true);

            imageView.setVisibility(View.VISIBLE);

            imageView.setImageBitmap(rotated);

            imageWidth = myImg.getWidth();
            imageHeight = myImg.getHeight();
        }
    }

    public void drawArticles(){
        File file = imageExists();
        Bitmap myBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());

        if (myBitmap != null){
            Bitmap tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
            Canvas tempCanvas = new Canvas(tempBitmap);

//            Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
//            backgroundPaint.setStyle(Paint.Style.FILL);
//            backgroundPaint.setColor(Color.LTGRAY);
//            tempCanvas.drawPaint(backgroundPaint);

// Paint it white (or whatever color you want)
//            tempCanvas.drawColor(Color.BLACK);

            tempCanvas.drawBitmap(myBitmap, 0, 0, null);

            Paint piePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            piePaint.setStyle(Paint.Style.FILL);
            piePaint.setAlpha(80);

            BigDecimal widthImg = BigDecimal.valueOf(myBitmap.getWidth());
            BigDecimal heightImg = BigDecimal.valueOf(myBitmap.getHeight());

            for (ArticleDTO articleDTO : articleDTOS) {

                BigDecimal startXtmp = articleDTO.getStartX().multiply(widthImg);
                BigDecimal endXtmp = articleDTO.getEndX().multiply(widthImg);
                BigDecimal startYtmp = articleDTO.getStartY().multiply(heightImg);
                BigDecimal endYtmp = articleDTO.getEndY().multiply(heightImg);

                tempCanvas.drawRoundRect(new RectF(startXtmp.floatValue(),startYtmp.floatValue(),endXtmp.floatValue(),endYtmp.floatValue()), 2, 2, piePaint);
            }

            imageView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
        }
    }

    public BigDecimal percentOfOverlapping(BigDecimal rectangle1StartX, BigDecimal rectangle1EndX, BigDecimal rectangle1StartY, BigDecimal rectangle1EndY, BigDecimal rectangle2StartX, BigDecimal rectangle2EndX, BigDecimal rectangle2StartY, BigDecimal rectangle2EndY){

        BigDecimal valueOverlapping = maxValue(new BigDecimal(0), minValue(rectangle1EndX, rectangle2EndX).subtract(maxValue(rectangle1StartX, rectangle2StartX)))
                .multiply(maxValue(new BigDecimal(0), minValue(rectangle1EndY, rectangle2EndY).subtract(maxValue(rectangle1StartY, rectangle2StartY))));

        BigDecimal totalOfRectangle1 = rectangle1EndX.subtract(rectangle1StartX).multiply(rectangle1EndY.subtract(rectangle1StartY));
        BigDecimal totalOfRectangle2 = rectangle2EndX.subtract(rectangle2StartX).multiply(rectangle2EndY.subtract(rectangle2StartY));

        BigDecimal areOfUnion = totalOfRectangle1.add(totalOfRectangle2).subtract(valueOverlapping);

        if (areOfUnion.compareTo(new BigDecimal(0)) == 0){
            return new BigDecimal(1);
        }

        return valueOverlapping.divide(areOfUnion, 10, RoundingMode.HALF_EVEN);
    }

    private BigDecimal minValue(BigDecimal value1, BigDecimal value2){
        if (value1.compareTo(value2) == -1){
            return value1;
        }
        return value2;
    }

    private BigDecimal maxValue(BigDecimal value1, BigDecimal value2){
        if (value1.compareTo(value2) == 1){
            return value1;
        }
        return value2;
    }

    public File imageExists(){
        if (invoiceId == null){
            return null;
        }

        IOHelper ioHelper = IOHelper.getInstance();
        ioHelper.setCtx(getApplicationContext());

        String pathTmp = ioHelper.getImageDirectory();
        File file = new File(pathTmp + "/" +invoiceId + "_thumbnail");

        if (file.exists()){
            return file;
        }

        return null;
    }
}

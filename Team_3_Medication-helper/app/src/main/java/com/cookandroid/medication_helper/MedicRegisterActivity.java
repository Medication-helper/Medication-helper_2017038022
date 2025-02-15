/****************************
 MedicRegisterActivity.java
 작성 팀 : [02-03]
 프로그램명 : Medication Helper
 설명 : 환자 - 복약 등록 화면
 ***************************/

package com.cookandroid.medication_helper;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ButtonBarLayout;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MedicRegisterActivity extends AppCompatActivity {
    Bitmap bitmap;
    Bitmap rotatedbitmap;

    PreviewView previewView;
    Button btnStartCamera;
    Button btnCaptureCamera;
    Button btnOcr;
    Button btnRegister;
    TextView textView;
    ImageView picture;
    View overlay;

    ProcessCameraProvider processCameraProvider;
    int lensFacing = CameraSelector.LENS_FACING_BACK;
    ImageCapture imageCapture;

    private TextRecognizer textRecognizer;//define TextRecognizer

    String data;

    String ocrResult;

    UserData userData;
    FirebaseStorage storage = FirebaseStorage.getInstance();

    String imgName="medicImage.jpg";

    long mNow;
    Date mDate;
    SimpleDateFormat mFormat = new SimpleDateFormat("yyyyMMddhhmm");

    public class addMedicine {
        public String cName, mIMG, mEffect;

        public addMedicine() {} //setValue를 사용하기 위해 필요, 없으면 작동하지 않음

        public addMedicine(String cName, String mIMG, String mEffect) {
            this.cName = cName; // 제조사 이름
            this.mIMG = mIMG; // 약 이미지로 연결되는 URL
            this.mEffect = mEffect; // 약 효능
        }
    }

    @Override
    public void onBackPressed() { // 하단의 뒤로가기(◀) 버튼을 눌렀을 시 동작
        //다이어로그를 화면에 나타냄
        AlertDialog.Builder exitDialogBuilder = new AlertDialog.Builder(MedicRegisterActivity.this);
        exitDialogBuilder
                .setTitle("프로그램 종료")
                .setMessage("종료하시겠습니까?")
                .setCancelable(false)
                .setPositiveButton("네",
                        //네를 누르면 앱 종료
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                int pid = android.os.Process.myPid();
                                android.os.Process.killProcess(pid);
                                finish();
                            }
                        })
                //아니오 누르면 다이어로그를 종료
                .setNegativeButton("아니오",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
        AlertDialog exitDialog = exitDialogBuilder.create();
        exitDialog.show();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_medicregister);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // 기본 타이틀 사용 안함
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM); // 커스텀 사용
        getSupportActionBar().setCustomView(R.layout.mediregititlebar_custom); // 커스텀 사용할 파일 위치

        SharedPreferences sharedPreferences=getSharedPreferences("PREF", Context.MODE_PRIVATE);
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child("pictures.jpg"); // Firebase Storage에 저장되는 사진의 위치 및 이름(현재는 이름이 고정되어있음.)

        userData = (UserData) getApplicationContext();
        previewView = (PreviewView) findViewById(R.id.previewView);
        btnStartCamera = (Button) findViewById(R.id.btnCameraStart);
        btnCaptureCamera = (Button) findViewById(R.id.btnPicture);
        btnRegister = (Button) findViewById(R.id.regimedicbtn);
        textView = (TextView) findViewById(R.id.OCRTextResult);
        picture = (ImageView) findViewById(R.id.imageview);
        overlay = (View) findViewById(R.id.overlay);

        textRecognizer= TextRecognition.getClient(new KoreanTextRecognizerOptions.Builder().build());

        //카메라 촬영을 위한 동의 얻기
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        try {
            processCameraProvider = processCameraProvider.getInstance(this).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //카메라 프리뷰 작동
        btnStartCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (ActivityCompat.checkSelfPermission(MedicRegisterActivity.this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    previewView.setVisibility(View.VISIBLE);
                    textView.setVisibility(View.INVISIBLE);
                    btnStartCamera.setVisibility(View.INVISIBLE);
                    btnStartCamera.setEnabled(false);
                    btnCaptureCamera.setVisibility(View.VISIBLE);
                    btnCaptureCamera.setEnabled(true);

                    bindPreview();
                    bindImageCapture();

                }
            }
        });

        //카메라에 접근해 사진 찍는 버튼
        btnCaptureCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                imageCapture.takePicture(ContextCompat.getMainExecutor(MedicRegisterActivity.this),
                        new ImageCapture.OnImageCapturedCallback() {
                            @Override
                            public void onCaptureSuccess(@NonNull ImageProxy image) {

                                @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
                                Image mediaImage = image.getImage();

                                //카메라에서 가져온 이미지를 비트맵 이미지로 변환
                                bitmap = com.cookandroid.medication_helper.ImageUtil.mediaImageToBitmap(mediaImage);

                                Log.d("result", Integer.toString(bitmap.getWidth())); //4032
                                Log.d("result", Integer.toString(bitmap.getHeight())); //3024
                                Log.d("result", Integer.toString(image.getImageInfo().getRotationDegrees()));

                                //이미지 회전(최종상태)
                                rotatedbitmap = com.cookandroid.medication_helper.ImageUtil.rotateBitmap(bitmap, image.getImageInfo().getRotationDegrees());

                                //최종적인 이미지는 3:4 비율이다
                                Log.d("result", Integer.toString(rotatedbitmap.getWidth())); //3024
                                Log.d("result", Integer.toString(rotatedbitmap.getHeight())); //4032
                                Log.d("result", Integer.toString(image.getImageInfo().getRotationDegrees()));

                                //가로, 세로 자르기
                                //시작지점은 가로 세로 각 1/8지점이다.
                                //가로로는 6/8, 세로로는 4/8만 남겨보자
                                Bitmap cutImage=Bitmap.createBitmap(rotatedbitmap,378,1008,2268,2016);

                                processCameraProvider.unbindAll();//카메라 프리뷰 중단
                                previewView.setVisibility(View.INVISIBLE);

                                super.onCaptureSuccess(image);

                                int height = rotatedbitmap.getHeight();
                                int width = rotatedbitmap.getWidth();

                                //AlertDialog에 사용할 비트맵 이미지의 사이즈를 가로세로 비율 맞춰서 축소한다.
                                Bitmap popupBitmap = Bitmap.createScaledBitmap(cutImage, 900, height / (width / 900), true);

                                Bitmap popupBitmap2= resizeBitmap(cutImage);

                                Log.d("result", Integer.toString(cutImage.getWidth())); //3096
                                Log.d("result", Integer.toString(cutImage.getHeight())); //4128
                                Log.d("result", Integer.toString(image.getImageInfo().getRotationDegrees()));

                                //카메라 바인딩 사용중단
                                processCameraProvider.unbindAll();

                                Bitmap gray=grayScale(popupBitmap);//사진 GrayScale
                                Bitmap binary=GetBinaryBitmap(gray);//이진화(내가 보기엔 버려야 할 것 같음)

                                ImageView capturedimage = new ImageView(MedicRegisterActivity.this);
                                capturedimage.setImageBitmap(popupBitmap2);

                                //사진 촬영 결과를 AlertDialog로 띄워 사용 여부를 선택한다
                                AlertDialog.Builder captureComplete = new AlertDialog.Builder(MedicRegisterActivity.this)
                                        .setTitle("촬영 결과")
                                        .setMessage("이 사진을 사용할까요?")
                                        .setView(capturedimage)
                                        //사용을 선택할 경우 OCR 실행
                                        .setPositiveButton("사용", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                                btnRegister.setEnabled(true);
                                                btnCaptureCamera.setEnabled(false);

                                                //MLkit OCR
                                                InputImage image=InputImage.fromBitmap(popupBitmap2,0);//MLKit에서 사용하기 위해서 비트맵에서 InputImage로 변환

                                                //Recognize Text
                                                textView.setTextSize(20);
                                                Task<Text> result = textRecognizer.process(image)
                                                        .addOnSuccessListener(new OnSuccessListener<Text>() {
                                                            @Override
                                                            public void onSuccess(Text text) {
                                                                //textView.setText(text.getText());
                                                                textView.setVisibility(View.VISIBLE);

                                                                ocrResult=text.getText().toString();
                                                                String replaceStr=ocrResult.replaceAll(" ","");
                                                                textView.setText(replaceStr);

                                                            }
                                                        })
                                                        .addOnFailureListener(new OnFailureListener() {
                                                            @Override
                                                            public void onFailure(@NonNull Exception e) {

                                                            }
                                                        });


                                                //사진 촬영용 오버레이 감추기
                                                overlay.setVisibility(View.INVISIBLE);
                                            }
                                        })
                                        //재촬영을 선택할 경우 bitmap에 저장된 비트맵 파일을 지우고 다시 카메라 프리뷰를 바인딩함
                                        .setNegativeButton("재촬영", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                bitmap = null;
                                                bindPreview();
                                                bindImageCapture();
                                                textView.setVisibility(View.INVISIBLE);

                                                previewView.setVisibility(View.VISIBLE);
                                            }
                                        });

                                captureComplete.setCancelable(false);

                                captureComplete.create().show();
                            }
                        });
            }
        });


        /* 복약 등록 버튼 */
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"등록 중, 잠시만 기다려주세요...",Toast.LENGTH_SHORT).show();
                String replaceStr=ocrResult.replaceAll(" ","");//OCR 결과물에서 간혹 발생하는 띄어쓰기 공백 삭제

                String[] list=replaceStr.split("\n");

                for(String line:list){
                    System.out.println(line);
                }

                //약물 목록의 길이
                int listSize= list.length;
                System.out.println("약물 개수 : " + listSize);

                String [] dataResult=replaceStr.split("\n");

                String [][] medicInfoList = new String[listSize][4];

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        for(int i=0;i<listSize;i++){
                            data=getXmlData(dataResult[i]);//리스트에서 해당 약품의 정보 가져오기
                            System.out.println(data);

                            String [] dataSplit = data.split("\n");//줄바꿈 단위로 문자열 분할
                            System.out.println("dataSplit Size: "+dataSplit.length);

                            if(dataSplit.length==4) {

                                System.out.print("파싱 결과 : ");
                                System.out.println(dataResult[i]);
                                System.out.print(dataSplit[0] + " ");
                                System.out.print(dataSplit[1] + " ");
                                System.out.print(dataSplit[2] + " ");
                                System.out.println(dataSplit[3]);

                                //DB에 파싱 결과 저장
                                boolean contains = dataSplit[0].contains(dataResult[i]);

                                if (contains) {
                                    medicInfoList[i][0] = dataResult[i];
                                    medicInfoList[i][1] = dataSplit[1];
                                    medicInfoList[i][2] = dataSplit[2];
                                    medicInfoList[i][3] = dataSplit[3];

                                    DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference(); // Firebase와 연동
                                    addMedicine addMedicine = new addMedicine(dataSplit[1], dataSplit[2], dataSplit[3]); // Firebase에 저장할 클래스 설정

                                    rootRef.child("Medicine").child(userData.getUserID()).child(dataResult[i]).setValue(""); // 사용자가 복용 중인 약 DB에 저장
                                    rootRef.child("MedicineList").child(dataResult[i]).setValue(addMedicine); // 약품 목록 DB에 약 추가
                                }
                            }
                        }
                    }
                }).start();

                Toast.makeText(getApplicationContext(),"등록했습니다",Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), MainPageActivity.class)); // 등록이 완료된 후 메인화면으로 전환
                finish(); // Progress 완전 종료
            }
        });


        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setSelectedItemId(R.id.cameraNav);

        /* 바텀 네비게이션을 나타나게 해주는 함수 */
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    /* 지도 화면으로 전환 */
                    case R.id.homeNav:
                        startActivity(new Intent(getApplicationContext(), com.cookandroid.medication_helper.MainPageActivity.class));
                        overridePendingTransition(0, 0);
                        finish();
                        return true;
                    /* 현재 페이지에서 보여주는 액티비티 */
                    case R.id.cameraNav:
                        return true;
                    /* 복용 약 목록 화면으로 전환 */
                    case R.id.articleNav:
                        startActivity(new Intent(getApplicationContext(), MedicineListActivity.class));
                        overridePendingTransition(0, 0);
                        finish();
                        return true;
                    /* 마이페이지 화면으로 전환 */
                    case R.id.userNav:
                        startActivity(new Intent(getApplicationContext(), com.cookandroid.medication_helper.MyPageActivity.class));
                        overridePendingTransition(0, 0);
                        finish();
                        return true;
                }
                return false;
            }
        });
        overlay.bringToFront();
    }

    void bindPreview(){
        previewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        processCameraProvider.bindToLifecycle(this,cameraSelector,preview);
    }

    void bindImageCapture(){
        CameraSelector cameraSelector=new CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build();
        imageCapture=new ImageCapture.Builder()
                .build();

        processCameraProvider.bindToLifecycle(this,cameraSelector,imageCapture);
    }

    //회색조
    private Bitmap grayScale(final Bitmap orgBitmap){
        int width, height;
        width=orgBitmap.getWidth();
        height=orgBitmap.getHeight();

        Bitmap bmpGrayScale=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_4444);
        Canvas canvas=new Canvas(bmpGrayScale);
        Paint paint=new Paint();
        ColorMatrix colorMatrix=new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter colorMatrixColorFilter=new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorMatrixColorFilter);
        canvas.drawBitmap(orgBitmap,0,0,paint);
        return bmpGrayScale;
    }

    //이진화
    private Bitmap GetBinaryBitmap(Bitmap bitmap_src) {

        Bitmap bitmap_new=bitmap_src.copy(bitmap_src.getConfig(), true);

        for(int x=0; x<bitmap_new.getWidth(); x++) {

            for(int y=0; y<bitmap_new.getHeight(); y++) {

                int color=bitmap_new.getPixel(x, y);

                color=GetNewColor(color);

                bitmap_new.setPixel(x, y, color);

            }

        }

        return bitmap_new;

    }

    private int GetNewColor(int c) {

        double dwhite=GetColorDistance(c, Color.WHITE);

        double dblack=GetColorDistance(c,Color.BLACK)*0.4;

        if(dwhite<=dblack) {

            return Color.WHITE;

        }

        else {

            return Color.BLACK;

        }

    }

    private double GetColorDistance(int c1, int c2) {

        int db= Color.blue(c1)-Color.blue(c2);

        int dg=Color.green(c1)-Color.green(c2);

        int dr=Color.red(c1)-Color.red(c2);

        double d=Math.sqrt(  Math.pow(db, 2) + Math.pow(dg, 2) +Math.pow(dr, 2)  );

        return d;

    }

    //OCR 결과물 처리
    public void ReturnThreadResult(String result) {
        System.out.println("###  Return Thread Result");
        String translateText = "";

        String rlt = result;
        try {
            JSONObject jsonObject = new JSONObject(rlt);

            JSONArray jsonArray  = jsonObject.getJSONArray("images");

            for (int i = 0; i < jsonArray.length(); i++ ){

                JSONArray jsonArray_fields  = jsonArray.getJSONObject(i).getJSONArray("fields");

                for (int j=0; j < jsonArray_fields.length(); j++ ){

                    String inferText = jsonArray_fields.getJSONObject(j).getString("inferText");
                    translateText += inferText;
                    translateText += " ";
                }
            }


            textView.setText(translateText);

        } catch (Exception e){

        }
    }

    static public Bitmap resizeBitmap(Bitmap original) {

        int resizeWidth = 900;

        double aspectRatio = (double) original.getHeight() / (double) original.getWidth();
        int targetHeight = (int) (resizeWidth * aspectRatio);
        Bitmap result = Bitmap.createScaledBitmap(original, resizeWidth, targetHeight, false);
        if (result != original) {
            original.recycle();
        }
        return result;
    }

    //약 이름을 이용해 공공데이터 포털에서 약 정보 알아내기
    String getXmlData(String medicname) {
        StringBuffer buffer=new StringBuffer();
        String str=medicname;
        String MedicineName= URLEncoder.encode(str);
        System.out.println("약품명:"+MedicineName);


        String queryUrl="http://apis.data.go.kr/1471000/MdcinGrnIdntfcInfoService01/getMdcinGrnIdntfcInfoList01?serviceKey=RZnyfUGsOhY2tWWUv262AHpeMQYn4Idqd5cgG0rGNHPd648m5j0Pu3eiS3ewN4XhhHT%2FvuliAmF9KLJdzh1TFA%3D%3D&item_name="+MedicineName+"&pageNo=1&numOfRows=1&type=xml";
        try {
            URL url=new URL(queryUrl);
            InputStream is=url.openStream();

            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp=factory.newPullParser();
            xpp.setInput(new InputStreamReader(is,"UTF-8"));

            String tag;

            xpp.next();
            int eventType=xpp.getEventType();

            while(eventType != XmlPullParser.END_DOCUMENT){
                switch (eventType){

                    case XmlPullParser.START_TAG:
                        tag=xpp.getName();

                        if(tag.equals("item"));


                        //약품명
                        else if(tag.equals("ITEM_NAME")){
                            xpp.next();
                            buffer.append(xpp.getText());
                            buffer.append("\n");
                        }
                        //약품 회사명
                        else if(tag.equals("ENTP_NAME")){
                            xpp.next();
                            buffer.append(xpp.getText());
                            buffer.append("\n");
                        }
                        //약품 이미지 URL
                        else if(tag.equals("ITEM_IMAGE")){
                            xpp.next();
                            buffer.append(xpp.getText());
                            buffer.append("\n");
                        }
                        //약품 종류
                        else if(tag.equals("CLASS_NAME")){
                            xpp.next();
                            buffer.append(xpp.getText());
                        }

                        break;
                }
                eventType=xpp.next();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return buffer.toString();
    }
}

package com.zz.demo;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.opengl.ETC1;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.hai.mediapicker.util.GalleryFinal;
import com.sendtion.xrichtext.RichTextEditor;
import com.sendtion.xrichtext.SDCardUtil;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity implements ActionSheet.ActionSheetListener {

    @Bind(R.id.bt_main)
    Button btMain;
    @Bind(R.id.et_main)
    RichTextEditor etMain;

    private ProgressDialog insertDialog;
    public final static int REQUEST_TAKE_PHOTO = 1001;
    private Subscription subsInsert;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initView();
    }
    private void initView() {
        insertDialog = new ProgressDialog(this);
        insertDialog.setMessage("正在插入图片...");
        insertDialog.setCanceledOnTouchOutside(false);
        btMain.setVisibility(View.VISIBLE);
        btMain.setText("添加");
        btMain.setOnClickListener(v -> ActionSheet.createBuilder(MainActivity.this, getSupportFragmentManager())
                .setCancelButtonTitle("取消")
                .setOtherButtonTitles("相机", "相册")
                .setCancelableOnTouchOutside(true)
                .setListener(MainActivity.this).show());

    }

    @Override
    public void onDismiss(ActionSheet actionSheet, boolean isCancel) {
        actionSheet.dismiss();
    }

    @Override
    public void onOtherButtonClick(ActionSheet actionSheet, int index) {
        switch (index) {
            case 0:
                Intent intent = new Intent(MainActivity.this, TakePhotoActivity.class);
                startActivityForResult(intent, REQUEST_TAKE_PHOTO);
                actionSheet.dismiss();
                break;
            case 1:
                GalleryFinal.selectMedias(this, GalleryFinal.TYPE_ALL, 10, photoArrayList -> {
                    ArrayList<String> list= new ArrayList<>();
                    if (photoArrayList.size()>0){
                        for (int i=0;i<photoArrayList.size();i++){
                            list.add(photoArrayList.get(i).getPath());
                        }
                        insertImagesSync(list);
                    }

                });
                break;
        }
    }
    /**
     * 异步方式插入图片
     *
     * @param
     */
    private void insertImagesSync(final ArrayList<String> photos) {
        insertDialog.show();

        subsInsert = Observable.create((Observable.OnSubscribe<String>) subscriber -> {
            try {
                etMain.measure(0, 0);
                int width = ScreenUtils.getScreenWidth(MainActivity.this);
                int height = ScreenUtils.getScreenHeight(MainActivity.this);
//                    ArrayList<String> photos = data.getStringArrayListExtra(PhotoPicker.KEY_SELECTED_PHOTOS);
//                    //可以同时插入多张图片
                for (String imagePath : photos) {
                    if (imagePath.contains("jpg")||imagePath.contains("png")){
                        //Log.i("NewActivity", "###path=" + imagePath);
                        Bitmap bitmap = ImageUtils.getSmallBitmap(imagePath, width, height);//压缩图片
                        //bitmap = BitmapFactory.decodeFile(imagePath);
                        imagePath = SDCardUtil.saveToSdCard(bitmap);
                        //Log.i("NewActivity", "###imagePath="+imagePath);
                    }

                    subscriber.onNext(imagePath);
                }
                subscriber.onCompleted();
            } catch (Exception e) {
                e.printStackTrace();
                subscriber.onError(e);
            }
        })
                .onBackpressureBuffer()
                .subscribeOn(Schedulers.io())//生产事件在io
                .observeOn(AndroidSchedulers.mainThread())//消费事件在UI线程
                .subscribe(new Observer<String>() {
                    @Override
                    public void onCompleted() {
                        insertDialog.dismiss();
                        etMain.addEditTextAtIndex(etMain.getLastIndex(), " ");
                        showToast(MainActivity.this,"图片/视频插入成功");
                    }

                    @Override
                    public void onError(Throwable e) {
                        insertDialog.dismiss();
                        showToast(MainActivity.this,"图片/视频插入失败" );
                    }

                    @Override
                    public void onNext(String imagePath) {
                        if (imagePath.contains("jpg")||imagePath.contains("png")){
                            etMain.insertImage(imagePath, etMain.getMeasuredWidth());
                        }else {
                            int width = ScreenUtils.getScreenWidth(MainActivity.this);
                            int height = ScreenUtils.getScreenHeight(MainActivity.this);
                            etMain.insertVideo(imagePath,VideoThumbLoader.getInstance().
                                    showThumb(imagePath,width,height), MainActivity.this);
                        }
                    }
                });
    }
    @Override
    protected void onPause() {
        super.onPause();
        etMain.releaseVideo();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    String path = data.getStringExtra("path");
                    ArrayList<String> list = new ArrayList<>();
                    list.add(path);
                    if (data.getBooleanExtra("take_photo", true)) {
                        //照片
                        insertImagesSync(list);
                    } else {
                        //小视频
                        insertImagesSync(list);
                    }
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);

    }
    public  void showToast(Context context,String msg) {
        Toast mToast;
        mToast = Toast.makeText(context, "", Toast.LENGTH_SHORT);
        mToast.setText(msg);
        mToast.show();
    }
}

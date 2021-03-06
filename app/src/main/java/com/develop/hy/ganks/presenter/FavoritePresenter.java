package com.develop.hy.ganks.presenter;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;

import com.develop.hy.ganks.model.Favorite;
import com.develop.hy.ganks.model.User;
import com.develop.hy.ganks.model.UserFile;
import com.develop.hy.ganks.presenter.CommenInterface.IFavoriteView;
import com.develop.hy.ganks.utils.ToastUtils;

import java.io.File;
import java.util.List;

import cn.bmob.v3.BmobObject;
import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobUser;
import cn.bmob.v3.datatype.BmobFile;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.ProgressCallback;
import cn.bmob.v3.listener.UpdateListener;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;

/**
 * Created by Helloworld on 2017/9/20.
 */

public class FavoritePresenter extends BasePresenter<IFavoriteView> {
    private String TAG = "FavoritePresenter";
    private final User user;
    private UserFile userFile;
    private String url;
    private BmobFile bmobFile;

    public FavoritePresenter(Context context, IFavoriteView iView) {
        super(context, iView);
        user = BmobUser.getCurrentUser(User.class);
    }

    @Override
    public void release() {
        unSubcription();
    }
    public void  getUserBgAndHeadImg(){

        BmobQuery<UserFile> query = new BmobQuery<>();
        query.addWhereEqualTo("username",user.getUsername());
        query.order("-updateAt");
        query.include("userId");//希望查询头像的同时也把用户背景图也查询出来
        query.findObjects(new FindListener<UserFile>() {
            @Override
            public void done(List<UserFile> list, BmobException e) {
                if (list!=null){
                   iView.initUserInfo(list);
                }else {

                }
            }
        });
    }

    public void getFavorite() {
        BmobQuery<Favorite> query = new BmobQuery<>();
        query.addWhereEqualTo("userId",user);
        query.order("-updateAt");
        query.include("userId");//希望查询收藏的同时也把用户信息也查询出来
        query.findObjects(new FindListener<Favorite>() {
            @Override
            public void done(List<Favorite> list, BmobException e) {
                if (list!=null){
                    iView.initData(list);
                }else {
                    iView.showErrorData();
                }
            }
        });
    }

    public void deleteFavorite(int position, final List<Favorite> favorites) {
        Favorite favorite = new Favorite();
        favorite.setObjectId(favorites.get(position).getObjectId());
        favorite.delete(new UpdateListener() {
            @Override
            public void done(BmobException e) {
                ToastUtils.showShortToast("刪除收藏成功");
            }
        });
    }

    public void pushImage(List<String> path, final boolean isHeadImg) {

        final ProgressDialog  dialog = new ProgressDialog(context);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setTitle("上传中...");
        dialog.setIndeterminate(false);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        final User user = BmobUser.getCurrentUser(User.class);


        File file = new File(path.get(0));

        bmobFile = new BmobFile(file);

        bmobFile.uploadObservable(new ProgressCallback() {//上传文件操作
            @Override
            public void onProgress(Integer value, long total) {
                log("uploadMovoieFile-->onProgress:"+value);
                dialog.setProgress(value);
            }
        }).doOnNext(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                url = bmobFile.getUrl();
                log("上传成功："+ url +","+ bmobFile.getFilename());
                dialog.dismiss();
            }
        }).concatMap(new Func1<Void, Observable<String>>() {//将bmobFile保存到UserFile表中
            @Override
            public Observable<String> call(Void aVoid) {
                userFile = new UserFile();
                userFile.setUserId(user);
                userFile.setUsername(user.getUsername());
                userFile.setBgimg(bmobFile.getUrl());
                updateData(userFile.getObjectId(),isHeadImg);
                return saveObservable(userFile);
            }
        }).subscribe(new Subscriber<String>() {
            @Override
            public void onCompleted() {
                log("--onCompleted--");
            }

            @Override
            public void onError(Throwable e) {
                log("--onError--:"+e.getMessage());
                dialog.dismiss();
            }

            @Override
            public void onNext(String s) {
                dialog.dismiss();
                log("download的文件地址：");
            }
        });
    }

    private void updateData(String objectId, boolean isHeadImg) {
        User user = BmobUser.getCurrentUser(User.class);
        UserFile userFile = new UserFile();
        userFile.setUsername(user.getUsername());
           if (!isHeadImg){
               userFile.setHeadImg(url);
           }else {
               userFile.setBgimg(url);
           }
        userFile.update(user.getUserInfoId(), new UpdateListener() {
            @Override
            public void done(BmobException e) {
                log("更新数据成功");
                iView.initViews();
            }
        });

    }

    private void log(String s) {
        Log.d(TAG,s);
    }

    /**
     * save的Observable
     * @param obj
     * @return
     */
    private Observable saveObservable(BmobObject obj){

        return obj.updateObservable();
    }

}

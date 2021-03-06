package br.com.frametcc.view.utils;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.test.suitebuilder.annotation.Suppress;

import br.com.frametcc.TCCApplication;
import br.com.frametcc.shared.api.BasePresenter;
import br.com.frametcc.shared.api.BaseView;

public class ActivityNavigator {

    private Activity from;
    private TCCApplication app;

    public ActivityNavigator(Activity from) {
        this.from = from;
        this.app = (TCCApplication) from.getApplication();
    }

    public <V extends BaseView<P>, P extends BasePresenter<V, ?>> void navigateTo(Class<V> clazz) {
        this.navigateTo(clazz, new Bundle());
    }

    @SuppressWarnings("all")
    public <V extends BaseView<P>, P extends BasePresenter<V, ?>> void navigateClearTop(Class<V> clazz) {
        Intent i = createIntent(clazz);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.navigateTo(i);
    }

    public <V extends BaseView<P>, P extends BasePresenter<V, ?>> void navigateTo(Intent intent) {
        this.from.startActivity(intent);
    }

    public <V extends BaseView<P>, P extends BasePresenter<V, ?>> void navigateTo(Class<V> clazz, Bundle extras) {
        Intent i = createIntent(clazz);
        i.putExtras(extras);
        this.from.startActivity(i);
    }

    public void navigateForResult(Class<? extends BaseView> clazz, int requestCode) {
        this.navigateForResult(clazz, new Bundle(), requestCode);
    }

    public void navigateForResult(Intent intent, int requestCode) {
        this.from.startActivityForResult(intent, requestCode);
    }

    public void navigateForResult(Class<? extends BaseView> clazz, Bundle extras, int requestCode) {
        Intent i = createIntent(clazz);
        i.putExtras(extras);
        this.from.startActivityForResult(i, requestCode);
    }

    public void startService(Class<? extends Service> service) {
        Intent i = new Intent();
        i.setClass(this.from, service);
        this.from.startService(i);
    }

    private Intent createIntent(Class clazz) {
        clazz = this.app.getViewImpl(clazz);
        return new Intent(this.from, clazz);
    }
}
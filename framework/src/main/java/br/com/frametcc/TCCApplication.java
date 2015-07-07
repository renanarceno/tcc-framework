package br.com.frametcc;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.Map;

import br.com.frametcc.database.DAOHelper;
import br.com.frametcc.database.DBConnectionHelper;
import br.com.frametcc.database.dao.DatabaseDAO;
import br.com.frametcc.factory.DAOFactory;
import br.com.frametcc.factory.ViewPresenterFactory;
import br.com.frametcc.shared.AbstractBasePresenter;
import br.com.frametcc.shared.api.BasePresenter;
import br.com.frametcc.shared.api.BaseView;

public abstract class TCCApplication extends Application {
    private static final int DEFAULT_DB_VERSION = 0;
    public static final String DB_VERSION = "tccframework.dbVersion";
    public static final String DB_NAME = "tccframework.dbName";
    public static final String DB_ASSETS_DB_CREATE_FOLDER = "tccframework.dbAssets.dbCreateFolder";
    public static final String DB_ASSETS_DB_UPDATE_FOLDER = "tccframework.dbAssets.dbUpdateFolder";

    private static DBConnectionHelper dbConnection;

    private final TCCLog log = TCCLog.getInstance();
    private Activity lastActivity;
    private ViewPresenterFactory factory;
    private DAOFactory daoFactory;
    private DAOHelper daoHelper;

    @Override
    @SuppressWarnings("all")
    public final void onCreate() {
        super.onCreate();
        getDbConfig();
        this.create();
        this.factory = new ViewPresenterFactory();
        this.daoFactory = new DAOFactory(this.getApplicationContext());
        this.daoFactory.daos = getDaosIntImpl();
        this.factory.viewPresenterImpl = getControlViewMap();
        this.factory.presenters = getControlIntImpl();
        this.factory.views = getViewIntImpl();
        this.onAfterCreate();
    }

    @SuppressWarnings("all")
    private void getDbConfig() {
        ApplicationInfo appInfo = null;
        try {
            appInfo = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        } catch (PackageManager.NameNotFoundException ignore) {
        }

        final Bundle metaData = appInfo.metaData;
        int dbVersion = metaData.getInt(DB_VERSION, DEFAULT_DB_VERSION);
        if (dbVersion == DEFAULT_DB_VERSION)
            log.w("Database VERSION not set");
        String dbName = metaData.getString(DB_NAME);
        if (dbName == null || dbName.trim().isEmpty())
            log.w("Database NAME not set");

        String dbCreateFolder = metaData.getString(DB_ASSETS_DB_CREATE_FOLDER);
        String dbUpdateFolder = metaData.getString(DB_ASSETS_DB_UPDATE_FOLDER);

        if (dbVersion != DEFAULT_DB_VERSION && !dbName.isEmpty())
            dbConnection = new DBConnectionHelper(this.getApplicationContext(), dbName, null, dbVersion, dbCreateFolder, dbUpdateFolder);
    }

    public static DBConnectionHelper getDbConnection() {
        return dbConnection;
    }

    public void setLogEnabled(boolean show) {
        log.setLogEnable(show);
    }

    public void setupView(BaseView view) {
        this.factory.createPresenterInstance(view);
    }

    public void setTopActivity(Activity activity) {
        this.lastActivity = activity;
    }

    public Activity getTopActivity() {
        return this.lastActivity;
    }

    /**
     * Sinaliza quando o aplicativo ficou em segundo plano (totalmente invisível ao usuário).
     */
    public void onActivityStopped(Activity activity) {
        if (this.lastActivity.equals(activity))
            log.w("App stopped");
    }

    /**
     * Sinaliza quando o aplicativo voltou a ser visível e o usuário consegue interagir com ele novamente.
     */
    public void onApplicationRestarted() {
        log.w("App restarted");
    }

    /**
     * Chamado quando uma activity é destruída
     */
    public void onActivityDestroyed(Activity activity) {
        BaseView v = ((BaseView) activity);
        this.factory.removeControl(v.getClass());
        log.w("Activity " + activity.getClass().getSimpleName() + " destroyed");
    }

    public <T extends DatabaseDAO> T getDao(Class<T> clazz) {
        T dao = daoFactory.getDao(clazz);
        if (dao == null) {
            throw new RuntimeException("DAO não mapeado para uma implementação");
        }
        return dao;
    }

    @Nullable
    public <C extends BasePresenter<?>, CI extends C> CI getControl(Class<CI> control) {
        return this.factory.getControl(control);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <V extends BaseView<C>, C extends BasePresenter<V>> Class<V> getViewImpl(Class<V> viewInterface) {
        return (Class<V>) this.factory.getViewImpl(viewInterface);
    }

    /**
     * Chamado ao criar a aplicação
     */
    public abstract void create();

    /**
     * Chamado assim que a aplicação inicializa as Factories
     */
    public void onAfterCreate() {

    }

    /**
     * Mapeamento da implementação do controle com sua implementação da visão
     */
    public abstract Map<Class<? extends BaseView>, Class<? extends BasePresenter>> getControlViewMap();

    /**
     * Mapeamento da interface do controle com sua implementação
     */
    public abstract Map<Class<? extends BasePresenter>, Class<? extends AbstractBasePresenter>> getControlIntImpl();

    /**
     * Mapeamento da interface da view com sua implementação
     */
    public abstract Map<Class<? extends BaseView>, Class<? extends BaseView>> getViewIntImpl();

    /**
     * Mapeamento da interface do DAO com sua implementação, que estende de DBHelper
     */
    public abstract Map<Class<? extends DatabaseDAO>, DAOHelper> getDaosIntImpl();

}
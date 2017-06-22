package com.alisir.dagger2test.dg;

import dagger.Module;
import dagger.Provides;

/**
 * Created by ALiSir on 17/6/22.
 */

@Module
public class MainModule {
    private final MainContract.view mView;

    public MainModule(MainContract.view mView) {
        this.mView = mView;
    }

    @Provides
    MainContract.view provideMainView(){
        return mView;
    }

}

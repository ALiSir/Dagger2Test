package com.alisir.dagger2test;

import com.alisir.dagger2test.dg.MainContract;

import javax.inject.Inject;

/**
 * Created by ALiSir on 17/6/22.
 */

public class MainPresenter {
    private MainContract.view mView;

    @Inject
    public MainPresenter(MainContract.view mView) {
        this.mView = mView;
    }

    public void loadData(){
        mView.updataUI();
    }

}

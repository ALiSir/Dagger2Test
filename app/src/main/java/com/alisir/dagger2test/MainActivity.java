package com.alisir.dagger2test;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.alisir.dagger2test.dg.DaggerMainComponent;
import com.alisir.dagger2test.dg.MainContract;
import com.alisir.dagger2test.dg.MainModule;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity implements MainContract.view {

    @Inject
    MainPresenter mainPresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DaggerMainComponent.builder()
                .mainModule(new MainModule(this))
                .build()
                .inject(this);

        mainPresenter.loadData();
    }

    @Override
    public void updataUI() {
        ((TextView)findViewById(R.id.title_s)).setText("Dagger2");
    }
}

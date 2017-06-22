package com.alisir.dagger2test.dg;

import com.alisir.dagger2test.MainActivity;

import dagger.Component;

/**
 * Created by ALiSir on 17/6/22.
 */
@Component(modules = MainModule.class)
public interface MainComponent {
    void inject(MainActivity activity);
}

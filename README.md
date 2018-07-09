# Dagger2Test
Dagger2基本原理
转载自：<a>http://www.jianshu.com/p/39d1df6c877d</a>

现在Dagger2在项目里用的越来越多了，最近花了些时间学习了一下Dagger2，这篇文章主要帮助理解Dagger2的注入实现过程，如有错误，还请指正!

## 写在前面的话

Dagger2是Dagger的升级版，是一个依赖注入框架，现在由Google接手维护。 恩，这里有个关键字依赖注入，因此我们得先知道什么是依赖注入，才能更好的理解Dagger2。

依赖注入是面向对象编程的一种设计模式，其目的是为了降低程序耦合，这个耦合就是类之间的依赖引起的。

举个例子：我们在写面向对象程序时，往往会用到组合，即在一个类中引用另一个类，从而可以调用引用的类的方法完成某些功能,就像下面这样.

```
public class ClassA {
      ...
      ClassB b;
      ...
      public ClassA() {
          b = new ClassB();
      }

      public void do() {
          ...
          b.doSomething();
         ...
      }
  }
  ```

  
这个时候就产生了依赖问题，ClassA依赖于ClassB，必须借助ClassB的方法，才能完成一些功能。这样看好像并没有什么问题，但是我们在ClassA的构造方法里面直接创建了ClassB的实例，问题就出现在这，在ClassA里直接创建ClassB实例，违背了单一职责原则，ClassB实例的创建不应由ClassA来完成；其次耦合度增加，扩展性差，如果我们想在实例化ClassB的时候传入参数，那么不得不改动ClassA的构造方法，不符合开闭原则。

因此我们需要一种注入方式，将依赖注入到宿主类（或者叫目标类）中，从而解决上面所述的问题。依赖注入有一下几种方式：

通过接口注入
```
  interface ClassBInterface {
      void setB(ClassB b);
  }

  public class ClassA implements ClassBInterface {
      ClassB classB;

      @override
      void setB(ClassB b) {
          classB = b;
      }
  }
```
通过set方法注入
```
  public class ClassA {
      ClassB classB;

      public void setClassB(ClassB b) {
          classB = b;
      }
  }
  ```
通过构造方法注入
```
  public class ClassA {
      ClassB classB;

      public void ClassA(ClassB b) {
          classB = b;
      }
```
通过Java注解
```
  public class ClassA {
      //此时并不会完成注入，还需要依赖注入框架的支持，如RoboGuice,Dagger2
      @inject ClassB classB;

      ...
      public ClassA() {}
```
在Dagger2中用的就是最后一种注入方式，通过注解的方式，将依赖注入到宿主类中。

## 1.如何引入Dagger2

### gradle 2.2 以下

配置apt插件(在build.gradle(Project:xxx)中添加如下代码)
```
  dependencies {
      classpath 'com.android.tools.build:gradle:2.1.0'
      //添加apt插件
      classpath 'com.neenbedankt.gradle.plugins:android-apt:1.8'

  }
  
  
```
添加依赖(在build.gradle(Module:app)中添加如下代码)
```
  apply plugin: 'com.android.application'
  //添加如下代码，应用apt插件
  apply plugin: 'com.neenbedankt.android-apt'
  ...
  dependencies {
      ...
      compile 'com.google.dagger:dagger:2.4'
      apt 'com.google.dagger:dagger-compiler:2.4'
      //java注解
      compile 'org.glassfish:javax.annotation:10.0-b28'
      ...
  }
```
### gradle 2.2以上
dependencies {
      ...
      api 'com.google.dagger:dagger:2.4'
      annotationProcessor 'com.google.dagger:dagger-compiler:2.4'
      //java注解
      api 'org.glassfish:javax.annotation:10.0-b28'
      ...
  }


## 2.使用Dagger2

下面用一个栗子来说明，如何使用Dagger2，需要说明的是，这个栗子是基于mvp模式的，所以如果还不了解mvp的话，可以先去了解mvp，再继续看下面的内容。

在mvp中，最常见的一种依赖关系，就是Activity持有presenter的引用，并在Activity中实例化这个presenter，即Activity依赖presenter，presenter又需要依赖View接口，从而更新UI，就像下面这样：
```
public class MainActivity extends AppCompatActivity implements MainContract.View {
    private MainPresenter mainPresenter;
    ...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //实例化presenter 将view传递给presenter
        mainPresenter = new MainPresenter(this);
        //调用Presenter方法加载数据
         mainPresenter.loadData();

         ...
    }

}
```

```
public class MainPresenter {
    //MainContract是个接口，View是他的内部接口，这里看做View接口即可
    private MainContract.View mView;

    MainPresenter(MainContract.View view) {
        mView = view;
    }

    public void loadData() {
        //调用model层方法，加载数据
        ...
        //回调方法成功时
        mView.updateUI();
    }
```
这样Activity与presenter仅仅耦合在了一起，当需要改变presenter的构造方式时，需要修改这里的代码。如果用依赖注入的话，是这样的：
```
public class MainActivity extends AppCompatActivity implements MainContract.View {
    @Inject
    MainPresenter mainPresenter;
    ...

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

         DaggerMainComponent.builder()
                .mainModule(new MainModule(this))
                .build()
                .inject(this);
        //调用Presenter方法加载数据
         mainPresenter.loadData();

         ...
    }

}
```
```
public class MainPresenter {
    //MainContract是个接口，View是他的内部接口，这里看做View接口即可
    private MainContract.View mView;

    @Inject
    MainPresenter(MainContract.View view) {
        mView = view;
    }    
    public void loadData() {
        //调用model层方法，加载数据
        ...
        //回调方法成功时
        mView.updateUI();
    }
```
```
@Module
public class MainModule {
    private final MainContract.View mView;

    public MainModule(MainContract.View view) {
        mView = view;
    }

    @Provides
    MainView provideMainView() {
        return mView;
    }
}
```
```
@Component(modules = MainModule.class)
public interface MainComponent {
    void inject(MainActivity activity);
}
```
额，貌似变得更复杂了，还不如不用Dagger2呢。不过仔细想想也是可以理解的，直接组合方式虽然简单，但是具有耦合性，为了解决这种耦合，可能就会多产生一些辅助类，让这种直接的依赖关系，变为间接，降低耦合。跟大多数设计模式一样，为了达到高内聚低耦合，往往会有很多接口与类，Daager2也是如此，虽然看似复杂了些，不过这在软件工程中是值得的。下面，就来分析下上面代码是什么意思。

我们先看MainActivity里的代码，之前是直接声明MainPresenter，现在在声明的基础上加了一个注解@Inject，表明MainPresenter是需要注入到MainActivity中，即MainActivity依赖于MainPresenter，这里要注意的是，使用@Inject时，不能用private修饰符修饰类的成员属性。

然后我们在MainPresenter的构造函数上同样加了@Inject注解。这样MainActivity里的mainPresenter与他的构造函数建立了某种联系。这种联系我们可以这样理解，当看到某个类被@Inject标记时，就会到他的构造方法中，如果这个构造方法也被@Inject标记的话，就会自动初始化这个类，从而完成依赖注入。

然后，他们之间并不会凭空建立起联系，因此我们想到，肯定需要一个桥梁，将他们连接起来，也就是下面要介绍的Component。

Component是一个接口或者抽象类，用@Component注解标注（这里先不管括号里的modules），我们在这个接口中定义了一个inject()方法,参数是Mainactivity。然后rebuild一下项目，会生成一个以Dagger为前缀的Component类，这里是DaggerMainComponent，然后在MainActivity里完成下面代码.
```
DaggerMainComponent.builder()
                .mainModule(new MainModule(this))
                .build()
                .inject(this);
```
此时Component就将@Inject注解的mainPresenter与其构造函数联系了起来。此时，看到这里，如果是初学者的话，一定会非常迷惑，究竟是怎么建立起联系的，实例化过程在哪？别急，后面会讲解这个过程原理的。

此时我们已经完成了presenter的注入过程，但是我们发现还有一个MainModule类，这个类是做什么的？MainModlue是一个注解类，用@Module注解标注，主要用来提供依赖。等等，刚才通过@Inject就可以完成依赖，为什么这里还要用到Module类来提供依赖？之所以有Module类主要是为了提供那些没有构造函数的类的依赖，这些类无法用@Inject标注，比如第三方类库，系统类，以及上面示例的View接口。

我们在MainModule类里声明了MainContract.View成员属性，在构造方法里将外界传进来的view赋值给mView，并通过一个@Provides标注的以provide开头的方法，将这个view返回，这个以provide开头的方法就是提供依赖的，我们可以创建多个方法来提供不同的依赖。那么这个类究竟是怎么作用的？可以想到上面提到的@Component注解括号里的东西了。就是下面这个
```
@Component(modules = MainModule.class)
public interface MainComponent {
    void inject(MainActivity activity);
}
```
所以Module要发挥作用，还是要依靠于Component类，一个Component类可以包含多个Module类，用来提供依赖。我们接着看下面这段代码：
```
DaggerMainComponent.builder()
                .mainModule(new MainModule(this))
                .build()
                .inject(this);
```
这里通过new MainModule(this)将view传递到MainModule里，然后MainModule里的provideMainView()方法返回这个View，当去实例化MainPresenter时，发现构造函数有个参数，此时会在Module里查找提供这个依赖的方法，将该View传递进去，这样就完成了presenter里View的注入。

我们来重新理一遍上面的注入过程，首先弄清楚以下几个概念：

@Inject 带有此注解的属性或构造方法将参与到依赖注入中，Dagger2会实例化有此注解的类
@Module 带有此注解的类，用来提供依赖，里面定义一些用@Provides注解的以provide开头的方法，这些方法就是所提供的依赖，Dagger2会在该类中寻找实例化某个类所需要的依赖。
@Component 用来将@Inject和@Module联系起来的桥梁，从@Module中获取依赖并将依赖注入给@Inject
接着我们重新回顾一下上面的注入过程：首先MainActivity需要依赖MainPresenter，因此，我们在里面用@Inject对MainPresenter进行标注，表明这是要注入的类。然后，我们对MainPresenter的构造函数也添加注解@Inject，此时构造函数里有一个参数MainContract.View，因为MainPresenter需要依赖MainContract.View，所以我们定义了一个类，叫做MainModule，提供一个方法provideMainView，用来提供这个依赖，这个MainView是通过MainModule的构造函数注入进来的，接着我们需要定义Component接口类，并将Module包含进来，即
```
@Component(modules = MainModule.class)
public interface MainComponent {
    void inject(MainActivity activity);
}
```
同时里面声明了一个inject方法，方法参数为ManActivity，用来获取MainActivity实例，以初始化在里面声明的MainPresenter
```
DaggerMainComponent.builder()
                .mainModule(new MainModule(this))
                .build()
                .inject(this);
```
此时，注入过程就完成了，或许到此时，还是会有一些疑惑，因为我们看不到实例化的过程在哪里，为什么要这样去写代码，所以下面，就基于这个实例，分析Dagger2内部究竟做了什么。

## 3.Dagger2注入原理

Dagger2与其他依赖注入框架不同，它是通过apt插件在编译阶段生成相应的注入代码，下面我们就具体看看Dagger2生成了哪些注入代码？

我们先看MainPresenter这个类，在这个类中我们对构造方法用了@Inject标注，然后Rebuild Project，Dagger2会在/app/build/generated/apt/debug/目录下生成一个对应的工厂类MainPresenter_Factory，我们看下面具体代码（为了方便理解，我把MainPresenter也贴了出来）
```
public class MainPresenter {
    MainContract.View mView;
    @Inject
    MainPresenter(MainContract.View view) {
        mView = view;
    }
 }
```

```
public final class MainPresenter_Factory implements Factory<MainPresenter> {
  private final Provider<MainContract.View> viewProvider;

  public MainPresenter_Factory(Provider<MainContract.View> viewProvider) {
    assert viewProvider != null;
    this.viewProvider = viewProvider;
  }

  @Override
  public MainPresenter get() {
    return new MainPresenter(viewProvider.get());
  }

  public static Factory<MainPresenter> create(Provider<MainContract.View> viewProvider) {
    return new MainPresenter_Factory(viewProvider);
  }
}
```
对比MainPresenter,我们发现在MainPre_Factory里也生成了对应的代码。首先是viewProvide，这是一个Provider类型，泛型参数就是我们的MainContract.View，接着通过构造方法，对viewProvider进行实例化。其实这里有个疑惑，上面的成员属性为什么不直接是MainContract.View，而是Provider类型？看到provider我们应该想到这个MainContract.View是一个依赖，而依赖的提供者是MainModule，因此这个viewProvider一定是由MainModul提供的。我们接着看下面的get()方法，看到这个方法，我想我们有点恍然大悟的感觉，原来MainPresenter的实例化就在这里，构造函数里的参数就是我们依赖的MainContract.View，它是由viewProvider通过get()提供。接着是一个create()方法，并且有一个参数viewProvider，用来创建这个MainPresenter_Factory类。

上面我们得出，viewProvider是由MainModule提供的，所以我们接着看MainModule所对应的注入类。

```
@Module
public class MainModule {
    private final MainContract.View mView;

    public MainModule(MainContract.View view) {
        mView = view;
    }

    @Provides
    MainContract.View provideMainView() {
        return mView;
   }   
}
```

```
public final class MainModule_ProvideMainViewFactory implements Factory<MainContract.View> {
  private final MainModule module;

  public MainModule_ProvideMainViewFactory(MainModule module) {
    assert module != null;
    this.module = module;
  }

  @Override
  public MainContract.View get() {
    return Preconditions.checkNotNull(
        module.provideMainView(), "Cannot return null from a non-@Nullable @Provides method");
  }

  public static Factory<MainContract.View> create(MainModule module) {
    return new MainModule_ProvideMainViewFactory(module);
  }
}
```
看到上面的类名，我们发现了一种对应关系，在MainModule中定义的@Provides修饰的方法会对应的生成一个工厂类，这里是MainModule_ProvideMainViewFactory。我们看到这个类里有一个get()方法，其中调用了MainModule里的provideMainView()方法来返回我们所需要的依赖MainContract.View。还记得在MainPresenter_Factory里的get()方法中，实例化MainPresenter时候的参数viewProvider.get()吗？到这里我们就明白了，原来那个viewProvider就是生成的MainModule_ProvideMainViewFactory，然后调用了其get()方法，将我们需要的MainContract.View注入到MainPresenter里。

看到这里我们应该明白了MainPresenter的实例化过程。MainPresenter会对应的有一个工厂类，在这个类的get()方法中进行MainPresenter创建，而MainPresenter所需要的View依赖，是由MainModule里定义的以provide开头的方法所对应的工厂类提供的。

虽然我们明白了实例化的创建过程，但是此时还是有点疑惑，MainPresenter_Factory的创建是由create()完成的，那么crate是在哪调用的，此时创建的MainPresenter实例是怎么跟@Inject注解的MainPresenter关联起来的，我想你已经知道了答案，没错就是Component。前面说过Component是连接@Module和@Inject的桥梁，所以上面的疑惑就要到编译后Component所对应的类中寻找答案。
```
@Component(modules = MainModule.class)
public interface MainComponent {
    void inject(MainActivity activity);
}

public final class DaggerMainComponent implements MainComponent {
  private Provider<MainContract.View> provideMainViewProvider;

  private Provider<MainPresenter> mainPresenterProvider;

  private MembersInjector<MainActivity> mainActivityMembersInjector;

  private DaggerMainComponent(Builder builder) {
    assert builder != null;
    initialize(builder);
  }

  public static Builder builder() {
    return new Builder();
  }

  @SuppressWarnings("unchecked")
  private void initialize(final Builder builder) {

    this.provideMainViewProvider = MainModule_ProvideMainViewFactory.create(builder.mainModule);

    this.mainPresenterProvider = MainPresenter_Factory.create(provideMainViewProvider);

    this.mainActivityMembersInjector = MainActivity_MembersInjector.create(mainPresenterProvider);
  }

  @Override
  public void inject(MainActivity activity) {
    mainActivityMembersInjector.injectMembers(activity);
  }

  public static final class Builder {
    private MainModule mainModule;

    private Builder() {}

    public MainComponent build() {
      if (mainModule == null) {
        throw new IllegalStateException(MainModule.class.getCanonicalName() + " must be set");
      }
      return new DaggerMainComponent(this);
    }

    public Builder mainModule(MainModule mainModule) {
      this.mainModule = Preconditions.checkNotNull(mainModule);
      return this;
    }
  }
}
```
从上面代码看到定义的MainComponent会生成一个对应的DaggerMainComponent，并且实现了MainComponent里的方法。我们看到代码中又出现了Provide类型的成员属性，前面说过这个Provide类型就是所提供的依赖，我们在看它们是在哪实例化的。我们看到有一个initialize()方法
```
@SuppressWarnings("unchecked")
  private void initialize(final Builder builder) {

    this.provideMainViewProvider = MainModule_ProvideMainViewFactory.create(builder.mainModule);

    this.mainPresenterProvider = MainPresenter_Factory.create(provideMainViewProvider);

    this.mainActivityMembersInjector = MainActivity_MembersInjector.create(mainPresenterProvider);
  }
  ```
看到这估计就明白了刚才的疑惑。首先创建了MainModule_ProvideMainViewFactory实例，用来提供MainContract.View依赖。这里可能有个小疑惑，create()方法返回的是Factory类型，而provideMainViewProvider是个Provider类型，其实看源码就明白了，Factory继承自Provider。
```
public interface Factory<T> extends Provider<T> {
}
```
然后将provideMainViewProvider传递到MainPresenter_Factory里，即就是前面讲到的viewProvider。接着将这个mainPresenterProvider又传递到MainActivity_MembersInjector中进行实例化，我们看到这个类前面是MainActivity开头，因此可以想到是MainActivity对应得注入类，我们后面再分析这个类。

接着是我们在MainComponent里定义的Inject方法的实现，这里调用了mainActivityMembersInjector.injectMembers(activity)方法，将我们的MainActivity注入到该类中。

接着就是Builder内部类，用来创建我们的module以及自身实例。所以在DaggerMainComponent里主要用来初始化依赖，而真正的将这些依赖于Inject关联起来的就是刚才的MainActivity_MembersInjector类，我们看看这个类里做了什么。
```
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<MainPresenter> mainPresenterProvider;

  public MainActivity_MembersInjector(Provider<MainPresenter> mainPresenterProvider) {
    assert mainPresenterProvider != null;
    this.mainPresenterProvider = mainPresenterProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<MainPresenter> mainPresenterProvider) {
    return new MainActivity_MembersInjector(mainPresenterProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    if (instance == null) {
      throw new NullPointerException("Cannot inject members into a null reference");
    }
    instance.mainPresenter = mainPresenterProvider.get();
  }

  public static void injectMainPresenter(
      MainActivity instance, Provider<MainPresenter> mainPresenterProvider) {
    instance.mainPresenter = mainPresenterProvider.get();
  }
}
```
这个类的关键就是injectMembers()方法，还记得这个方法在哪调用吗?我想你肯定记得，就在刚才提到的DaggerMainComponent类中的inject()方法里,所以这里的instance实例是由DaggerMainComponent提供的，然后我们看到了最关键的一句代码

instance.mainPresenter = mainPresenterProvider.get();
看到这，我想应该一切都明白了，将mainPresenterProvider中创建好的MainPresenter实例赋值给instance(MainActivity)的成员mainPresenter，这样我们用@Inject标注的mainPresenter就得到了实例化，接着就可以在代码中使用了。

到这里，就分析完了Dagger2的注入过程，如果不去看这些生成的类，就很难理解整个过程究竟是怎么发生的，从而导致还是不知道怎么去使用这个依赖注入框架。所以重点去理解这个内部实现原理是非常重要的，刚开始学的时候也是一脸懵逼，总搞不太清之间的关系，不知道究竟怎么写，弄懂了整个来龙去脉后，发现就知道怎么去运用了。

关于Dagger2的其他使用就不多讲了，可以看其他的优秀博客，我会再后面附上链接，方便学习。Dagger2就是一个依赖注入工具，至于怎么使用完全在个人，所以不必纠结到底怎么去写才是正确的，只要弄懂原理，灵活运用，能够做到尽可能解耦，适合自己的业务就是最好的写法。

感谢：

http://www.jianshu.com/p/cd2c1c9f68d4

http://alighters.com/blog/2016/04/15/dagger2-indepth-understanding/

http://chriszou.com/2016/05/10/android-unit-testing-di-dagger.html

http://blog.nimbledroid.com/2016/03/07/performance-of-dependency-injection-libraries-zh.html

http://google.github.io/dagger/


# 联系方式：

QQ群：584616826
  
![QQ群](https://github.com/ALiSir/Resource/raw/master/Images/qq.JPG "扫一扫，加入QQ群！")


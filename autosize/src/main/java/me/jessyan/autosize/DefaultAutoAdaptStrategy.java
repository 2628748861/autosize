/*
 * Copyright 2018 JessYan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.jessyan.autosize;

import android.app.Activity;
import android.app.Application;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.Locale;

import me.jessyan.autosize.external.ExternalAdaptInfo;
import me.jessyan.autosize.internal.CancelAdapt;
import me.jessyan.autosize.internal.CustomAdapt;
import me.jessyan.autosize.utils.LogUtils;

/**
 * ================================================
 * 屏幕适配逻辑策略默认实现类, 可通过 {@link AutoSizeConfig#init(Application, boolean, AutoAdaptStrategy)}
 * 和 {@link AutoSizeConfig#setAutoAdaptStrategy(AutoAdaptStrategy)} 切换策略
 *
 * @see AutoAdaptStrategy
 * Created by JessYan on 2018/8/9 15:57
 * <a href="mailto:jess.yan.effort@gmail.com">Contact me</a>
 * <a href="https://github.com/JessYanCoding">Follow me</a>
 * ================================================
 */
public class DefaultAutoAdaptStrategy implements AutoAdaptStrategy {
    @Override
    public void applyAdapt(Object target, Activity activity) {

        //检查是否开启了外部三方库的适配模式, 只要不主动调用 ExternalAdaptManager 的方法, 下面的代码就不会执行
        if (AutoSizeConfig.getInstance().getExternalAdaptManager().isRun()) {
            if (AutoSizeConfig.getInstance().getExternalAdaptManager().isCancelAdapt(target.getClass())) {
                LogUtils.w(String.format(Locale.ENGLISH, "%s canceled the adaptation!", target.getClass().getName()));
                AutoSize.cancelAdapt(activity);
                return;
            } else {
                ExternalAdaptInfo info = AutoSizeConfig.getInstance().getExternalAdaptManager()
                        .getExternalAdaptInfoOfActivity(target.getClass());
                if (info != null) {
                    LogUtils.d(String.format(Locale.ENGLISH, "%s used %s for adaptation!", target.getClass().getName(), ExternalAdaptInfo.class.getName()));
                    AutoSize.autoConvertDensityOfExternalAdaptInfo(activity, info);
                    return;
                }
            }
        }

        //如果 target 实现 CancelAdapt 接口表示放弃适配, 所有的适配效果都将失效
        if (target instanceof CancelAdapt) {
            LogUtils.w(String.format(Locale.ENGLISH, "%s canceled the adaptation!", target.getClass().getName()));
            AutoSize.cancelAdapt(activity);
            return;
        }


       // LogUtils.d("是否是customD的实例:"+(target instanceof CustomAdapt));
        //getInterfaces(target);
        //如果 target 实现 CustomAdapt 接口表示该 target 想自定义一些用于适配的参数, 从而改变最终的适配效果
        CustomAdapt customAdapt=hasInterfaces(target);
        if (customAdapt!=null) {
            LogUtils.d(String.format(Locale.ENGLISH, "%s implemented by %s!", target.getClass().getName(), CustomAdapt.class.getName()));
            AutoSize.autoConvertDensityOfCustomAdapt(activity, customAdapt);
        } else {
            LogUtils.d(String.format(Locale.ENGLISH, "%s used the global configuration.", target.getClass().getName()));
            AutoSize.autoConvertDensityOfGlobal(activity);
        }
    }

    private CustomAdapt hasInterfaces(Object target)
    {
        try
        {
            Class<?> clasz=target.getClass();
            Class<?> interfaces[] =clasz.getInterfaces();//获得Dog所实现的所有接口
            for (Class<?> inte : interfaces) {//打印
                LogUtils.d("类名："+clasz+",接口:"+inte+",boolean:"+(inte.getName().equals(CustomAdapt.class.getName())));
                if(inte.getName().equals(CustomAdapt.class.getName()))
                {
                    Method method=inte.getMethod("isBaseOnWidth",null);
                    Method method1=inte.getMethod("getSizeInDp",null);
                    final boolean isBaseOnWidth=(Boolean) method.invoke(target,null);
                    final float sizeInDp=(Float) method1.invoke(target,null);
                    LogUtils.d("isBaseOnWidth:"+isBaseOnWidth+",sizeInDp:"+sizeInDp);

                    CustomAdapt customAdapt=new CustomAdapt() {
                        @Override
                        public boolean isBaseOnWidth() {
                            return isBaseOnWidth;
                        }

                        @Override
                        public float getSizeInDp() {
                            return sizeInDp;
                        }
                    };

                    return customAdapt;
                }
            }
        }catch ( Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }
}

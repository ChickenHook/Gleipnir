package org.gleipnir.app.helpers.binder;

import android.content.Context;
import androidx.annotation.NonNull;
import org.chickenhook.binderhooks.ServiceHooks;
import org.chickenhook.binderhooks.proxyListeners.ProxyListener;
import org.gleipnir.app.extendableLoader.LogExtensionsKt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.chickenhook.binderhooks.Logger.log;

public class BinderProxyHook {

    static void dump(Method method, Object[] objects) {
        String str = "\n===================\n" + method.getName() + "\n" + "------------------\n" + "\n";
        if (objects != null) {
            for (Object o : objects) {
                str += o + "\n";
            }
        }

        str += "\n===================\n";
        log("BinderProxyHook" + str);
    }

    public static void installHooks(@NonNull Context context) throws InvocationTargetException, NoSuchMethodException, ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        ServiceHooks.hookActivityManager(new ProxyListener() {
            @Override
            public Object invoke(Object original, Object proxy, Method method, Object[] args) throws Throwable {
                dump(method, args);
                if(method.getName().equals("startService")){
                    LogExtensionsKt.log("Suppress startService");
                    return null;
                }
                return method.invoke(original,args);
            }
        });

        ServiceHooks.hookActivityTaskManager(new ProxyListener() {
            @Override
            public Object invoke(Object original, Object proxy, Method method, Object[] args) throws Throwable {
                dump(method, args);
                return method.invoke(original,args);
            }
        });
        updateContextBasedHooks(context);
    }

    public static void updateContextBasedHooks(@NonNull Context context) throws NoSuchFieldException, IllegalAccessException {

        ServiceHooks.hookPackageManager(context.getPackageManager(), new ProxyListener() {
            @Override
            public Object invoke(Object original, Object proxy, Method method, Object[] args) throws Throwable {
                dump(method, args);
                if(method.getName().equals("setComponentEnabledSetting")){
                    LogExtensionsKt.log("Suppress setComponentEnabledSetting");
                    return null;
                }
                return method.invoke(original,args);
            }
        });
    }
}

package com.example.network;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Objects;



public class ClassFactory {
    private static final String TAG = "ClassFactory";

    public static <T> Class<T> findClass(Object obj){
        return (Class<T>)(obj.getClass().getTypeParameters()[0]).getBounds()[0];
    }

    public static <T> T create(Api.Request<T> obj){
//        Type type = ((ParameterizedType) Objects.requireNonNull(obj.getClass().getGenericSuperclass())).getActualTypeArguments()[0];
//        Log.d(TAG, "create: "+type);
        Class<T> clz = (Class<T>)(obj.getClass().getTypeParameters()[0]).getBounds()[0];
        for (Constructor<?> cc: clz.getDeclaredConstructors()) {
            Log.d(TAG, "create: "+cc);
            Log.d(TAG, "create: "+cc.getTypeParameters()[0]);
        }
//        try {
//            return .newInstance();
//        } catch (IllegalAccessException | InstantiationException e) {
//            e.printStackTrace();
//        }

//        for (Type type : obj.getClass().getTypeParameters()) {
//            Log.d(TAG, "create: "+type);

//            ParameterizedType pType = (ParameterizedType) type.getClass().getGenericSuperclass();
//            Log.d(TAG, "create: "+pType.getActualTypeArguments()[0]);
//        }



//        Class<?> clz = ClassUtils.getReclusiveGenericClass(obj.getClass(), 0);
//        if(clz == null) return null;
//        try {
//            Object aa = clz.newInstance();
//            Log.d(TAG, "create: ");
//            return (T) aa;
//        } catch (IllegalAccessException | InstantiationException e) {
//            e.printStackTrace();
//        }
        return null;
    }
}

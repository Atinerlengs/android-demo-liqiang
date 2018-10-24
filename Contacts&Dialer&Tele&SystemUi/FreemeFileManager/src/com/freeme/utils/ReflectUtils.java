package com.freeme.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {

    public static class ReflAgent {
        private Class<?> mClass;
        private Object mObject;
        private Object mResult;

        public static ReflAgent getClass(String clsStr) {
            ReflAgent reflAgent = new ReflAgent();
            try {
                reflAgent.mClass = Class.forName(clsStr);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return reflAgent;
        }

        public static ReflAgent getObject(Object obj) {
            ReflAgent reflAgent = new ReflAgent();
            if (obj != null) {
                reflAgent.mObject = obj;
                reflAgent.mClass  = obj.getClass();
            }
            return reflAgent;
        }

        private ReflAgent() {
        }

        public boolean booleanResult() {
            return mResult != null ? (Boolean)mResult : false;
        }

        public Object objectResult() {
            return mResult;
        }

        public int intResult() {
            return mResult != null ? (Integer)mResult : 0;
        }

        public long longResult() {
            return mResult != null ? (Long)mResult : 0;
        }

        public String stringResult() {
            return mResult != null ? mResult.toString() : null;
        }

        public ReflAgent newObject(Class<?>[] parameterTypes, Object[] values) {
            return this;
        }

        public ReflAgent call(String method, Class<?>[] parameterTypes, Object[] values) {
            if (mObject != null) {
                try {
                    mResult = ReflectUtils.callObjectMethod(mObject, method, parameterTypes, values);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            return this;
        }

        public ReflAgent callStatic(String method, Class<?>[] parameterTypes, Object[] values) {
            if (mClass != null) {
                try {
                    mResult = ReflectUtils.callStaticObjectMethod(mClass, method, parameterTypes, values);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
            return this;
        }

        public ReflAgent getObjectFiled(String field) {
            if (mObject != null) {
                try {
                    mResult = ReflectUtils.getObjectField(mObject, field);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return this;
        }

        public ReflAgent getStaticFiled(String field) {
            if (mClass != null) {
                try {
                    mResult = ReflectUtils.getStaticObjectField(mClass, field);
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                } catch (SecurityException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            return this;
        }

        public ReflAgent setResultToSelf() {
            mObject = mResult;
            mResult = null;
            return this;
        }
    }

    public static <T> T callObjectMethod(Object target, Class<T> returnType,
                                         String method, Class<?>[] parameterTypes, Object[] values)
            throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Method declaredMethod = target.getClass().getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return returnType.cast(declaredMethod.invoke(target, values));
    }

    public static Object callObjectMethod(Object target, String method,
                                          Class<?> clazz, Class<?>[] parameterTypes, Object[] values)
            throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Method declaredMethod = clazz.getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(target, values);
    }

    public static Object callObjectMethod(Object target, String method,
                                          Class<?>[] parameterTypes, Object[] values)
            throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Method declaredMethod = target.getClass().getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(target, values);
    }

    public static Object callStaticObjectMethod(Class<?> clazz,
                                                String method, Class<?>[] parameterTypes, Object[] values)
            throws NoSuchMethodException, SecurityException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
        Method declaredMethod = clazz.getDeclaredMethod(method, parameterTypes);
        declaredMethod.setAccessible(true);
        return declaredMethod.invoke(null, values);
    }

    public static Object getObjectField(Object target, String field)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field declaredField = target.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        return declaredField.get(target);
    }

    public static <T> T getObjectField(Object target, String field, Class<T> returnType)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field declaredField = target.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        return returnType.cast(declaredField.get(target));
    }

    public static Object getStaticObjectField(Class<?> clazz, String field)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        return declaredField.get(null);
    }

    public static <T> T getStaticObjectField(Class<?> clazz, String field, Class<T> returnType)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        return returnType.cast(declaredField.get(null));
    }

    public static void setObjectField(Object target, String field, Object value)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field declaredField = target.getClass().getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(target, value);
    }

    public static void setStaticObjectField(Class<?> clazz, String field, Object value)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        declaredField.set(null, value);
    }

    public static <T> T getObjectSuperField(Object target, String field, Class<T> returnType)
            throws NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        Field declaredField = target.getClass().getSuperclass().getDeclaredField(field);
        declaredField.setAccessible(true);
        return returnType.cast(declaredField.get(target));
    }

    public ReflectUtils() {
        // Cannot initiate
    }
}

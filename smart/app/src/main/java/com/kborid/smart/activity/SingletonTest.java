package com.kborid.smart.activity;

import java.util.HashMap;

public class SingletonTest {
    //1、懒汉
    public static class Singleton1 {
        private static final Singleton1 instance = new Singleton1();
        private Singleton1(){}
        public static Singleton1 getInstance() {
            return instance;
        }
    }
    //2、饿汉
    public static class Singleton2{
        private static Singleton2 instance = null;
        private Singleton2() {};
        public synchronized static Singleton2 getInstance() {
            if (null == instance) {
                instance = new Singleton2();
            }
            return instance;
        }
    }
    //3、double check
    public static class Singleton3{
        private static volatile Singleton3 instance = null;
        private Singleton3() {}
        public static Singleton3 getInstance() {
            if (null == instance) {
                synchronized (Singleton3.class) {
                    if (null == instance) {
                        instance = new Singleton3();
                    }
                }
            }
            return instance;
        }
    }
    //4、静态内部类
    public static class Singleton4{
        private static class Singleton4Instance{
            static Singleton4 instance = new Singleton4();
        }
        private Singleton4() {}
        public static Singleton4 getInstance() {
            return Singleton4Instance.instance;
        }
    }
    //5、枚举
    public enum Singleton5{
        instance;
        public String getTestName() {
            return "234";
        }
    }
    //6、容器
    public static class Singleton6{
        private static HashMap<String, Singleton6> map = new HashMap<>();
        private Singleton6(){}
        public static Singleton6 getInstance() {
            if (!map.containsKey("instance")) {
                map.put("instance", new Singleton6());
            }
            return map.get("instance");
        }
    }
}

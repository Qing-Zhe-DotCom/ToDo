package com.example;

/**
 * 打包后的桌面启动入口。
 * 避免让打包工具直接启动 JavaFX Application 子类，从而触发
 * “缺少 JavaFX 运行时组件”的经典报错。
 */
public final class PackagingLauncher {

    private PackagingLauncher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}

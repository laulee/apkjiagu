# apkjiagu
###Apk加固

**一、知识储备**

1、熟练掌握Java IO相关代码</br>
2、深入研究Android apk启动流程</br>
3、精通Multidex文件加载机制，精通类加载机制</br>
4、明确dex文件的基本构造，了解dex文件相关源码</br>
5、apk打包的基本流程需要理解 gradle工具</br>
6、掌握C/C++语言及NDK开发</br>
7、掌握Java反射和动态代理


***二、思路来源***

1、原理:来源于热修复</br>
2、微信dex加载思路

***三、粒度划分***

1、粗粒度:dex文件加密</br>
2、细粒度:dex文件里面的具体类加密

***四、具体加固流程(粗粒度)***

****1、将apk文件解压缩****</br>

****2、过滤dex文件****</br>

****3、对dex文件进行****</br>

****4、创建壳dex****</br>
        （****作用：****</br>1、迷惑敌人，暴露出去；</br>2、加载加密的dex文件）
        </br>
        
****5、通过创建module打包成aar创建dex****</br>
jar->通过dx.bat命令->dex文件
</br>

****6、将壳dex和源文件进行合并打包apk****</br>
如何在apk第一次启动时进行脱壳(热修复技术)</br>

****7、将apk文件进行sign验签****

***五、dex脱壳加载技术***

****1、apk第一次启动时通过壳application的attachBaseContext方法解密****</br>
****2、通过install方法获取dexElements[]****</br>
****3、通过hook技术进行加载外部dex包****</br>
****4、通过hook技术实现“两个”application统一成一个application****

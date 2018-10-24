[TOC]

## 部署新源代码

公司`OpenGrok`源代码索引网站：http://192.168.0.193:8080/source/

### 登陆服务器

公司的`OpenGrok`grok服务部署在`192.168.0.193`服务器的`docker`中，其`OpenGrok`代码索引目录位于`/root/sources`下。

```
$ ssh server@192.168.0.193 
输入密码后即可登陆（密码可联系运维获取，仅限leader）
```

### 添加索引

```txt
$ sudo su -
# cd /root/sources/
```

将需要添加的代码拷贝`src`目录下。比如在`OpenGrok`页面添加新的一项`android7.0`，命令如下

```txt
# mkdir src/android7.0
# cp your-src-code src/android7.0
# exit
```

### 更新`OpenGrok`索引

`OpenGrok`部署在`docker`中，因此请继续登陆`docker`，命令如下

```
$ sudo docker ps
CONTAINER ID        IMAGE               COMMAND               CREATED             STATUS              PORTS                    NAMES
04340f040aa8        opengrok            "/scripts/start.sh"   3 months ago        Up 9 weeks          0.0.0.0:8080->8080/tcp   drunk_wozniak
```

其中`04340f040aa8`即表示当前正在运行的docker的ID，执行如下命令登陆`docker`并运行shell

```
$ sudo docker exec -it 04340f040aa8 /bin/bash
```

执行索引

```
$ cd /var/opengrok
$ OpenGrok index
```

OpenGrok使用增量索引方式。一个AOSP项目，大概需要索引2-4小时，

索引完毕后退出`docker`。

```
$ exit
```

## OpenGrok配置说明

Opengrok使用docker部署，登陆docker的方法请参考本文第一节。公司的Opengrok安装目录是`/opengrok-0.12.1.5/`

### 配置OpenGrok工作目录
找到OpenGrok安装目录，修改bin/OpenGrok（编辑/opengrok-0.12.1.5/bin/OpenGrok）

```
OPENGROK_INSTANCE_BASE="${OPENGROK_INSTANCE_BASE:-/var/opengrok}"
```

### 配置tomcat

修改配置文件`/var/lib/tomcat7/webapps/source/WEB-INF/web.xml`，如下：

```
  <context-param>
    <param-name>CONFIGURATION</param-name>
    <param-value>/var/opengrok/etc/configuration.xml</param-value>
    <description>Full path to the configuration file where OpenGrok can read it's configuration</description>
  </context-param>
```

## 参考资料
- http://www.cnblogs.com/pengdonglin137/p/4717903.html
- https://github.com/openthos/openthos/wiki/opengrok%E7%8E%AF%E5%A2%83%E6%90%AD%E5%BB%BA#1%E9%9C%80%E8%A6%81apache-tomcat%E6%9D%A5%E5%90%91%E5%A4%96%E6%8F%90%E4%BE%9Bweb%E6%9C%8D%E5%8A%A1%E4%BE%9D%E8%B5%96jdk8%E4%BB%A5%E4%B8%8A%E8%AE%BE%E7%BD%AEjava_home

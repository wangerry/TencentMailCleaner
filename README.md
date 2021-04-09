# 腾讯企业邮箱自动清理
使用imap协议实现收信功能，并根据邮件时间自动清理指定天数以前的邮件
> ⚠️注意：当前仅为beta版，测试无法覆盖所有情况，请自行决定是否使用，建议先设置单个文件夹进行测试
> 
> 如有问题欢迎反馈

## 使用方法
1. clone项目后打包jar，设置环境变量后运行

2. 使用docker镜像
```shell
> docker pull registry.cn-beijing.aliyuncs.com/wangerry_bj/tencent-mail-cleaner
```

## 清理原理
默认情况下，文件夹中的邮件是按照时间排序的，1为第一封邮件，时间最早。

因此根据二分法查找到指定时间前的最后一封邮件例如1024, 则删除1-1024的邮件

>（每次删除1000封，因邮箱服务器返回较慢，删除过多可能导致超时）
 
> 本实现严格依赖邮件的顺序，如果腾讯邮件服务器顺序不正确则可能会导致删除邮件的范围不正确。

## 配置说明

| 名称 | 说明 | 默认值 | 备注 |
| ---- | ---- | ---- | ---- |
| CLEANER_HOST | 邮箱IMAP服务器地址 | imap.exmail.qq.com | 理论上可以支持其他邮箱，未测试 |
| CLEANER_USER | 邮箱用户名 | - | *<必填>* |
| CLEANER_PASS | 邮箱密码 | - | *<必填>* |
| CLEANER_FOLDERS_INCLUDES | 包含的文件夹 | - | 如果有值，将无视EXCLUDES配置 |
| CLEANER_FOLDERS_EXCLUDES | 排除的文件夹 | ["Deleted Messages", "Junk", "Drafts", "Sent Messages", "INBOX"] | 默认排除了收件、发件、垃圾箱等默认文件夹，仅包含用户自建的文件夹 |
| CLEANER_DAYS | 清理指定天数之前的邮件，全局默认配置 | 365 | 可以单独指定某个文件夹的天数，未指定则默认为该值 |
| CLEANER_RULES_NAME / CLEANER_RULES_DAYS | 指定文件夹的天数配置 | - | 指定的文件夹必须在INCLUDES和EXCLUDES规则后包含的文件夹中，否则将会被忽略。如未指定则会使用CLEANER_DAYS配置 |

## 推荐的docker命令
如果不需要任何特殊配置，可以通过docker run来执行
```shell
> docker run -it --rm -e CLEANER_USER='<用户名>' -e CLEANER_PASS='<密码>' -e CLEANER_DAYS=30 registry.cn-beijing.aliyuncs.com/wangerry_bj/tencent-mail-cleaner
```

## 推荐的docker-compose.yml
```yaml
version: '2'
  
services:
  cleaner:
    image: registry.cn-beijing.aliyuncs.com/wangerry_bj/tencent-mail-cleaner
    environment:
      CLEANER_USER: <用户名>
      CLEANER_PASS: <密码>
      CLEANER_DAYS: 30

      # 指定文件夹清理天数规则
      # CLEANER_RULES_0_NAME: Tasks
      # CLEANER_RULES_0_DAYS: 1
      # CLEANER_RULES_1_NAME: cashloan业务错误
      # CLEANER_RULES_1_DAYS: 5

      # 仅清理以下文件夹
      # CLEANER_FOLDERS_INCLUDES_0: "Tasks"
      
      # 排除以下文件夹，INCLUDES存在时将无视EXCLUDES
      # CLEANER_FOLDERS_EXCLUDES_0: "Deleted Messages"
      # CLEANER_FOLDERS_EXCLUDES_1: "Junk"
      # CLEANER_FOLDERS_EXCLUDES_2: "Drafts"
      # CLEANER_FOLDERS_EXCLUDES_3: "Sent Messages"
      # CLEANER_FOLDERS_EXCLUDES_4: "INBOX"
      # CLEANER_FOLDERS_EXCLUDES_5: "Tasks"
```

## 已知问题
- 邮件如果特别特别多，可能导致IMAP无法连接，邮箱服务器直接报错Server Busy，可以先手动删除后再使用
- 通过IMAP获取到的文件夹邮件数量与WEB中看到的不一致（原因不明）
- 执行后可能会有如下报错一次，因为连接可能还未完成，如果多次重复报错则为不正常
```shell
query folders failed, maybe is connecting, retry after 5 secs
```
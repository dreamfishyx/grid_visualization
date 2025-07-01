### :maple_leaf:写在前面

其实最开始选这个题目时，内心是完全没有底的。首先就是对于接地线这样一个东西我是完全没有概念的，其实就是设计到物联网设备的对接完全就是属于知识空白。在这个项目之前，一些课设或者小组作业其实都是做一些很简单的管理系统，涉及一些基本的增删改查而已，这个项目对我来说还是太难了。

首先就是技术上，存在太多的技术空白，例如 InfluxDB 时序数据库，之前从来就没有使用过，包括它的一些查询语法、Spring Boot 集成方式都不了解。而对于 Kafka 消息队列而言，唯一可能就是在 Redis 发布订阅部分对消息队列的流程有所了解而已。最后就是实时数据推送，最开始其实不是使用 SSE 而是使用 Webscoket ，但是参考很多博客前端项目无法与后端建立连接，自己尝试了好几天最后急得跳脚，最后放弃选用 SSE 替代，论文里还得瞎说是因为 SSE 更轻量级低能耗，哈哈。

在整个项目编写上，前端借助 cursor 编写的。前端我虽然会一些 html、css 和 js，之前自己也借助thymeleaf写过一些项目，但是没系统学过 css 设计，写出来的东西是真的丑(参考 oldbbok 项目，一个半成品)。Vue 自己也学过一段时间，但是没有具体开发过项目，所以再三思考还是借助 ai 工具设计。emmm…但是说实话结果不是很理想，生成的项目结构就很乱，甚至编写 Ajax 请求拦截器的时候总是出错。也可能是第一次使用，在问题和项目描述上不明确，以后有时间还是自己在学学前端内容。

对于后端而言一些数据库部署上采用 Docker，对于 Docker 这个软件还是比较熟悉的，之前也耍过一段时间，电脑上也装有WSL2+Arch Linux。但是对于 Kafka 的部署，真的是天坑，首先就是访问ip的设置上就有不明白，从 `win->wsl2(Arch)->docker` 这里面的网络问题太复杂了。其次就是镜像的问题，在部署完成后可以生产但是死活无法消费，最后废了好大劲查了很多资料才知道是里面有一个默认的主题没有创建(按照 deepseek 描述似乎该主题是默认自动创建的)，于是只能手动创建。

对于后端项目，尝试学习一些网上项目的标准，但是还是不太满意。我尝试使用一个 Result 返回响应数据、使用自定义状态码返回错误信息、使用全局异常处理，但是代码上还是感觉很乱，例如在 controller 中有时使用抛出异常有时返回 Result，其实现在想起来异常处理似乎在非 controller 中使用更好，因为这类方法无法直接向前端返回数据。而对于安全校验用户部分采用 Spring Security，说实话第一次用，之前都是将用户数据存储到 session 请求域中的。

说实话，这个项目到目前为止还是不尽如人意，许多的细节方面没有做好，逻辑上有些也存在问题。但其实也还好，在这个项目中，我尝试展示我会的技术，挑战一些我不会的技术，也算是为这 16 年求学路画下句号了。





### :maple_leaf:项目结构

```bash
❯ tree . -L 1 -d
.
├── backend				# 后端项目
├── device				# python模拟代码
├── doc					# 文档(ppt)
├── docker_config		# docker compose文件和一些配置文件
├── frontend			# 前端项目
└── oldbook				# 一个很老的项目半成品(顺带上传了)
```





### :maple_leaf:设计说明

##### 设备校验

为了设备注册的安全性和系统的易操作性，设备注册采用预注册和注册双重保证。当用户需要注册一个设备时，需要先在本系统中预注册设备，填写设备 ID 等信息，此后这些信息会在 Redis 的预注册临时表中保存一段时间。在这段时间内，接地线设备需要向系统发送注册请求，并携带设备 ID、经纬度、API 密钥等信息。后端接收到注册请求之后，会从系统的预注册临时表中查找预注册记录进行设备 ID 比较，数据符合则对当前设备执行注册操作，后续设备发送的数据才会被系统接收。具体的注册操作包含基于经纬度信息分析设备具体地址、设备在数据库中注册、向用户推送注册结果通知等等。

##### 维系调度

系统自动在设备每次提交数据的时候，在 Redis 心跳表中更新心跳，并同时对设备数据进行异常判别。当然系统心跳表中的数据都是存在时间限制，当系统检测到心跳表中某条记录过期时，说明设备长时间失联，此时一般说明接地线设备发生故障，需要立即以系统通知的形式提醒用户并及时更新设备状态，并且立即紧急维护该设备。此时需要判断设备所属用户是否存在空闲维修人员，若是存在则将该故障设备分配给空闲维修人员维修。若是不存在空闲维修人员，则加入待维修队列，待存在空闲维修人员时，再进行维修分配。

##### 数据传输

采用字节流传参，不携带参数名，从而提高数据传输效率并降低设备能耗。后端构建自定义参数解析器和参数标注注解解析字节流参数，提高代码的复用性和可读性。

##### 数据保障

采用 Kafka 消息队列配合 InfluxDB 接收设备数据，同时对于设备合法性采取后验证机制，降低数据丢失的可能性。







### :maple_leaf:存在问题

##### 乐观锁:

由于设备是用户和系统共同维护，考虑到若是设备在维修人员维修过程中，若被用户删除会可能导致系统故障。具体情况如下:分配维修人员维修(系统)->设备删除(用户)->维修人员维修完毕(系统)->报错。

于是自然就想到在维修期间不允许对设备进行删除操作，类似于 Redis 里面的乐观锁一样，具体的代码如下所示:

```java
@Override
@Transactional
public void deleteDevice(Integer deviceId) {
    // 判断设备是正在被占用(是否已经分配维修)
    if (maintenanceRecordService.isDeviceOccupancy(deviceId)) {
        throw new DbOperationFailedException(ErrorCode.DB_OPERATION_FAILED, "设备正在被占用,无法删除！");
    }

    // 获取乐观锁:开始尝试删除设备，此期间若是发生维修就会由于乐观锁导致删除失败
    Device device = findByDeviceId(deviceId);
    if (device == null) {
        return;
    }

    // 删除设备
    Integer i = deviceMapper.deleteDevice(device);
    // 回滚
    if (i == 0) {
        throw new DbOperationFailedException(ErrorCode.DB_OPERATION_FAILED, "设备正在被占用,暂时无法删除");
    } else {
        // sendUpdateMessage(getUserIdByDeviceId(device.getUserId()));
        // 在事务提交后触发 SSE 通知
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendUpdateMessage(getUserIdByDeviceId(device.getUserId()));
                }
            }
        );
    }

    // 删除设备相关维修记录
    maintenanceRecordService.deleteRecordWithDeviceId(deviceId);

    // 删除设备缓存
    redisTemplate.delete(RedisKeyPrefix.DEVICE_CACHE.generateFullKey(deviceId + ""));
}
```

当设备被分配维修时，设备不允许删除。否则获取当前 version 值，若是此后到删除完成之前该设备被分配维修就会导致 vesion 字段更新，此后删除操作就会失败回滚。而对于维修操作代码如下：

```java
@Override
@Transactional
public void doMaintenance(Integer deviceId) {
    //获取设备所属用户
    Integer uid = getUserIdByDeviceId(deviceId);
    if (uid == null) {
        return;
    }

    // 获取设备乐观锁:
    Device device = findByDeviceId(deviceId);
    if (device == null) {
        // 设备在此期间被误删
        return;
    }

    // 尝试分配一个维修人员
    Worker worker = workerService.assignWorker();
    if (worker == null) {
        // 将设备加入维修队列
        redisTemplate.opsForList().leftPush(
            RedisKeyPrefix.MAINTENANCE_QUEUE.generateFullKey(uid + ""),
            deviceId
        );
    } else {
        // 将设备分配给维修人员
        workerService.assignTask(worker, findByDeviceId(deviceId));

        // 判断在此期间，是否有过删除行为有则报错回滚
        updateDeviceStatus(device, false);
    }

}
```

首先就是获取乐观锁 version 避免后续分配维修过程中设备被删除(若是设备被删除则会导致 version 改变，此时回滚不分配维修)，然后就是判断是否在获取乐观锁其设备就已经被删除(若是也没有必要分配维修)。

但是实际上述的思考是多余的 :thinking:?因为本系统的维修记录生成是在系统发现设备故障时，此后就会生成维修记录(由于维修记录的存在，设备被占用，必不可能在分配维修过程中被删除)。但是若是将时间跨度调大，若是在维修记录创建过程中设备被删除上述代码是不起作用的。而对于 删除前判断操作发现设备未占用(系统)->系统创建维修记录->设备删除(用户)->分配维修 这种状况维修分配中发现设备不存在不会维修。

所以上述代码似乎是多余的​ :thinking:?

##### 参数解析器

对于接地线设备而言能耗是很重要的一个指标，为了提高接地线设备的数据传输效率并尽可能降低设备能耗，接地线的数据传输虽然选择我比较熟悉的 HTTP 协议，但是不同于传统浏览器的含参数名传参方式，接地线设备会将数据转换拼接为字节流传输到后端。后端需要根据约定将字节流分割补全并转换为对应类型数据。而由于后端场景对上述参数转换逻辑的频繁性，为保证项目代码的可复用性，也是出于自身挑战自我的愿景，我尝试在后端的 Spring Boot 框架中编写并注册自定义参数解析器和自定义参数注解，最终也是满足参数解析的需求。

在这个参数解析器的编写上,我尽可能的想完善它，例如参考 JSR303 参数校验实现分组校验、实现嵌套校验、通过大小端字节补全、小数缩放比等等。但是这个参数最终还是不够完美，最大的问题就是无法直接适用于基本数据类型，参数必须使用对象类型接受；此外对象类型的属性也有要求，一些复杂的属性例如数组等都没有实现。

也正如我论文中所描述的：由于开发周期和技术能力等因素限制并没有写出一个完善的参数解析器，而只是写出了一个符合当前系统的参数解析器；对于一些与当前阶段开发需求无关的特殊情况或是没有考虑的，或是考虑到了但是没有去实际解决，从长远来看或许会为后续系统拓展和再开发埋下隐患，这也算是一个很大的遗憾。

##### 状态码

对于状态码的设计使用太乱了，很多状态码的定义上都存在重合部分。​​

##### 其他不足

见[ppt](./doc/项目答辩ppt.pptx)

> 其实项目存在的问题还挺多的，自己写的自己心里有数 :sob:…


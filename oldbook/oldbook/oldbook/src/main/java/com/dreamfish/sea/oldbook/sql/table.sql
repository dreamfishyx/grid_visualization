#===创建oldbook数据库===
create database oldbook default character set utf8mb4;
use oldbook;

#===创建user表并插入初始数据===
create table user
(
    user_id     int auto_increment comment '用户id',
    password    varchar(32)                        not null comment '密码',
    user_name   varchar(30)                        not null comment '用户名',
    create_time DATETIME default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '创建时间',
    gender      tinyint  default 0                 not null comment '性别(0女1男2)',
#     question    varchar(50)                        null comment '密保问题',
#     answer      varchar(50)                        null comment '答案',
    constraint user_pk
        primary key (user_id)
);
insert into user(password, user_name)
values ('b71aaf60b59a28472fd8feeb3ae2eec9', 'root'),
       ('b71aaf60b59a28472fd8feeb3ae2eec9', 'fish');

#===创建diary表并插入初始数据===
create table diary
(
    diary_id    int auto_increment                 not null comment '自增主键',
    title       varchar(20)                        not null comment '标题',
    weather     varchar(20) comment '天气',
    mood        TINYINT  default 0                 not null comment '心情(0好、1一般、2差)',
    link        varchar(50)                        not null comment '文件位置',
    is_delete   TINYINT  default 0                 not null comment '是否删除(1删除，默认0)',
    user_id     int                                not null comment '用户id',
    create_time DATETIME default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '创建时间',
    update_time DATETIME default CURRENT_TIMESTAMP not null comment '更新时间',
    constraint diary_pk
        primary key (diary_id)
)
    comment '日记';
insert into diary(title, weather, mood, link, user_id)
values ('关于系统', '晴', 1, '1_982_1700625504183.md', 1);

#===创建plan表并插入初始数据===
create table plan
(
    plan_id       int auto_increment comment '计划主键',
    user_id       int                                not null comment '用户id',
    content       varchar(50) comment '计划内容',
    deadline      DATETIME                           null comment '截止时间',
    complete      TINYINT  default 0                 not null comment '是否完成(1完成，默认0)',
    is_delete     TINYINT  default 0                 not null comment '是否删除(1删除，默认0)',
    create_time   DATETIME default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '创建时间',
    update_time   DATETIME default CURRENT_TIMESTAMP not null comment '更新时间',
    complete_time DATETIME comment '完成时间',
    constraint plan_pk
        primary key (plan_id)
)
    comment '计划表';

insert into plan(user_id, content, deadline)
values (1, '好好学习', '2025-11-25 22:30:00'),
       (1, 'ヽ(ー_ー)ノ:皇帝的计划(无内容)', '2025-11-25 22:00:00'),
       (1, '天天向上', '2025-11-25 00:00:00');


#===创建word表并插入初始数据===
create table word
(
    word_id     int auto_increment comment '一言主键',
    user_id     int                                not null comment '用户id',
    content     varchar(50) comment '每日一言内容',
    is_delete   TINYINT  default 0                 not null comment '是否删除(1删除，默认0)',
    create_time DATETIME default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '创建时间',
    update_time DATETIME default CURRENT_TIMESTAMP not null comment '更新时间',
    constraint word_pk
        primary key (word_id)
) comment '每日一言表';
insert into word(user_id, content)
values (1, '毕竟几人真得鹿,不知终日梦为鱼。'),
       (1, '遇事不决,可问春风;春风不语,即遵本心。'),
       (1, '知命不惧,日日自新！'),
       (1, '千年暗室,一灯即明。'),
       (1, '眼前无常物,窗外有清风。');

# drop database oldbook;
# drop table user,word,plan,diary;

# create table image
# (
#     image_id    int auto_increment comment '图片主键',
#     link        varchar(50)                        not null comment '图片链接',
#     is_delete   TINYINT  default 0                 not null comment '是否删除(1删除，默认0)',
#     create_time DATETIME default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '创建时间',
#     update_time DATETIME default CURRENT_TIMESTAMP not null comment '更新时间',
#     constraint image_pk
#         primary key (image_id)
# )
#     comment '图片表';

set names utf8mb4;

use volunteer_service;

create table if not exists credit_rule (
    id bigint primary key auto_increment,
    code varchar(50) not null unique,
    name varchar(80) not null,
    change_value int not null,
    enabled tinyint(1) not null default 1,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp
) comment='信用分加减规则';

create table if not exists credit_record (
    id bigint primary key auto_increment,
    user_id bigint not null,
    change_value int not null,
    reason varchar(160) not null,
    source_type varchar(40),
    source_id bigint,
    created_at datetime not null default current_timestamp
) comment='信用分变更记录';

create table if not exists activity_evaluation (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    evaluator_id bigint not null,
    target_user_id bigint,
    target_type varchar(30) not null,
    score int not null,
    content varchar(500),
    created_at datetime not null default current_timestamp,
    unique key uk_activity_eval(activity_id,evaluator_id,target_type,target_user_id)
) comment='活动互评';

create table if not exists checkin_adjustment (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    user_id bigint not null,
    original_status varchar(30),
    original_checkin_time datetime,
    reason varchar(160),
    description varchar(500),
    proof_image_url varchar(700),
    original_service_hours decimal(8,2),
    audit_status varchar(30) not null default 'PENDING',
    new_status varchar(30),
    new_checkin_time datetime,
    new_service_hours decimal(8,2),
    hours_reason varchar(255),
    admin_remark varchar(255),
    admin_id bigint,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    index idx_checkin_adjustment_status(audit_status),
    index idx_checkin_adjustment_activity(activity_id)
) comment='签到异常与补签申请';

create table if not exists announcement (
    id bigint primary key auto_increment,
    title varchar(200) not null,
    content text not null,
    status varchar(20) not null default 'DRAFT',
    created_by bigint not null,
    published_at datetime null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    index idx_announcement_status(status),
    index idx_announcement_published(published_at)
) comment='公告';

create table if not exists announcement_image (
    id bigint primary key auto_increment,
    announcement_id bigint not null,
    image_url varchar(700) not null,
    sort_order int not null default 0,
    created_at datetime not null default current_timestamp,
    index idx_announcement_image(announcement_id)
) comment='公告图片';

create table if not exists announcement_attachment (
    id bigint primary key auto_increment,
    announcement_id bigint not null,
    rule_file_id bigint not null,
    created_at datetime not null default current_timestamp,
    unique key uk_announcement_rule_file(announcement_id, rule_file_id),
    index idx_announcement_attachment(announcement_id)
) comment='公告附件';

create table if not exists rule_file (
    id bigint primary key auto_increment,
    original_name varchar(255) not null,
    file_type varchar(20) not null,
    file_size bigint not null,
    s3_key varchar(500) not null,
    s3_url varchar(700) not null,
    status varchar(30) not null default 'READY',
    chunk_count int not null default 0,
    created_by bigint not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    index idx_rule_file_status(status)
) comment='规则文件元数据';

create table if not exists chat_block (
    id bigint primary key auto_increment,
    blocker_id bigint not null,
    blocked_user_id bigint not null,
    created_at datetime not null default current_timestamp,
    unique key uk_chat_block_pair(blocker_id, blocked_user_id),
    index idx_chat_block_blocked(blocked_user_id)
) comment='聊天拉黑关系';

create table if not exists ai_report (
    id bigint primary key auto_increment,
    report_no varchar(80) not null,
    report_type varchar(20) not null,
    user_id bigint not null,
    period_start varchar(20),
    period_end varchar(20),
    stats_json longtext not null,
    ai_analysis longtext,
    pdf_url varchar(700),
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_ai_report_no(report_no),
    index idx_ai_report_user(user_id, report_type, created_at)
) comment='AI分析报告';

set foreign_key_checks = 0;

truncate table ai_report;
truncate table chat_block;
truncate table announcement_attachment;
truncate table announcement_image;
truncate table announcement;
truncate table rule_file;
truncate table activity_evaluation;
truncate table credit_record;
truncate table credit_rule;
truncate table checkin_adjustment;
truncate table activity_checkin;
truncate table chat_message;
truncate table chat_conversation;
truncate table notification;
truncate table activity_news_image;
truncate table activity_news;
truncate table service_record;
truncate table registration;
truncate table activity;
truncate table volunteer_profile;
truncate table user;

set foreign_key_checks = 1;

insert into user(id,username,password,name,nickname,identity_no,phone,role,created_at) values
(1,'admin','123456','学院管理员','学院管理员','T0001','13800000000','ADMIN','2026-05-01 09:00:00'),
(2,'20240001','123456','林一凡','林一凡','20240001','13811111111','VOLUNTEER','2026-05-01 09:10:00'),
(3,'20240002','123456','陈雨晴','陈雨晴','20240002','13822222222','VOLUNTEER','2026-05-01 09:12:00'),
(4,'20240003','123456','周子涵','周子涵','20240003','13833333333','VOLUNTEER','2026-05-01 09:14:00'),
(5,'20240004','123456','王浩然','王浩然','20240004','13844444444','VOLUNTEER','2026-05-01 09:16:00'),
(6,'20240005','123456','赵思源','赵思源','20240005','13855555555','VOLUNTEER','2026-05-01 09:18:00'),
(7,'20240006','123456','刘佳宁','刘佳宁','20240006','13866666666','VOLUNTEER','2026-05-01 09:20:00'),
(8,'20240007','123456','孙明哲','孙明哲','20240007','13877777777','VOLUNTEER','2026-05-01 09:22:00'),
(9,'20240008','123456','何若曦','何若曦','20240008','13888888888','VOLUNTEER','2026-05-01 09:24:00');

insert into volunteer_profile(user_id,college,major_class,skill_tags,available_time,bio,total_hours,credit_score,service_count) values
(2,'数计学院','软件工程2401','摄影,文案,秩序维护','周末全天,工作日晚上','熟悉活动摄影、现场引导和推文整理，能快速完成志愿服务记录。',18.0,98,5),
(3,'数计学院','计算机科学2402','讲解,摄像,秩序维护','工作日下午,周末上午','表达清晰，适合承担展厅讲解、嘉宾接待和现场协助。',12.5,95,4),
(4,'数计学院','数据科学2301','文案,活动组织,讲解','周末全天','参与过学院开放日和竞赛志愿服务，擅长流程协调。',25.0,100,7),
(5,'数计学院','网络工程2302','物资搬运,秩序维护,后勤保障','工作日晚上,周末下午','负责过物资分发、场地布置和秩序维护，执行力稳定。',9.0,92,3),
(6,'数计学院','人工智能2401','编程,讲解,设备调试','周三下午,周末全天','熟悉机房设备和竞赛环境，适合技术支持类志愿服务。',7.0,96,2),
(7,'数计学院','软件工程2303','摄影,新媒体运营,文案','周末上午,工作日晚上','能完成照片拍摄、活动短文和公众号素材整理。',16.0,97,5),
(8,'数计学院','计算机科学2301','秩序维护,后勤保障,活动组织','周五下午,周末全天','熟悉大型活动现场分工，适合担任小组负责人。',20.5,94,6),
(9,'数计学院','数据科学2402','讲解,文案,数据统计','周二下午,周末下午','沟通耐心，能协助活动数据统计与总结归档。',6.5,99,2);

insert into activity(id,name,cover_image_url,category,location,latitude,longitude,start_time,end_time,signup_start_time,signup_deadline,checkin_start_time,checkin_end_time,recruit_number,registered_number,skill_requirements,description,signup_requirement,contact_name,contact_phone,service_hours,review_method,status,created_by,finished_at,published_at,created_at) values
(1,'数计学院开放日讲解服务','https://images.unsplash.com/photo-1523580846011-d3a5bc25702b?auto=format&fit=crop&w=1200&q=80','校园讲解','数计学院展厅',31.230416,121.473701,'2026-06-06 09:00:00','2026-06-06 12:00:00','2026-05-28 09:00:00','2026-06-05 18:00:00','2026-06-06 08:30:00','2026-06-06 09:30:00',12,3,'讲解,摄影,秩序维护','面向来访师生和家长介绍学院专业特色、实验室成果和学生培养情况，协助完成路线引导与现场答疑。','普通话清晰，熟悉学院基本情况，报名后需参加一次线上培训。','王老师','13800003333',3.0,'人工审核','报名中',1,null,'2026-05-28 09:00:00','2026-05-28 09:00:00'),
(2,'程序设计竞赛赛务支持','https://images.unsplash.com/photo-1515879218367-8466d910aaa4?auto=format&fit=crop&w=1200&q=80','赛事保障','实验楼三楼机房',31.231100,121.474200,'2026-06-12 13:00:00','2026-06-12 18:00:00','2026-05-29 08:00:00','2026-06-10 20:00:00','2026-06-12 12:30:00','2026-06-12 13:30:00',10,4,'编程,设备调试,秩序维护','协助竞赛机房检查、参赛签到、设备问题登记、现场秩序维护和赛后资料整理。','需提前 30 分钟到场，熟悉机房基础设备者优先。','周老师','13800002222',5.0,'人工审核','报名中',1,null,'2026-05-29 08:00:00','2026-05-29 08:00:00'),
(3,'校园环保宣传志愿活动','https://images.unsplash.com/photo-1542601906990-b4d3fb778b09?auto=format&fit=crop&w=1200&q=80','社区服务','学校南门广场',31.229700,121.472500,'2026-06-08 15:00:00','2026-06-08 17:30:00','2026-05-27 10:00:00','2026-06-07 18:00:00','2026-06-08 14:40:00','2026-06-08 15:20:00',16,2,'文案,摄影,秩序维护','开展环保知识宣传、垃圾分类小游戏、宣传单发放和现场互动记录。','认真负责，能主动与同学沟通。','刘老师','13800001111',2.5,'自动通过','报名中',1,null,'2026-05-27 10:00:00','2026-05-27 10:00:00'),
(4,'毕业季行李搬运志愿服务','https://images.unsplash.com/photo-1529156069898-49953e39b3ac?auto=format&fit=crop&w=1200&q=80','校园服务','学生公寓 6 栋',31.228900,121.471900,'2026-06-18 08:30:00','2026-06-18 11:30:00','2026-05-30 08:00:00','2026-06-16 18:00:00','2026-06-18 08:10:00','2026-06-18 09:00:00',18,1,'物资搬运,后勤保障,秩序维护','协助毕业生搬运行李、维持车辆临停秩序，并完成服务点位信息登记。','身体状况良好，能服从现场调度。','陈老师','13800004444',3.0,'人工审核','报名中',1,null,'2026-05-30 08:00:00','2026-05-30 08:00:00'),
(5,'新生编程体验课助教','https://images.unsplash.com/photo-1522202176988-66273c2fd55f?auto=format&fit=crop&w=1200&q=80','校园讲解','智慧教室 A201',31.230900,121.474900,'2026-06-22 14:00:00','2026-06-22 17:00:00','2026-05-30 12:00:00','2026-06-20 20:00:00','2026-06-22 13:30:00','2026-06-22 14:20:00',8,0,'编程,讲解,设备调试','为新生体验课提供环境配置、代码运行指导和课堂秩序协助。','有基础编程经验，耐心细致。','唐老师','13800005555',3.0,'人工审核','已发布',1,null,'2026-05-30 12:00:00','2026-05-30 12:00:00'),
(6,'学院运动会志愿服务','https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1200&q=80','赛事保障','学校田径场',31.227800,121.470800,'2026-05-18 08:00:00','2026-05-18 17:00:00','2026-05-01 09:00:00','2026-05-16 18:00:00','2026-05-18 07:30:00','2026-05-18 08:30:00',20,5,'秩序维护,摄影,后勤保障','协助运动会检录、物资发放、秩序维护和影像记录。','按时到岗，全天服务。','马老师','13800006666',8.0,'人工审核','已结束',1,'2026-05-18 17:10:00','2026-05-01 09:00:00','2026-05-01 09:00:00'),
(7,'社区智慧助老志愿服务','https://images.unsplash.com/photo-1581579438747-104c53d7fbc4?auto=format&fit=crop&w=1200&q=80','社区服务','阳光社区服务中心',31.226900,121.469900,'2026-05-24 09:00:00','2026-05-24 11:30:00','2026-05-10 09:00:00','2026-05-22 18:00:00','2026-05-24 08:40:00','2026-05-24 09:20:00',10,4,'讲解,耐心沟通,设备调试','帮助社区老人学习手机常用功能、反诈知识和线上挂号流程。','沟通耐心，能一对一指导。','胡老师','13800007777',2.5,'人工审核','已结束',1,'2026-05-24 11:40:00','2026-05-10 09:00:00','2026-05-10 09:00:00'),
(8,'学院资料归档与数据整理','https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=1200&q=80','校园服务','学院办公室 B305',31.230100,121.473300,'2026-05-10 14:00:00','2026-05-10 17:00:00','2026-04-26 09:00:00','2026-05-09 18:00:00','2026-05-10 13:40:00','2026-05-10 14:20:00',6,3,'文案,数据统计,活动组织','协助整理活动报名表、服务记录和照片素材，完成资料归档。','细心认真，能使用表格工具。','许老师','13800008888',3.0,'自动通过','已结束',1,'2026-05-10 17:05:00','2026-04-26 09:00:00','2026-04-26 09:00:00');

insert into registration(id,activity_id,user_id,status,review_remark,created_at,updated_at) values
(1,1,2,'已通过','讲解岗位，通过审核。','2026-05-28 10:20:00','2026-05-28 11:00:00'),
(2,1,3,'已通过','摄影记录岗位，通过审核。','2026-05-28 10:26:00','2026-05-28 11:02:00'),
(3,1,4,'待审核',null,'2026-05-30 09:30:00','2026-05-30 09:30:00'),
(4,2,6,'已通过','技术支持岗位，通过审核。','2026-05-29 09:05:00','2026-05-29 10:00:00'),
(5,2,8,'已通过','赛务保障岗位，通过审核。','2026-05-29 09:15:00','2026-05-29 10:05:00'),
(6,2,5,'待审核',null,'2026-05-30 10:12:00','2026-05-30 10:12:00'),
(7,2,9,'待审核',null,'2026-05-30 10:16:00','2026-05-30 10:16:00'),
(8,3,7,'已通过','宣传记录岗位，自动通过。','2026-05-27 13:00:00','2026-05-27 13:00:00'),
(9,3,2,'已通过','现场引导岗位，自动通过。','2026-05-27 14:10:00','2026-05-27 14:10:00'),
(10,4,5,'已通过','后勤保障岗位，通过审核。','2026-05-30 08:40:00','2026-05-30 09:10:00'),
(11,6,2,'已完成','表现优秀，完成全天服务。','2026-05-02 10:00:00','2026-05-18 17:30:00'),
(12,6,3,'已完成','完成摄影记录。','2026-05-02 10:05:00','2026-05-18 17:30:00'),
(13,6,4,'已完成','负责检录协助。','2026-05-02 10:10:00','2026-05-18 17:30:00'),
(14,6,5,'已完成','完成物资保障。','2026-05-02 10:15:00','2026-05-18 17:30:00'),
(15,6,8,'已通过','系统识别迟到，待管理员确认。','2026-05-02 10:20:00','2026-05-18 17:30:00'),
(16,7,3,'已完成','耐心指导社区老人。','2026-05-11 09:00:00','2026-05-24 12:00:00'),
(17,7,6,'已完成','完成设备调试指导。','2026-05-11 09:05:00','2026-05-24 12:00:00'),
(18,7,8,'已完成','现场组织有序。','2026-05-11 09:08:00','2026-05-24 12:00:00'),
(19,7,9,'已完成','完成资料统计。','2026-05-11 09:12:00','2026-05-24 12:00:00'),
(20,8,4,'已完成','完成资料归档。','2026-04-27 09:00:00','2026-05-10 17:20:00'),
(21,8,7,'已完成','完成照片素材整理。','2026-04-27 09:10:00','2026-05-10 17:20:00'),
(22,8,9,'已完成','完成数据表整理。','2026-04-27 09:20:00','2026-05-10 17:20:00');

insert into activity_checkin(activity_id,user_id,status,checkin_time,method,latitude,longitude,distance_meters,manual_admin_id,manual_time,manual_reason,created_at,updated_at) values
(6,2,'CHECKED_IN','2026-05-18 07:51:00','LOCATION',31.227810,121.470790,18.60,null,null,null,'2026-05-18 07:51:00','2026-05-18 07:51:00'),
(6,3,'CHECKED_IN','2026-05-18 07:55:00','LOCATION',31.227820,121.470805,20.30,null,null,null,'2026-05-18 07:55:00','2026-05-18 07:55:00'),
(6,4,'CHECKED_IN','2026-05-18 08:02:00','LOCATION',31.227790,121.470780,25.10,null,null,null,'2026-05-18 08:02:00','2026-05-18 08:02:00'),
(6,5,'MANUAL_CHECKED_IN','2026-05-18 08:10:00','MANUAL',null,null,null,1,'2026-05-18 08:20:00','现场设备网络异常，管理员补签。','2026-05-18 08:20:00','2026-05-18 08:20:00'),
(6,8,'LATE_CHECKED_IN','2026-05-18 08:46:00','LOCATION',31.227840,121.470820,31.40,null,null,null,'2026-05-18 08:46:00','2026-05-18 08:46:00'),
(7,3,'CHECKED_IN','2026-05-24 08:50:00','LOCATION',31.226910,121.469890,15.20,null,null,null,'2026-05-24 08:50:00','2026-05-24 08:50:00'),
(7,6,'CHECKED_IN','2026-05-24 08:54:00','LOCATION',31.226900,121.469910,11.80,null,null,null,'2026-05-24 08:54:00','2026-05-24 08:54:00'),
(7,8,'CHECKED_IN','2026-05-24 08:57:00','LOCATION',31.226880,121.469920,19.50,null,null,null,'2026-05-24 08:57:00','2026-05-24 08:57:00'),
(7,9,'CHECKED_IN','2026-05-24 09:01:00','LOCATION',31.226930,121.469930,28.40,null,null,null,'2026-05-24 09:01:00','2026-05-24 09:01:00'),
(8,4,'CHECKED_IN','2026-05-10 13:45:00','LOCATION',31.230110,121.473320,12.60,null,null,null,'2026-05-10 13:45:00','2026-05-10 13:45:00'),
(8,7,'CHECKED_IN','2026-05-10 13:48:00','LOCATION',31.230090,121.473310,16.00,null,null,null,'2026-05-10 13:48:00','2026-05-10 13:48:00'),
(8,9,'CHECKED_IN','2026-05-10 13:52:00','LOCATION',31.230120,121.473290,18.20,null,null,null,'2026-05-10 13:52:00','2026-05-10 13:52:00');

insert into checkin_adjustment(activity_id,user_id,original_status,original_checkin_time,reason,description,original_service_hours,audit_status,new_status,new_checkin_time,new_service_hours,hours_reason,admin_remark,admin_id,created_at,updated_at) values
(6,8,'LATE_CHECKED_IN','2026-05-18 08:46:00','公交临时改道导致迟到','已上传到场说明，申请按实际服务时长核定。',8.0,'PENDING',null,null,null,null,null,null,'2026-05-18 19:20:00','2026-05-18 19:20:00');

insert into service_record(user_id,activity_id,hours,comment,created_at) values
(2,6,8.0,'学院运动会全天志愿服务','2026-05-18 17:30:00'),
(3,6,8.0,'学院运动会摄影记录','2026-05-18 17:30:00'),
(4,6,8.0,'学院运动会检录协助','2026-05-18 17:30:00'),
(5,6,8.0,'学院运动会物资保障','2026-05-18 17:30:00'),
(3,7,2.5,'社区智慧助老志愿服务','2026-05-24 12:00:00'),
(6,7,2.5,'社区智慧助老设备指导','2026-05-24 12:00:00'),
(8,7,2.5,'社区智慧助老现场组织','2026-05-24 12:00:00'),
(9,7,2.5,'社区智慧助老资料统计','2026-05-24 12:00:00'),
(4,8,3.0,'学院资料归档','2026-05-10 17:20:00'),
(7,8,3.0,'活动照片素材整理','2026-05-10 17:20:00'),
(9,8,3.0,'服务数据表整理','2026-05-10 17:20:00');

insert into credit_rule(code,name,change_value,enabled) values
('LEADER_GOOD_REVIEW','负责人五星评价',2,1),
('LEADER_BAD_REVIEW','负责人低分评价',-5,1),
('ABSENT','活动缺勤',-10,1),
('LATE_CHECKIN','迟到签到',-3,1),
('MANUAL_RECOVER','管理员修复信用',5,1);

insert into credit_record(user_id,change_value,reason,source_type,source_id,created_at) values
(8,-3,'学院运动会迟到签到，待异常处理确认','CHECKIN',15,'2026-05-18 18:00:00'),
(5,-3,'活动前临时调整岗位，管理员已提醒','ADMIN',14,'2026-05-18 18:10:00'),
(4,2,'负责人五星评价','EVALUATION',20,'2026-05-10 18:00:00');

insert into activity_evaluation(activity_id,evaluator_id,target_user_id,target_type,score,content,created_at) values
(6,1,2,'VOLUNTEER',5,'到岗及时，现场引导主动。','2026-05-18 18:20:00'),
(6,1,3,'VOLUNTEER',5,'照片记录完整，素材质量高。','2026-05-18 18:22:00'),
(7,3,null,'ACTIVITY',5,'活动组织清晰，服务对象反馈很好。','2026-05-24 13:00:00'),
(8,1,4,'VOLUNTEER',5,'资料整理准确，效率高。','2026-05-10 18:00:00');

insert into activity_news(id,activity_id,title,content,result_summary,status,read_count,created_by,published_at,created_at,updated_at) values
(1,6,'数计学院运动会志愿服务圆满完成','5 月 18 日，数计学院志愿者参与学校运动会服务保障，承担检录引导、物资分发、现场秩序维护和影像记录等工作。志愿者们按时到岗、分工协作，为赛事顺利进行提供了有力支持。','志愿者完成检录引导、物资保障和影像记录，展现了数计学子的服务担当。','PUBLISHED',128,1,'2026-05-19 10:00:00','2026-05-19 09:20:00','2026-05-19 10:00:00'),
(2,7,'社区智慧助老服务走进阳光社区','数计学院志愿者走进阳光社区，围绕手机常用功能、反诈提醒和线上挂号流程开展一对一指导。活动现场互动热烈，社区老人对志愿者的耐心讲解给予好评。','活动帮助社区老人掌握常用数字工具，推进专业能力服务社会。','PUBLISHED',96,1,'2026-05-25 09:30:00','2026-05-25 09:00:00','2026-05-25 09:30:00'),
(3,8,'学院资料归档志愿服务完成阶段整理','志愿者协助学院办公室完成活动报名表、服务记录和照片素材归档，进一步规范学院志愿服务资料管理。','志愿者完成资料归档和数据整理，为后续统计分析打好基础。','PUBLISHED',74,1,'2026-05-11 11:00:00','2026-05-11 10:30:00','2026-05-11 11:00:00');

insert into activity_news_image(news_id,image_url,sort_order) values
(1,'https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=1200&q=80',0),
(2,'https://images.unsplash.com/photo-1581579438747-104c53d7fbc4?auto=format&fit=crop&w=1200&q=80',0),
(3,'https://images.unsplash.com/photo-1450101499163-c8848c66ca85?auto=format&fit=crop&w=1200&q=80',0);

insert into announcement(id,title,content,status,created_by,published_at,created_at,updated_at) values
(1,'关于规范志愿服务签到的通知','为保证志愿服务时长统计准确，请已通过报名的同学在活动签到时段内到达指定地点完成定位签到。如遇网络异常或特殊情况，请在活动结束后 24 小时内提交签到异常申请。','PUBLISHED',1,'2026-05-26 10:00:00','2026-05-26 09:30:00','2026-05-26 10:00:00'),
(2,'六月志愿服务活动报名提醒','六月学院将开展开放日讲解、程序设计竞赛赛务支持、校园环保宣传等志愿服务，请同学们结合技能标签和可服务时间报名。','PUBLISHED',1,'2026-05-30 08:30:00','2026-05-30 08:00:00','2026-05-30 08:30:00');

insert into rule_file(id,original_name,file_type,file_size,s3_key,s3_url,status,chunk_count,created_by,created_at,updated_at) values
(1,'数计学院志愿服务积分规则.pdf','pdf',245760,'demo/rules/volunteer-points.pdf','https://example.com/demo/volunteer-points.pdf','READY',8,1,'2026-05-20 10:00:00','2026-05-20 10:00:00'),
(2,'活动签到与异常处理说明.docx','docx',184320,'demo/rules/checkin-adjustment.docx','https://example.com/demo/checkin-adjustment.docx','READY',6,1,'2026-05-21 10:00:00','2026-05-21 10:00:00');

insert into announcement_attachment(announcement_id,rule_file_id) values
(1,2),
(2,1);

insert into chat_conversation(id,user_a_id,user_b_id,type,last_message,last_message_at,created_at,updated_at) values
(1,1,2,'PRIVATE','开放日讲解服务已通过审核，请记得参加培训。','2026-05-28 11:05:00','2026-05-28 11:00:00','2026-05-28 11:05:00'),
(2,1,6,'PRIVATE','程序设计竞赛赛务支持已通过审核，赛前请提前测试机房环境。','2026-05-29 10:10:00','2026-05-29 10:00:00','2026-05-29 10:10:00');

insert into chat_message(id,conversation_id,sender_id,receiver_id,type,content,activity_id,image_url,invite_status,read_at,created_at) values
(1,1,1,2,'TEXT','开放日讲解服务已通过审核，请记得参加培训。',1,null,null,null,'2026-05-28 11:05:00'),
(2,2,1,6,'TEXT','程序设计竞赛赛务支持已通过审核，赛前请提前测试机房环境。',2,null,null,null,'2026-05-29 10:10:00'),
(3,1,1,2,'ACTIVITY_INVITE','邀请你参加校园环保宣传志愿活动，适合你的摄影和文案技能。',3,null,'PENDING',null,'2026-05-30 09:40:00');

insert into notification(user_id,type,title,content,target_type,target_id,read_at,created_at) values
(2,'REGISTRATION','报名审核通过','你报名的“数计学院开放日讲解服务”已通过审核。','ACTIVITY',1,null,'2026-05-28 11:00:00'),
(3,'REGISTRATION','报名审核通过','你报名的“数计学院开放日讲解服务”已通过审核。','ACTIVITY',1,'2026-05-28 12:00:00','2026-05-28 11:02:00'),
(4,'NEWS','活动新闻发布','“学院资料归档志愿服务完成阶段整理”已发布。','NEWS',3,null,'2026-05-11 11:00:00'),
(8,'CHECKIN','签到异常提醒','系统识别到你在“学院运动会志愿服务”中迟到签到，可在我的报名中提交异常说明。','ACTIVITY',6,null,'2026-05-18 18:00:00'),
(2,'ANNOUNCEMENT','六月志愿服务活动报名提醒','六月学院将开展多场志愿服务活动，请结合个人时间报名。','ANNOUNCEMENT',2,null,'2026-05-30 08:30:00');

insert into ai_report(report_no,report_type,user_id,period_start,period_end,stats_json,ai_analysis,pdf_url,created_at,updated_at) values
('VOL-202405-20240001','VOLUNTEER',2,'2026-05-01','2026-05-31','{"totalHours":18,"activityCount":3,"completedCount":1}','本月参与活动类型覆盖赛事保障和资料整理，现场执行稳定。建议继续强化讲解能力，优先报名开放日和环保宣传类活动。',null,'2026-05-29 15:00:00','2026-05-29 15:00:00'),
('ADM-202405','ADMIN',1,'2026-05-01','2026-05-31','{"activityCount":8,"volunteerCount":8,"registrationCount":22,"totalHours":114.5}','本月志愿服务活动类型较均衡，赛事保障和社区服务参与度高。建议对六月开放日和竞赛服务提前完成人员确认，并重点关注签到异常处理效率。',null,'2026-05-29 16:00:00','2026-05-29 16:00:00');

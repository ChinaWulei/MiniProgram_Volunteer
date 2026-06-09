use volunteer_service;

alter table volunteer_profile add column campus varchar(50) null after college;
alter table volunteer_profile add column department varchar(80) null after campus;

create table if not exists activity_position (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    name varchar(100) not null,
    start_time datetime not null,
    end_time datetime not null,
    recruit_number int not null,
    requirements varchar(500),
    requires_rehearsal tinyint(1) not null default 0,
    rehearsal_start_time datetime null,
    rehearsal_end_time datetime null,
    sort_order int not null default 0,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    constraint fk_position_activity foreign key(activity_id) references activity(id) on delete cascade,
    index idx_position_activity(activity_id, sort_order)
);

create table if not exists exam_schedule (
    id bigint primary key auto_increment,
    user_id bigint not null,
    course_name varchar(120) not null,
    start_time datetime not null,
    end_time datetime not null,
    location varchar(120),
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    constraint fk_exam_user foreign key(user_id) references user(id) on delete cascade,
    index idx_exam_user_time(user_id, start_time, end_time)
);

alter table registration add column position_id bigint null after user_id;
alter table registration add column transport_required tinyint(1) not null default 0 after review_remark;
alter table registration add column boarding_point varchar(120) null after transport_required;

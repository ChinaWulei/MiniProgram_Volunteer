use volunteer_service;

create table if not exists course_schedule (
    id bigint primary key auto_increment,
    user_id bigint not null,
    course_name varchar(120) not null,
    weekday int not null,
    start_time time not null,
    end_time time not null,
    location varchar(120),
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    constraint fk_course_user foreign key(user_id) references user(id) on delete cascade,
    index idx_course_user_weekday(user_id, weekday, start_time, end_time)
);

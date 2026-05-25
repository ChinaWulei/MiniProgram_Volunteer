set names utf8mb4;
use volunteer_service;

alter table activity add column finished_at datetime null after created_by;

create table if not exists activity_news (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    title varchar(160) not null,
    content text,
    result_summary varchar(500),
    status varchar(20) not null default 'DRAFT',
    read_count int not null default 0,
    created_by bigint not null,
    published_at datetime,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp
);

create table if not exists activity_news_image (
    id bigint primary key auto_increment,
    news_id bigint not null,
    image_url varchar(500) not null,
    sort_order int not null default 0,
    created_at datetime not null default current_timestamp
);

alter table notification
    add column target_type varchar(40) null after content,
    add column target_id bigint null after target_type;

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
);

create table if not exists announcement_image (
    id bigint primary key auto_increment,
    announcement_id bigint not null,
    image_url varchar(700) not null,
    sort_order int not null default 0,
    created_at datetime not null default current_timestamp,
    index idx_announcement_image(announcement_id)
);

create table if not exists announcement_attachment (
    id bigint primary key auto_increment,
    announcement_id bigint not null,
    rule_file_id bigint not null,
    created_at datetime not null default current_timestamp,
    unique key uk_announcement_rule_file(announcement_id, rule_file_id),
    index idx_announcement_attachment(announcement_id)
);

set names utf8mb4;
use volunteer_service;

create table if not exists rule_file (
    id bigint primary key auto_increment,
    original_name varchar(255) not null,
    file_type varchar(20) not null,
    file_size bigint not null,
    s3_key varchar(500) not null,
    s3_url varchar(700) not null,
    status varchar(30) not null default 'PROCESSING',
    chunk_count int not null default 0,
    created_by bigint not null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    index idx_rule_file_status(status),
    constraint fk_rule_file_creator foreign key(created_by) references user(id)
) comment='规则文件元数据';

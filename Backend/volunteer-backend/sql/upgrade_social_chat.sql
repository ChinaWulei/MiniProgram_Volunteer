set names utf8mb4;
use volunteer_service;

create table if not exists chat_conversation (
    id bigint primary key auto_increment,
    user_a_id bigint not null,
    user_b_id bigint,
    type varchar(30) not null default 'PRIVATE',
    last_message varchar(500),
    last_message_at datetime,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_private_pair(user_a_id, user_b_id, type)
);

create table if not exists chat_message (
    id bigint primary key auto_increment,
    conversation_id bigint not null,
    sender_id bigint not null,
    receiver_id bigint not null,
    type varchar(30) not null default 'TEXT',
    content varchar(1000),
    activity_id bigint,
    invite_status varchar(20),
    read_at datetime,
    created_at datetime not null default current_timestamp
);

create table if not exists notification (
    id bigint primary key auto_increment,
    user_id bigint not null,
    type varchar(40) not null,
    title varchar(120) not null,
    content varchar(500),
    read_at datetime,
    created_at datetime not null default current_timestamp
);

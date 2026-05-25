set names utf8mb4;
use volunteer_service;

create table if not exists ai_chat_message (
    id bigint primary key auto_increment,
    session_id varchar(64) not null,
    user_id bigint not null,
    role varchar(20) not null comment 'USER/ASSISTANT',
    content text not null,
    created_at datetime not null default current_timestamp,
    index idx_ai_chat_session(session_id),
    constraint fk_ai_chat_user foreign key(user_id) references user(id) on delete cascade
) comment='AI助手聊天记录预留表';

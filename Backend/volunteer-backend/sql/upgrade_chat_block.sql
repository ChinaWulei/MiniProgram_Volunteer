set names utf8mb4;
use volunteer_service;

create table if not exists chat_block (
    id bigint primary key auto_increment,
    blocker_id bigint not null,
    blocked_user_id bigint not null,
    created_at datetime not null default current_timestamp,
    unique key uk_chat_block_pair(blocker_id, blocked_user_id),
    index idx_chat_block_blocked(blocked_user_id),
    constraint fk_chat_block_blocker foreign key(blocker_id) references user(id) on delete cascade,
    constraint fk_chat_block_blocked foreign key(blocked_user_id) references user(id) on delete cascade
) comment='聊天拉黑关系';

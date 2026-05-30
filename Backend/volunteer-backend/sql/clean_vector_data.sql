create extension if not exists vector;

create table if not exists rule_file_chunk (
    id bigserial primary key,
    rule_file_id bigint not null,
    file_name varchar(255) not null,
    chunk_index int not null,
    content text not null,
    embedding vector(768) not null,
    created_at timestamptz not null default now(),
    unique(rule_file_id, chunk_index)
);

truncate table rule_file_chunk restart identity;


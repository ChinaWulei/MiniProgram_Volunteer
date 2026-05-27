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

create index if not exists idx_rule_file_chunk_file on rule_file_chunk(rule_file_id);
create index if not exists idx_rule_file_chunk_embedding
    on rule_file_chunk using hnsw (embedding vector_cosine_ops);

-- Vector search SQL used by the backend:
-- select rule_file_id,file_name,chunk_index,content,embedding <=> ?::vector as distance
-- from rule_file_chunk
-- order by embedding <=> ?::vector
-- limit ?;

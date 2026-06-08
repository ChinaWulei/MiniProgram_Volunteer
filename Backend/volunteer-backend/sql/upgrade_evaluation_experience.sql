set names utf8mb4;
use volunteer_service;

set @ddl = (
    select if(count(*) = 0,
        'alter table activity_evaluation add column `anonymous` tinyint(1) not null default 0 after content',
        'select 1')
    from information_schema.columns
    where table_schema = database() and table_name = 'activity_evaluation' and column_name = 'anonymous'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
    select if(count(*) = 0,
        'alter table activity_evaluation add column parsed_overall varchar(500) after `anonymous`',
        'select 1')
    from information_schema.columns
    where table_schema = database() and table_name = 'activity_evaluation' and column_name = 'parsed_overall'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
    select if(count(*) = 0,
        'alter table activity_evaluation add column parsed_advantages varchar(500) after parsed_overall',
        'select 1')
    from information_schema.columns
    where table_schema = database() and table_name = 'activity_evaluation' and column_name = 'parsed_advantages'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
    select if(count(*) = 0,
        'alter table activity_evaluation add column parsed_problems varchar(500) after parsed_advantages',
        'select 1')
    from information_schema.columns
    where table_schema = database() and table_name = 'activity_evaluation' and column_name = 'parsed_problems'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
    select if(count(*) = 0,
        'alter table activity_evaluation add column parsed_suggestions varchar(500) after parsed_problems',
        'select 1')
    from information_schema.columns
    where table_schema = database() and table_name = 'activity_evaluation' and column_name = 'parsed_suggestions'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
    select if(count(*) = 0,
        'alter table activity_evaluation add column analysis_status varchar(20) not null default ''PENDING'' after parsed_suggestions',
        'select 1')
    from information_schema.columns
    where table_schema = database() and table_name = 'activity_evaluation' and column_name = 'analysis_status'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = (
    select if(count(*) = 0,
        'alter table activity_evaluation add column analyzed_at datetime after analysis_status',
        'select 1')
    from information_schema.columns
    where table_schema = database() and table_name = 'activity_evaluation' and column_name = 'analyzed_at'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

create table if not exists activity_experience (
    id bigint primary key auto_increment,
    activity_id bigint not null,
    evaluation_id bigint not null,
    activity_category varchar(50) not null,
    experience_type varchar(30) not null comment 'ADVANTAGE/SUGGESTION',
    content varchar(500) not null,
    enabled tinyint(1) not null default 1,
    adopted_by bigint not null,
    adopted_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_experience_source(evaluation_id, experience_type),
    key idx_experience_category_enabled(activity_category, enabled),
    constraint fk_experience_activity foreign key(activity_id) references activity(id) on delete cascade,
    constraint fk_experience_evaluation foreign key(evaluation_id) references activity_evaluation(id) on delete cascade,
    constraint fk_experience_admin foreign key(adopted_by) references user(id)
) comment='活动评价经验库';

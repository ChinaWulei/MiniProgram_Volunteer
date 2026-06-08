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


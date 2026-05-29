set names utf8mb4;
use volunteer_service;

create table if not exists ai_report (
    id bigint primary key auto_increment,
    report_no varchar(80) not null,
    report_type varchar(20) not null,
    user_id bigint not null,
    period_start varchar(20) null,
    period_end varchar(20) null,
    stats_json longtext not null,
    ai_analysis longtext null,
    pdf_url varchar(700) null,
    created_at datetime not null default current_timestamp,
    updated_at datetime not null default current_timestamp on update current_timestamp,
    unique key uk_ai_report_no(report_no),
    index idx_ai_report_user(user_id, report_type, created_at)
);

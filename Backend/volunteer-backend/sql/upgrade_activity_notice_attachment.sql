use volunteer_service;

create table if not exists notification_attachment (
    id bigint primary key auto_increment,
    notification_id bigint not null,
    rule_file_id bigint not null,
    created_at datetime not null default current_timestamp,
    unique key uk_notification_rule_file(notification_id, rule_file_id),
    index idx_notification_attachment(notification_id),
    index idx_notification_attachment_file(rule_file_id)
) comment='活动通知附件关联';

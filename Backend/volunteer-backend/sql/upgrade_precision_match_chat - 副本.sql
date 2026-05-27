set names utf8mb4;
use volunteer_service;

alter table activity
    add column signup_start_time datetime null after end_time,
    add column checkin_start_time datetime null after signup_deadline,
    add column checkin_end_time datetime null after checkin_start_time;

update activity
set checkin_start_time = coalesce(checkin_start_time, start_time),
    checkin_end_time = coalesce(checkin_end_time, end_time);

alter table chat_message
    add column image_url varchar(500) null after activity_id;

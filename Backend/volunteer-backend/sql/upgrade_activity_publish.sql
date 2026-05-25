set names utf8mb4;
use volunteer_service;

alter table activity
    add column cover_image_url varchar(500) null after name,
    add column signup_deadline datetime null after end_time,
    add column published_at datetime null after created_by;

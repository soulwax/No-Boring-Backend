-- Migration V3: Add quests, micro-steps, slay tracking, and R2 media artwork support

alter table boring_thing add column image_url varchar(512);
alter table boring_thing add column quest_steps varchar(1000) not null default '';
alter table boring_thing add column estimated_minutes integer not null default 15;
alter table boring_thing add column slayed_at timestamp(6) with time zone;
alter table boring_thing add column slayed_by varchar(40);

alter table player_progress add column slayed_count integer not null default 0;
alter table player_progress add column slayer_xp integer not null default 0;

create index boring_thing_slayed on boring_thing (slayed_at, created_at desc);

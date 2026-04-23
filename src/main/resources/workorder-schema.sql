create table if not exists wo_user (
  id varchar(36) primary key,
  username varchar(64) not null unique,
  password varchar(128) not null,
  display_name varchar(128) not null,
  avatar_url text,
  role varchar(32) not null,
  created_at varchar(19) not null,
  updated_at varchar(19) not null
);

create table if not exists wo_feedback (
  id varchar(36) primary key,
  code varchar(32) not null unique,
  title varchar(200) not null,
  description text not null,
  status varchar(32) not null,
  owner_username varchar(64) not null,
  account_name varchar(128) not null,
  assignee varchar(128) not null default '',
  images_json text,
  attachments_json text,
  created_at varchar(19) not null,
  updated_at varchar(19) not null
);

create table if not exists wo_feedback_reply (
  id varchar(36) primary key,
  feedback_id varchar(36) not null,
  role varchar(32) not null,
  author varchar(128) not null,
  content text not null,
  created_at varchar(19) not null,
  constraint fk_feedback_reply_feedback foreign key (feedback_id) references wo_feedback(id) on delete cascade
);

create table if not exists wo_work_order (
  id varchar(36) primary key,
  code varchar(32) not null unique,
  title varchar(200) not null,
  description text not null,
  status varchar(32) not null,
  assignee varchar(128) not null,
  created_at varchar(19) not null,
  updated_at varchar(19) not null
);

create table if not exists wo_work_order_flow (
  id varchar(36) primary key,
  work_order_id varchar(36) not null,
  status varchar(32) not null,
  remark varchar(500),
  operator varchar(128) not null,
  created_at varchar(19) not null,
  constraint fk_work_order_flow_work_order foreign key (work_order_id) references wo_work_order(id) on delete cascade
);

create table direct_message (
    id uuid primary key not null,
    sender_id uuid not null,
    recipient_id uuid not null,
    content text not null,
    sent_at timestamptz not null default now(),
    delivered boolean default false
);

create index idx_dm_participants on direct_message (sender_id, recipient_id);
create index idx_dm_sent_at on direct_message (sent_at);

create table message
(
    conversation_id bigserial             not null,
    message_id      text                  not null,
    sender_id       uuid                  not null,
    recipient_id    uuid                  not null,
    content         text                  not null,
    sent_at         timestamptz           not null default now(),
    delivered       boolean               not null default false,
    primary key (conversation_id, message_id)
);

create index idx_dm_participants on message (conversation_id, message_id);
create index idx_dm_sent_at on message (sent_at);

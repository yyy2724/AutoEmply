create table sample_template_sets (
    id uuid primary key,
    name varchar(255) not null,
    template_ids_json text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_sample_template_sets_updated_at
    on sample_template_sets (updated_at desc);

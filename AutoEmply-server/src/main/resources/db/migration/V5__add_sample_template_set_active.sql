alter table sample_template_sets
    add column is_active boolean not null default true;

create index idx_sample_template_sets_active_updated_at
    on sample_template_sets (is_active, updated_at desc);

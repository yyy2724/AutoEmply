alter table prompt_presets
    add column if not exists is_primary boolean not null default false;

alter table sample_template_sets
    add column if not exists is_primary boolean not null default false;

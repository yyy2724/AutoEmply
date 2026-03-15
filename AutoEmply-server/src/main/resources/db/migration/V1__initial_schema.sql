create table prompt_presets (
    id uuid primary key,
    name varchar(255) not null,
    system_prompt text not null,
    user_prompt_template text not null,
    style_rules_json text,
    model varchar(255),
    temperature numeric(38, 2),
    max_tokens integer,
    is_active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table prompt_versions (
    id uuid primary key,
    version integer not null,
    system_prompt text not null,
    user_prompt_template text not null,
    style_rules_json text,
    created_at timestamp with time zone not null,
    preset_id uuid not null,
    constraint fk_prompt_versions_preset
        foreign key (preset_id) references prompt_presets (id) on delete cascade
);

create index idx_prompt_presets_active_updated_at
    on prompt_presets (is_active, updated_at desc);

create index idx_prompt_presets_name
    on prompt_presets (name);

create index idx_prompt_versions_preset_id
    on prompt_versions (preset_id);

create table report_templates (
    id uuid primary key,
    name varchar(255) not null,
    category varchar(255) not null,
    dfm_content text not null,
    pas_content text not null,
    original_form_name varchar(255) not null,
    preview_content_type varchar(255),
    preview_data bytea,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_report_templates_category_name
    on report_templates (category, name);

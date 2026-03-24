alter table prompt_presets
    alter column style_rules_json type jsonb using style_rules_json::jsonb;

alter table prompt_versions
    alter column style_rules_json type jsonb using style_rules_json::jsonb;

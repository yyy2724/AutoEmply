alter table prompt_presets
    alter column style_rules_json type jsonb
    using case
        when style_rules_json is null or btrim(style_rules_json) = '' then null
        else style_rules_json::jsonb
    end;

alter table prompt_versions
    alter column style_rules_json type jsonb
    using case
        when style_rules_json is null or btrim(style_rules_json) = '' then null
        else style_rules_json::jsonb
    end;

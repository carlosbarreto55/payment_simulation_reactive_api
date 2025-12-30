-- Inserir o role USER se não existir
INSERT INTO roles (name, description)
VALUES ('USER', 'Usuário padrão do sistema')
    ON DUPLICATE KEY UPDATE
                         description = VALUES(description),
                         updated_at = CURRENT_TIMESTAMP;
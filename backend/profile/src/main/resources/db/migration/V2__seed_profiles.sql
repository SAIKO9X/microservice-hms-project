-- ============================================================
-- V2__seed_profiles.sql
-- Seed consolidado de perfis (Médicos, Pacientes, Alergias e Avaliações)
-- Referência direta aos IDs do user-service (ms_user_db).
-- ============================================================

-- ============================================================
-- MÉDICOS
-- ============================================================

-- Médico Base (Demo)
INSERT IGNORE INTO tb_doctors (
    user_id, name, date_of_birth, crm_number, specialization, department, phone_number, years_of_experience, qualifications, consultation_fee, biography, profile_picture_url
) VALUES (
    2, 'Doctor Demo', '1982-05-15', 'CRM-SP-123456', 'Clínica Geral', 'Ambulatório', '(11) 91234-5000', 5, 'Residência em Clínica Médica', 150.00, 'Bio: Médico generalista com foco em prevenção e atendimento humanizado.', 'https://randomuser.me/api/portraits/men/10.jpg'
);

-- Demais Médicos
INSERT IGNORE INTO tb_doctors (
    user_id, name, date_of_birth, crm_number, specialization, department,
    phone_number, years_of_experience, qualifications, biography, consultation_fee, profile_picture_url
) VALUES
    (
        4, 'Dr. Carlos Eduardo Ribeiro', '1978-04-22', 'CRM-SP-234567',
        'Cardiologia', 'Cardiologia Intervencionista',
        '(11) 91234-5001', 18,
        'Doutorado em Cardiologia - USP; Membro da Sociedade Brasileira de Cardiologia',
        'Especialista em doenças cardiovasculares com foco em hipertensão, insuficiência cardíaca e arritmias. Mais de 18 anos de experiência clínica.',
        250.00,
        'https://randomuser.me/api/portraits/men/32.jpg'
    ),
    (
        5, 'Dra. Ana Paula Ferreira', '1985-08-10', 'CRM-SP-345678',
        'Pediatria', 'Pediatria Geral',
        '(11) 91234-5002', 11,
        'Residência em Pediatria - UNIFESP; Especialização em Neonatologia',
        'Pediatra dedicada ao cuidado integral de crianças e adolescentes, com experiência em desenvolvimento infantil e doenças respiratórias pediátricas.',
        180.00,
        'https://randomuser.me/api/portraits/women/44.jpg'
    ),
    (
        6, 'Dr. Roberto Nascimento', '1975-11-30', 'CRM-SP-456789',
        'Ortopedia', 'Cirurgia de Joelho e Quadril',
        '(11) 91234-5003', 21,
        'Mestre em Ortopedia - UNICAMP; Fellowship em Cirurgia do Joelho - Hospital das Clínicas',
        'Ortopedista especializado em lesões esportivas, substituição articular e fraturas. Atende atletas amadores e profissionais.',
        200.00,
        'https://randomuser.me/api/portraits/men/57.jpg'
    ),
    (
        7, 'Dra. Mariana Cavalcante', '1989-03-14', 'CRM-SP-567890',
        'Dermatologia', 'Dermatologia Clínica e Cosmética',
        '(11) 91234-5004', 7,
        'Residência em Dermatologia - Hospital A.C. Camargo; Membro da Sociedade Brasileira de Dermatologia',
        'Dermatologista com foco em dermatites, acne, psoríase e procedimentos estéticos minimamente invasivos.',
        170.00,
        'https://randomuser.me/api/portraits/women/23.jpg'
    ),
    (
        8, 'Dr. Paulo Henrique Almeida', '1972-07-25', 'CRM-SP-678901',
        'Neurologia', 'Neurologia Clínica',
        '(11) 91234-5005', 24,
        'Doutorado em Neurociências - USP; Especialização em Cefaleia e Epilepsia',
        'Neurologista com vasta experiência em enxaquecas, epilepsia, AVC e doenças neurodegenerativas. Referência regional em cefaleia crônica.',
        220.00,
        'https://randomuser.me/api/portraits/men/71.jpg'
    );

-- ============================================================
-- PACIENTES
-- ============================================================

-- Paciente Base (Demo)
INSERT IGNORE INTO tb_patients (
    user_id, name, cpf, date_of_birth, phone_number, blood_group, gender, address, emergency_contact_name, emergency_contact_phone, profile_picture_url
) VALUES (
    3, 'Patient Demo', '000.000.000-00', '1990-01-01', '(11) 99999-9999', 'O_POSITIVE', 'MALE', 'Rua Exemplo Demo, 123, Centro, São Paulo/SP', 'Contato Demo', '(11) 98888-8888', 'https://randomuser.me/api/portraits/men/15.jpg'
);

-- Demais Pacientes
INSERT IGNORE INTO tb_patients (
    user_id, name, cpf, date_of_birth, phone_number,
    blood_group, gender, address,
    emergency_contact_name, emergency_contact_phone,
    family_history, chronic_conditions, profile_picture_url
) VALUES
    (
        9,  'João Victor Silva',       '123.456.789-01', '1988-06-15', '(11) 98765-4001',
        'A_POSITIVE', 'MALE', 'Rua das Flores, 123 - Mooca, São Paulo/SP',
        'Carla Silva', '(11) 98765-4100',
        'Hipertensão paterna; Diabetes tipo 2 materno',
        'Hipertensão arterial em tratamento',
        'https://randomuser.me/api/portraits/men/22.jpg'
    ),
    (
        10, 'Maria Fernanda Santos',   '234.567.890-12', '1995-02-28', '(11) 98765-4002',
        'O_NEGATIVE', 'FEMALE', 'Av. Paulista, 456 - Bela Vista, São Paulo/SP',
        'José Santos', '(11) 98765-4200',
        'Nenhum histórico relevante',
        NULL,
        'https://randomuser.me/api/portraits/women/31.jpg'
    ),
    (
        11, 'Pedro Henrique Oliveira', '345.678.901-23', '1980-09-03', '(11) 98765-4003',
        'B_POSITIVE', 'MALE', 'Rua Vergueiro, 789 - Vila Mariana, São Paulo/SP',
        'Sandra Oliveira', '(11) 98765-4300',
        'Artrose no avô paterno',
        'Gonartrose joelho direito (diagnosticada 2025)',
        'https://randomuser.me/api/portraits/men/43.jpg'
    ),
    (
        12, 'Ana Beatriz Costa',       '456.789.012-34', '1993-12-20', '(11) 98765-4004',
        'AB_POSITIVE', 'FEMALE', 'Rua Augusta, 321 - Consolação, São Paulo/SP',
        'Ricardo Costa', '(11) 98765-4400',
        'Alergias cutâneas maternas',
        'Dermatite atópica leve',
        'https://randomuser.me/api/portraits/women/52.jpg'
    ),
    (
        13, 'Lucas Gabriel Ferreira',  '567.890.123-45', '1990-04-18', '(11) 98765-4005',
        'O_POSITIVE', 'MALE', 'Rua da Consolação, 654 - Higienópolis, São Paulo/SP',
        'Paula Ferreira', '(11) 98765-4500',
        'Enxaqueca materna; Hipertensão paterna',
        'Enxaqueca crônica',
        'https://randomuser.me/api/portraits/men/64.jpg'
    ),
    (
        14, 'Júlia Rodrigues Alves',   '678.901.234-56', '1998-07-07', '(11) 98765-4006',
        'A_NEGATIVE', 'FEMALE', 'Rua Haddock Lobo, 987 - Cerqueira César, São Paulo/SP',
        'Eduardo Alves', '(11) 98765-4600',
        'Nenhum histórico relevante',
        NULL,
        'https://randomuser.me/api/portraits/women/17.jpg'
    ),
    (
        15, 'Marcos Vinícius Souza',   '789.012.345-67', '1975-10-10', '(11) 98765-4007',
        'B_NEGATIVE', 'MALE', 'Av. Brigadeiro Faria Lima, 111 - Itaim Bibi, São Paulo/SP',
        'Cláudia Souza', '(11) 98765-4700',
        'Diabetes tipo 2 paterno',
        NULL,
        'https://randomuser.me/api/portraits/men/78.jpg'
    ),
    (
        16, 'Fernanda Cristina Lima',  '890.123.456-78', '1987-01-25', '(11) 98765-4008',
        'O_POSITIVE', 'FEMALE', 'Rua Oscar Freire, 222 - Jardins, São Paulo/SP',
        'Roberto Lima', '(11) 98765-4800',
        'Tireoide materna',
        'Hipotireoidismo em tratamento',
        'https://randomuser.me/api/portraits/women/63.jpg'
    ),
    (
        17, 'Gabriel Augusto Rocha',   '901.234.567-89', '2001-03-30', '(11) 98765-4009',
        'A_POSITIVE', 'MALE', 'Rua dos Três Irmãos, 333 - Saúde, São Paulo/SP',
        'Patrícia Rocha', '(11) 98765-4900',
        'Nenhum histórico relevante',
        NULL,
        'https://randomuser.me/api/portraits/men/5.jpg'
    ),
    (
        18, 'Beatriz Caroline Mendes', '012.345.678-90', '2003-11-05', '(11) 98765-4010',
        'AB_NEGATIVE', 'FEMALE', 'Rua Maestro Cardim, 444 - Paraíso, São Paulo/SP',
        'Fernando Mendes', '(11) 98765-5000',
        'Nenhum histórico relevante',
        NULL,
        'https://randomuser.me/api/portraits/women/8.jpg'
    );

-- ============================================================
-- ALERGIAS DE PACIENTES
-- ============================================================

-- João Silva (patient_id=2)
INSERT IGNORE INTO patient_allergies (patient_id, allergy) SELECT id, 'Dipirona' FROM tb_patients WHERE cpf = '123.456.789-01';
INSERT IGNORE INTO patient_allergies (patient_id, allergy) SELECT id, 'Penicilina' FROM tb_patients WHERE cpf = '123.456.789-01';

-- Ana Costa (patient_id=5)
INSERT IGNORE INTO patient_allergies (patient_id, allergy) SELECT id, 'Látex' FROM tb_patients WHERE cpf = '456.789.012-34';
INSERT IGNORE INTO patient_allergies (patient_id, allergy) SELECT id, 'Níquel' FROM tb_patients WHERE cpf = '456.789.012-34';

-- Lucas Ferreira (patient_id=6)
INSERT IGNORE INTO patient_allergies (patient_id, allergy) SELECT id, 'Aspirina' FROM tb_patients WHERE cpf = '567.890.123-45';

-- Fernanda Lima (patient_id=9)
INSERT IGNORE INTO patient_allergies (patient_id, allergy) SELECT id, 'Sulfa' FROM tb_patients WHERE cpf = '890.123.456-78';

-- ============================================================
-- AVALIAÇÕES DE CONSULTAS CONCLUÍDAS
-- ============================================================
INSERT IGNORE INTO reviews (
    doctor_id, patient_id, appointment_id, rating, comment, created_at
) VALUES
    -- Doctor Demo (id=1)
    (1, 1, 1, 5, 'Médico excelente! Muito atencioso e didático ao explicar o diagnóstico. Consulta de rotina muito bem conduzida.', '2026-02-12 11:30:00'),
    (1, 7, 6, 4, 'Ótimo atendimento do doutor, pediu todos os exames preventivos básicos. A recepção estava um pouco cheia no dia, mas ocorreu tudo bem.', '2026-02-28 14:15:00'),
    (1, 10, 7, 5, 'Fui muito bem atendido. O médico me passou muita confiança e tirou todas as minhas dúvidas com paciência.', '2026-03-10 09:45:00'),

    -- Dr. Carlos (id=2) - Cardiologia
    (2, 2, 2, 5, 'Dr. Carlos foi preciso no diagnóstico e bastante tranquilizador. Explicou cada etapa do tratamento para hipertensão com muita clareza.', '2026-02-15 15:45:00'),
    (2, 8, 8, 5, 'Acompanhamento cardiológico de primeira. Ele analisa os exames com muita atenção e ajustou minha medicação perfeitamente.', '2026-03-05 10:20:00'),
    (2, 11, 9, 4, 'A consulta atrasou uns 15 minutos, mas valeu a pena a espera. O médico é excelente e passa muita segurança.', '2026-03-18 16:30:00'),

    -- Dra. Ana Paula (id=3) - Pediatria
    (3, 3, 3, 4, 'Dra. Ana Paula foi muito atenciosa com meu filho. O tratamento para a febre foi eficaz e ela explicou muito bem os cuidados em casa.', '2026-02-21 09:00:00'),
    (3, 4, 10, 5, 'Trouxe minha filha recém-nascida e a doutora foi super carinhosa e paciente com nós, pais de primeira viagem. Recomendo muito!', '2026-03-01 13:00:00'),
    (3, 9, 11, 5, 'A melhor pediatra que já fomos. O ambiente é acolhedor e o tratamento é extremamente humanizado.', '2026-03-20 15:10:00'),

    -- Dr. Roberto (id=4) - Ortopedia
    (4, 4, 12, 5, 'Excelente profissional! Me explicou perfeitamente sobre a situação da artrose no meu joelho e as opções de tratamento.', '2026-02-10 11:00:00'),
    (4, 6, 13, 4, 'Médico muito direto e objetivo. O tratamento que ele receitou para a minha dor lombar funcionou muito rápido.', '2026-03-08 14:40:00'),
    (4, 1, 14, 5, 'Fiz o acompanhamento pós-operatório com ele. A recuperação foi ótima, sempre muito claro nas orientações fisioterápicas.', '2026-03-25 10:15:00'),

    -- Dra. Mariana (id=5) - Dermatologia
    (5, 5, 5, 5, 'Incrível! A Dra. Mariana identificou rapidamente o problema de pele e indicou o tratamento certo. Já estou muito melhor.', '2026-03-02 16:00:00'),
    (5, 7, 15, 5, 'Os manipulados que ela receitou para o controle da acne fizeram efeito na primeira semana! Profissional muito atualizada.', '2026-03-12 08:30:00'),
    (5, 2, 16, 4, 'A consulta médica foi muito boa, ela analisou bem meu caso de dermatite. Tive um pequeno problema com o sistema de agendamento, mas a doutora compensou.', '2026-03-22 17:00:00'),

    -- Dr. Paulo (id=6) - Neurologia
    (6, 6, 17, 5, 'Sofro de enxaqueca crônica há anos e, depois de passar por vários médicos, finalmente o Dr. Paulo acertou o meu tratamento profilático.', '2026-02-18 09:20:00'),
    (6, 10, 18, 5, 'Profissional extremamente competente e detalhista. Fez vários testes clínicos no consultório antes de pedir os exames de imagem.', '2026-03-15 11:50:00'),
    (6, 8, 19, 5, 'Avaliação neurológica muito completa. Fiquei muito satisfeito com a clareza das explicações sobre os próximos passos.', '2026-03-28 15:30:00');
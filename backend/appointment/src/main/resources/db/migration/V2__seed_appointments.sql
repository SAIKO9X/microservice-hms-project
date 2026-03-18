-- ============================================================
-- V2__seed_appointments.sql
-- Seed completo do appointment-service (Datas Dinâmicas)
--
-- NOTAS DE MAPEAMENTO:
-- * doctor_id e patient_id referenciam o profile_id (profile-service).
-- * Consultas 1 a 6: Concluídas (possuem prontuário, prescrição e exames) - PASSADO.
-- * Consultas 7 a 9: Canceladas ou No-Show - PASSADO.
-- * Consultas 10 a 18: Agendadas - FUTURO.
-- ============================================================

-- ============================================================
-- 1. READ MODEL DE MÉDICOS E PACIENTES (Projeções)
-- ============================================================
INSERT IGNORE INTO doctor_read_model (doctor_id, user_id, full_name, specialization, profile_picture, biography)
VALUES
    (1, 2, 'Doctor Demo',               'Clínica Geral',   NULL, 'Bio: Médico generalista com foco em prevenção.'),
    (2, 4, 'Dr. Carlos Eduardo Ribeiro','Cardiologia',      NULL, 'Bio: Especialista em saúde cardiovascular e prevenção de riscos.'),
    (3, 5, 'Dra. Ana Paula Ferreira',   'Pediatria',        NULL, 'Bio: Dedicada ao cuidado integral da criança e adolescente.'),
    (4, 6, 'Dr. Roberto Nascimento',    'Ortopedia',        NULL, 'Bio: Especialista em traumatologia e medicina esportiva.'),
    (5, 7, 'Dra. Mariana Costa',        'Dermatologia',     NULL, 'Bio: Foco em tratamentos clínicos e estéticos da pele.'),
    (6, 8, 'Dr. Paulo Mendes',          'Neurologia',       NULL, 'Bio: Tratamento de distúrbios neurológicos e dores crônicas.');

INSERT IGNORE INTO patient_read_model (patient_id, user_id, full_name, phone_number, email, profile_picture)
VALUES
    (1,  3,  'Patient Demo',               '(11) 99999-0001', 'patient@hms.com',           NULL),
    (2,  9,  'João Victor Silva',           '(11) 98765-4001', 'joao.silva@email.com',      NULL),
    (3,  10, 'Maria Fernanda Santos',       '(11) 98765-4002', 'maria.santos@email.com',    NULL),
    (4,  11, 'Pedro Henrique Oliveira',     '(11) 98765-4003', 'pedro.oliveira@email.com',  NULL),
    (5,  12, 'Ana Beatriz Costa',           '(11) 98765-4004', 'ana.costa@email.com',       NULL),
    (6,  13, 'Lucas Gabriel Ferreira',      '(11) 98765-4005', 'lucas.ferreira@email.com',  NULL),
    (7,  14, 'Júlia Rodrigues Alves',       '(11) 98765-4006', 'julia.alves@email.com',     NULL),
    (8,  15, 'Marcos Vinícius Souza',       '(11) 98765-4007', 'marcos.souza@email.com',    NULL),
    (9,  16, 'Fernanda Cristina Lima',      '(11) 98765-4008', 'fernanda.lima@email.com',   NULL),
    (10, 17, 'Gabriel Augusto Rocha',       '(11) 98765-4009', 'gabriel.rocha@email.com',   NULL),
    (11, 18, 'Beatriz Caroline Mendes',     '(11) 98765-4010', 'beatriz.mendes@email.com',  NULL);

-- ============================================================
-- 2. DISPONIBILIDADE DOS MÉDICOS (Agenda Semanal)
-- ============================================================
INSERT IGNORE INTO tb_doctor_availability (doctor_id, day_of_week, start_time, end_time)
VALUES
    -- Doctor Demo (Seg a Sex, 08h-17h)
    (1, 'MONDAY', '08:00:00', '17:00:00'), (1, 'TUESDAY', '08:00:00', '17:00:00'),
    (1, 'WEDNESDAY', '08:00:00', '17:00:00'), (1, 'THURSDAY', '08:00:00', '17:00:00'),
    (1, 'FRIDAY', '08:00:00', '17:00:00'),
    -- Dr. Carlos (Seg a Sex, 09h-18h)
    (2, 'MONDAY', '09:00:00', '18:00:00'), (2, 'TUESDAY', '09:00:00', '18:00:00'),
    (2, 'WEDNESDAY', '09:00:00', '18:00:00'), (2, 'THURSDAY', '09:00:00', '18:00:00'),
    (2, 'FRIDAY', '09:00:00', '18:00:00'),
    -- Dra. Ana Paula (Seg, Qua, Sex, 08h-16h)
    (3, 'MONDAY', '08:00:00', '16:00:00'), (3, 'WEDNESDAY', '08:00:00', '16:00:00'),
    (3, 'FRIDAY', '08:00:00', '16:00:00'),
    -- Dr. Roberto (Ter e Qui, 08h-18h)
    (4, 'TUESDAY', '08:00:00', '18:00:00'), (4, 'THURSDAY', '08:00:00', '18:00:00'),
    -- Dra. Mariana (Seg a Sex, 14h-19h)
    (5, 'MONDAY', '14:00:00', '19:00:00'), (5, 'TUESDAY', '14:00:00', '19:00:00'),
    (5, 'WEDNESDAY', '14:00:00', '19:00:00'), (5, 'THURSDAY', '14:00:00', '19:00:00'),
    (5, 'FRIDAY', '14:00:00', '19:00:00'),
    -- Dr. Paulo (Seg a Qui, 09h-17h)
    (6, 'MONDAY', '09:00:00', '17:00:00'), (6, 'TUESDAY', '09:00:00', '17:00:00'),
    (6, 'WEDNESDAY', '09:00:00', '17:00:00'), (6, 'THURSDAY', '09:00:00', '17:00:00');

-- ============================================================
-- 3. CONSULTAS (Histórico e Futuras)
-- ============================================================
INSERT IGNORE INTO tb_appointments
    (id, patient_id, doctor_id, appointment_date_time, duration, appointment_end_time, reason, status, notes, type, reminder_24h_sent, reminder_1h_sent, billing_processed, pharmacy_processed)
VALUES
    -- --- CONCLUÍDAS (PASSADO) ---
    (1, 1, 1, TIMESTAMP(CURDATE() - INTERVAL 30 DAY, '10:00:00'), 30, TIMESTAMP(CURDATE() - INTERVAL 30 DAY, '10:30:00'), 'Consulta de rotina anual — check-up geral', 'COMPLETED', 'Paciente sem queixas agudas. Exames solicitados.', 'IN_PERSON', TRUE, TRUE, TRUE, TRUE),
    (2, 2, 2, TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '14:00:00'), 30, TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '14:30:00'), 'Dor no peito e cansaço aos esforços', 'COMPLETED', 'Diagnóstico de hipertensão arterial. Iniciado tratamento medicamentoso.', 'IN_PERSON', TRUE, TRUE, TRUE, TRUE),
    (3, 3, 3, TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '09:00:00'), 30, TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '09:30:00'), 'Criança com febre há 2 dias e tosse', 'COMPLETED', 'Infecção viral de vias aéreas superiores. Prescrito antitérmico e antibiótico.', 'IN_PERSON', TRUE, TRUE, TRUE, TRUE),
    (4, 4, 4, TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '11:00:00'), 45, TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '11:45:00'), 'Dor persistente no joelho direito há 3 semanas', 'COMPLETED', 'Gonartrose grau II confirmada por imagem. Anti-inflamatório prescrito. Fisioterapia recomendada.', 'IN_PERSON', TRUE, TRUE, TRUE, TRUE),
    (5, 5, 5, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '15:00:00'), 30, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '15:30:00'), 'Lesão avermelhada e pruriginosa no antebraço esquerdo', 'COMPLETED', 'Dermatite de contato confirmada. Afastado uso de acessórios de níquel.', 'IN_PERSON', TRUE, TRUE, TRUE, TRUE),
    (6, 6, 6, TIMESTAMP(CURDATE() - INTERVAL 5 DAY,  '10:00:00'), 30, TIMESTAMP(CURDATE() - INTERVAL 5 DAY,  '10:30:00'), 'Cefaleia intensa recorrente há 6 meses, com náuseas e fotofobia', 'COMPLETED', 'Diagnóstico de enxaqueca crônica. Iniciada profilaxia com beta-bloqueador.', 'IN_PERSON', TRUE, TRUE, TRUE, TRUE),

    -- --- CANCELADAS / NÃO COMPARECEU (PASSADO) ---
    (7, 8, 1, TIMESTAMP(CURDATE() - INTERVAL 18 DAY, '09:00:00'), 30, TIMESTAMP(CURDATE() - INTERVAL 18 DAY, '09:30:00'), 'Check-up geral', 'CANCELED', 'Paciente cancelou por motivo pessoal.', 'IN_PERSON', TRUE, FALSE, FALSE, FALSE),
    (8, 9, 2, TIMESTAMP(CURDATE() - INTERVAL 8 DAY,  '14:00:00'), 30, TIMESTAMP(CURDATE() - INTERVAL 8 DAY,  '14:30:00'), 'Avaliação cardiológica preventiva', 'CANCELED', 'Cancelada com 1 dia de antecedência.', 'IN_PERSON', TRUE, FALSE, FALSE, FALSE),
    (9, 10, 3, TIMESTAMP(CURDATE() - INTERVAL 2 DAY,  '11:00:00'), 30, TIMESTAMP(CURDATE() - INTERVAL 2 DAY,  '11:30:00'), 'Consulta pediátrica de rotina', 'NO_SHOW', 'Paciente não compareceu e não avisou previamente.', 'IN_PERSON', TRUE, TRUE, FALSE, FALSE),

    -- --- AGENDADAS (FUTURO) ---
    (10, 1,  1, TIMESTAMP(CURDATE() + INTERVAL 2 DAY,  '10:00:00'), 30, TIMESTAMP(CURDATE() + INTERVAL 2 DAY,  '10:30:00'), 'Retorno — resultado dos exames de check-up', 'SCHEDULED', NULL, 'IN_PERSON', FALSE, FALSE, FALSE, FALSE),
    (11, 11, 2, TIMESTAMP(CURDATE() + INTERVAL 2 DAY,  '14:00:00'), 30, TIMESTAMP(CURDATE() + INTERVAL 2 DAY,  '14:30:00'), 'Primeira consulta cardiológica', 'SCHEDULED', NULL, 'IN_PERSON', FALSE, FALSE, FALSE, FALSE),
    (12, 2,  2, TIMESTAMP(CURDATE() + INTERVAL 5 DAY,  '09:00:00'), 30, TIMESTAMP(CURDATE() + INTERVAL 5 DAY,  '09:30:00'), 'Retorno — controle de hipertensão arterial', 'SCHEDULED', NULL, 'IN_PERSON', FALSE, FALSE, FALSE, FALSE),
    (13, 3,  3, TIMESTAMP(CURDATE() + INTERVAL 7 DAY,  '10:00:00'), 30, TIMESTAMP(CURDATE() + INTERVAL 7 DAY,  '10:30:00'), 'Retorno pediátrico — avaliação pós-infecção', 'SCHEDULED', NULL, 'IN_PERSON', FALSE, FALSE, FALSE, FALSE),
    (14, 4,  4, TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '11:00:00'), 45, TIMESTAMP(CURDATE() + INTERVAL 10 DAY, '11:45:00'), 'Retorno ortopédico', 'SCHEDULED', NULL, 'IN_PERSON', FALSE, FALSE, FALSE, FALSE),
    (15, 7,  1, TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '14:00:00'), 30, TIMESTAMP(CURDATE() + INTERVAL 12 DAY, '14:30:00'), 'Consulta geral — cansaço frequente', 'SCHEDULED', NULL, 'IN_PERSON', FALSE, FALSE, FALSE, FALSE),
    (16, 5,  5, TIMESTAMP(CURDATE() + INTERVAL 15 DAY, '15:00:00'), 30, TIMESTAMP(CURDATE() + INTERVAL 15 DAY, '15:30:00'), 'Retorno dermatológico', 'SCHEDULED', NULL, 'ONLINE', FALSE, FALSE, FALSE, FALSE),
    (17, 6,  6, TIMESTAMP(CURDATE() + INTERVAL 20 DAY, '09:00:00'), 30, TIMESTAMP(CURDATE() + INTERVAL 20 DAY, '09:30:00'), 'Retorno neurológico — controle da enxaqueca', 'SCHEDULED', NULL, 'IN_PERSON', FALSE, FALSE, FALSE, FALSE),
    (18, 8,  4, TIMESTAMP(CURDATE() + INTERVAL 25 DAY, '10:00:00'), 30, TIMESTAMP(CURDATE() + INTERVAL 25 DAY, '10:30:00'), 'Avaliação ortopédica inicial', 'SCHEDULED', NULL, 'IN_PERSON', FALSE, FALSE, FALSE, FALSE);

UPDATE tb_appointments SET meeting_url = 'https://meet.jit.si/hms-april-consultation-5-5' WHERE id = 16;

-- ============================================================
-- 4. PRESCRIÇÕES E MEDICAMENTOS (Para consultas concluídas)
-- ============================================================
INSERT IGNORE INTO tb_prescriptions (id, appointment_id, notes, created_at, status)
VALUES
    (1, 1, 'Prescrição de rotina. Retorno em 30 dias com resultados dos exames.', TIMESTAMP(CURDATE() - INTERVAL 30 DAY, '10:28:00'), 'DISPENSED'),
    (2, 2, 'Tratamento para hipertensão arterial estágio 1. Monitorar PA semanalmente.', TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '14:28:00'), 'DISPENSED'),
    (3, 3, 'Tratamento de infecção viral com antibioticoterapia empírica e antitérmico.', TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '09:28:00'), 'DISPENSED'),
    (4, 4, 'Anti-inflamatório para gonartrose. Evitar impacto. Iniciar fisioterapia.', TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '11:43:00'), 'ISSUED'),
    (5, 5, 'Antifúngico para dermatite de contato por sensibilização ao níquel. Evitar bijuteria.', TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '15:28:00'), 'ISSUED'),
    (6, 6, 'Profilaxia de enxaqueca com beta-bloqueador + analgésico para crises agudas.', TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '10:28:00'), 'ISSUED');

INSERT IGNORE INTO tb_medicines (name, dosage, frequency, duration, prescription_id)
VALUES
    -- Receita 1: Rotina
    ('Paracetamol', '500mg', '1 comprimido a cada 8 horas se dor ou febre', 7, 1),
    ('Omeprazol', '20mg', '1 cápsula em jejum pela manhã', 30, 1),
    -- Receita 2: Hipertensão
    ('Losartana Potássica', '50mg', '1 comprimido ao dia, preferencialmente pela manhã', 30, 2),
    ('Atorvastatina', '20mg', '1 comprimido à noite', 30, 2),
    -- Receita 3: Pediatria
    ('Amoxicilina', '500mg', '1 cápsula a cada 8 horas', 7, 3),
    ('Paracetamol Pediátrico', '200mg/mL (solução)', 'Dose: 0,4 mL/kg a cada 6 horas se febre acima de 37,8°C', 5, 3),
    -- Receita 4: Ortopedia
    ('Ibuprofeno', '600mg', '1 comprimido a cada 8 horas junto às refeições', 10, 4),
    -- Receita 5: Dermatologia
    ('Fluconazol', '150mg', 'Dose única via oral', 1, 5),
    ('Desonida Creme', '0,05%', 'Aplicar fina camada na área afetada 2x ao dia', 14, 5),
    -- Receita 6: Neurologia
    ('Metoprolol Succinato', '50mg', '1 comprimido ao dia (profilaxia da enxaqueca)', 30, 6),
    ('Naratriptana', '2,5mg', '1 comprimido ao início da crise; repetir após 4h se necessário', 30, 6);

-- ============================================================
-- 5. PRONTUÁRIOS (Registros clínicos)
-- ============================================================
INSERT IGNORE INTO tb_appointment_records
    (appointment_id, chief_complaint, history_of_present_illness, physical_exam_notes, symptoms, diagnosis_cid10, diagnosis_description, treatment_plan, requested_tests, notes, created_at, updated_at)
VALUES
    (1, 'Check-up anual de rotina', 'Paciente de 34 anos, sexo masculino, sem queixas agudas.', 'PA: 120/80 mmHg. FC: 72 bpm. Exame físico geral sem alterações.', 'Assintomático', 'Z00.0', 'Exame médico geral', 'Solicitação de exames de rotina.', 'Hemograma completo, Glicemia em jejum, TSH', 'Orientado sobre alimentação saudável.', TIMESTAMP(CURDATE() - INTERVAL 30 DAY, '10:28:00'), TIMESTAMP(CURDATE() - INTERVAL 30 DAY, '10:28:00')),
    (2, 'Dor precordial e dispneia aos esforços', 'Paciente de 37 anos, queixa de dor no peito e cansaço.', 'PA: 148/95 mmHg. Ritmo cardíaco regular.', 'Precordialgia, dispneia', 'I10', 'Hipertensão arterial primária', 'Iniciado Losartana + Atorvastatina. Orientado dieta.', 'Ecocardiograma transtorácico, Perfil lipídico, ECG', 'Monitorar PA em casa.', TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '14:28:00'), TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '14:28:00')),
    (3, 'Febre e tosse em criança (7 anos)', 'Febre (38,5°C) há 2 dias, tosse seca e inapetência.', 'Temperatura: 38,2°C. Orofaringe hiperemiada.', 'Febre, tosse seca, inapetência', 'J06.9', 'Infecção aguda de vias aéreas', 'Amoxicilina + Paracetamol.', NULL, 'Sinais de alarme explicados à mãe.', TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '09:28:00'), TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '09:28:00')),
    (4, 'Dor e limitação funcional no joelho direito', 'Dor progressiva no joelho direito há 3 semanas.', 'Marcha antálgica. Joelho com crepitação.', 'Dor articular, rigidez matinal', 'M17.1', 'Gonartrose primária', 'Ibuprofeno. Fisioterapia.', NULL, 'Discutida possibilidade de artroplastia futura.', TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '11:43:00'), TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '11:43:00')),
    (5, 'Lesão eritematosa e pruriginosa no antebraço', 'Erupção avermelhada após uso de pulseira.', 'Placas eritematosas com vesiculação leve.', 'Prurido, eritema', 'L23.0', 'Dermatite de contato', 'Fluconazol + Desonida creme.', NULL, 'Retorno em 30 dias.', TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '15:28:00'), TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '15:28:00')),
    (6, 'Cefaleia intensa recorrente há 6 meses', 'Cefaleia pulsátil unilateral, com náuseas e fotofobia.', 'Exame neurológico normal.', 'Cefaleia pulsátil, náusea', 'G43.1', 'Enxaqueca com aura', 'Metoprolol Succinato + Naratriptana.', 'Ressonância Magnética do Encéfalo', 'Manter diário de cefaleias.', TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '10:28:00'), TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '10:28:00'));

-- ============================================================
-- 6. EXAMES LABORATORIAIS
-- ============================================================
INSERT IGNORE INTO tb_lab_orders (id, order_number, appointment_id, patient_id, order_date, notes, status)
VALUES
    (1, 'LAB-2026-001', 1, 1, TIMESTAMP(CURDATE() - INTERVAL 30 DAY, '10:25:00'), 'Exames de rotina.', 'COMPLETED'),
    (2, 'LAB-2026-002', 2, 2, TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '14:25:00'), 'Avaliação cardiovascular.', 'COMPLETED'),
    (3, 'LAB-2026-003', 6, 6, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '10:25:00'), 'RNM para exclusão de causas secundárias.', 'PENDING');

INSERT IGNORE INTO lab_test_items (test_name, category, clinical_indication, instructions, result_notes, status, lab_order_id)
VALUES
    ('Hemograma Completo', 'Hematologia', 'Rotina', 'Coleta em tubo EDTA.', 'Dentro dos parâmetros normais.', 'COMPLETED', 1),
    ('Glicemia em Jejum', 'Bioquímica', 'Rastreio DM', 'Jejum de 8h.', 'Glicemia: 95 mg/dL (normal).', 'COMPLETED', 1),
    ('Ecocardiograma Transtorácico', 'Cardiologia', 'Avaliação estrutural', 'Sem preparo.', 'Leve hipertrofia ventricular esquerda.', 'COMPLETED', 2),
    ('Perfil Lipídico Completo', 'Bioquímica', 'Risco CV', 'Jejum de 12h.', 'Dislipidemia leve.', 'COMPLETED', 2),
    ('Ressonância Magnética com Contraste', 'Neurologia', 'Exclusão de tumores', 'Jejum. Sem metais.', NULL, 'PENDING', 3);

-- ============================================================
-- 7. MÉTRICAS DE SAÚDE (Sinais Vitais)
-- ============================================================
INSERT IGNORE INTO tb_health_metrics (patient_id, blood_pressure, glucose_level, weight, height, bmi, heart_rate, recorded_at)
VALUES
    (1, '120/80', 95.0, 70.0, 1.72, 23.7, 72, TIMESTAMP(CURDATE() - INTERVAL 30 DAY, '10:05:00')), -- Patient Demo
    (2, '148/95', 110.0, 85.0, 1.75, 27.8, 82, TIMESTAMP(CURDATE() - INTERVAL 25 DAY, '14:05:00')), -- João (Consulta)
    (3, '100/65', 88.0, 25.0, 1.22, 16.8, 98, TIMESTAMP(CURDATE() - INTERVAL 20 DAY, '09:05:00')), -- Maria
    (4, '128/84', 102.0, 94.0, 1.80, 29.0, 76, TIMESTAMP(CURDATE() - INTERVAL 15 DAY, '11:05:00')), -- Pedro
    (5, '115/74', 90.0, 57.0, 1.63, 21.4, 68, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '15:05:00')), -- Ana
    (6, '132/86', 104.0, 79.0, 1.78, 24.9, 80, TIMESTAMP(CURDATE() - INTERVAL 5 DAY, '10:05:00')), -- Lucas
    (2, '135/88', 108.0, 85.0, 1.75, 27.8, 78, TIMESTAMP(CURDATE() - INTERVAL 10 DAY, '09:00:00')); -- João (Retorno PA)
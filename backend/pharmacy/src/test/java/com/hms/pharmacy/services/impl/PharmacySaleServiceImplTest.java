package com.hms.pharmacy.services.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hms.common.dto.event.EventEnvelope;
import com.hms.common.exceptions.InvalidOperationException;
import com.hms.pharmacy.dto.request.PharmacySaleRequest;
import com.hms.pharmacy.dto.request.SaleItemRequest;
import com.hms.pharmacy.dto.response.PharmacySaleResponse;
import com.hms.pharmacy.entities.Medicine;
import com.hms.pharmacy.entities.PatientReadModel;
import com.hms.pharmacy.entities.PharmacySale;
import com.hms.pharmacy.entities.PrescriptionCopy;
import com.hms.pharmacy.repositories.MedicineRepository;
import com.hms.pharmacy.repositories.PatientReadModelRepository;
import com.hms.pharmacy.repositories.PharmacySaleRepository;
import com.hms.pharmacy.repositories.PrescriptionCopyRepository;
import com.hms.pharmacy.services.MedicineInventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PharmacySaleServiceImplTest {

  @InjectMocks
  private PharmacySaleServiceImpl pharmacySaleService;

  @Mock
  private PharmacySaleRepository saleRepository;
  @Mock
  private MedicineRepository medicineRepository;
  @Mock
  private MedicineInventoryService inventoryService;
  @Mock
  private RabbitTemplate rabbitTemplate;
  @Mock
  private PrescriptionCopyRepository prescriptionCopyRepository;
  @Mock
  private PatientReadModelRepository patientReadModelRepository;


  @Spy // para utilizar a conversão real de JSON e evitar mocks complexos de TypeReference
  private ObjectMapper objectMapper = new ObjectMapper();

  private PatientReadModel mockPatient;
  private PrescriptionCopy mockPrescription;
  private Medicine mockMedicine;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(pharmacySaleService, "self", pharmacySaleService);

    mockPatient = new PatientReadModel();
    mockPatient.setUserId(100L);
    mockPatient.setName("João Comprador");
    mockPatient.setPhoneNumber("11999999999");
    mockPatient.setEmail("joao@email.com");

    mockMedicine = new Medicine();
    mockMedicine.setId(10L);
    mockMedicine.setName("Amoxicilina");
    mockMedicine.setDosage("500mg");
    mockMedicine.setUnitPrice(new BigDecimal("15.50"));

    mockPrescription = new PrescriptionCopy();
    mockPrescription.setPrescriptionId(1L);
    mockPrescription.setPatientId(100L);
    mockPrescription.setValidUntil(LocalDate.now().plusDays(10));
    mockPrescription.setProcessed(false);
    // simulando a string JSON que vem do banco de dados
    mockPrescription.setItemsJson("[{\"medicineName\":\"Amoxicilina\",\"dosage\":\"500mg\",\"durationDays\":7}]");
  }

  @Test
  @DisplayName("Deve rejeitar a criação da venda se a receita já possuir uma venda registrada (Idempotência)")
  void createSale_Fails_DuplicatePrescription() {
    PharmacySaleRequest request = new PharmacySaleRequest(1L, 100L, List.of());

    // bloqueio de dupla dispensação para a mesma receita
    when(saleRepository.existsByOriginalPrescriptionId(1L)).thenReturn(true);

    InvalidOperationException exception = assertThrows(InvalidOperationException.class,
      () -> pharmacySaleService.createSale(request));

    assertTrue(exception.getMessage().contains("Venda já registrada para esta prescrição"));
    verify(saleRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve rejeitar o processamento se a receita estiver vencida")
  void processPrescription_Fails_Expired() {
    // receita venceu ontem
    mockPrescription.setValidUntil(LocalDate.now().minusDays(1));

    when(prescriptionCopyRepository.findById(1L)).thenReturn(Optional.of(mockPrescription));

    InvalidOperationException exception = assertThrows(InvalidOperationException.class,
      () -> pharmacySaleService.processPrescriptionAndCreateSale(1L));

    assertTrue(exception.getMessage().contains("receita está expirada"));
  }

  @Test
  @DisplayName("Deve rejeitar o processamento se a receita já constar como processada internamente")
  void processPrescription_Fails_AlreadyProcessed() {
    mockPrescription.setProcessed(true);

    when(prescriptionCopyRepository.findById(1L)).thenReturn(Optional.of(mockPrescription));

    InvalidOperationException exception = assertThrows(InvalidOperationException.class,
      () -> pharmacySaleService.processPrescriptionAndCreateSale(1L));

    assertTrue(exception.getMessage().contains("receita já foi processada"));
  }

  @Test
  @DisplayName("Deve processar a receita, deduzir estoque, salvar venda e publicar eventos")
  void processPrescription_Success() {
    when(prescriptionCopyRepository.findById(1L)).thenReturn(Optional.of(mockPrescription));
    when(saleRepository.existsByOriginalPrescriptionId(1L)).thenReturn(false);
    when(medicineRepository.findByNameIgnoreCaseAndDosageIgnoreCase("Amoxicilina", "500mg"))
      .thenReturn(Optional.of(mockMedicine));
    when(patientReadModelRepository.findById(100L)).thenReturn(Optional.of(mockPatient));
    when(medicineRepository.findById(10L)).thenReturn(Optional.of(mockMedicine));

    // mock do retorno do serviço de inventário (Lote de onde saiu o remédio)
    when(inventoryService.sellStock(10L, 7)).thenReturn("Lote L-2023: 7");

    PharmacySale savedSale = new PharmacySale();
    savedSale.setId(500L);
    savedSale.setPatientId(100L);
    savedSale.setBuyerName("João Comprador");
    savedSale.setTotalAmount(new BigDecimal("108.50")); // 15.50 * 7
    savedSale.setSaleDate(LocalDateTime.now());
    when(saleRepository.save(any(PharmacySale.class))).thenReturn(savedSale);

    PharmacySaleResponse response = pharmacySaleService.processPrescriptionAndCreateSale(1L);

    assertNotNull(response);
    assertEquals(500L, response.id());
    assertTrue(mockPrescription.isProcessed());
    verify(prescriptionCopyRepository, times(1)).save(mockPrescription);
    verify(saleRepository, times(1)).save(any(PharmacySale.class));

    // verifica se os eventos financeiros e de notificação foram despachados
    verify(rabbitTemplate, atLeastOnce()).convertAndSend(any(), anyString(), any(EventEnvelope.class));
  }

  @Test
  @DisplayName("Deve criar uma venda avulsa direta (sem receita) com sucesso")
  void createDirectSale_Success() {
    SaleItemRequest item = new SaleItemRequest(10L, 2);
    PharmacySaleRequest request = new PharmacySaleRequest(null, 100L, List.of(item));

    when(patientReadModelRepository.findById(100L)).thenReturn(Optional.of(mockPatient));
    when(medicineRepository.findById(10L)).thenReturn(Optional.of(mockMedicine));
    when(inventoryService.sellStock(10L, 2)).thenReturn("Lote L-2023: 2");

    PharmacySale savedSale = new PharmacySale();
    savedSale.setId(501L);
    savedSale.setTotalAmount(new BigDecimal("31.00"));
    savedSale.setSaleDate(LocalDateTime.now());
    when(saleRepository.save(any(PharmacySale.class))).thenReturn(savedSale);

    PharmacySaleResponse response = pharmacySaleService.createSale(request);

    assertNotNull(response);
    verify(inventoryService, times(1)).sellStock(10L, 2);
    verify(saleRepository, times(1)).save(any(PharmacySale.class));
  }
}
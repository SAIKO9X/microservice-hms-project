package com.hms.billing.services.impl;

import com.hms.billing.clients.ProfileFeignClient;
import com.hms.billing.entities.Invoice;
import com.hms.billing.enums.InvoiceStatus;
import com.hms.billing.repositories.InsuranceProviderRepository;
import com.hms.billing.repositories.InvoiceRepository;
import com.hms.billing.repositories.PatientInsuranceRepository;
import com.hms.common.exceptions.InvalidOperationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

  @Mock
  private InvoiceRepository invoiceRepository;

  @Mock
  private PatientInsuranceRepository patientInsuranceRepository;

  @Mock
  private PdfGeneratorService pdfGeneratorService;

  @Mock
  private InsuranceProviderRepository providerRepository;

  @Mock
  private ProfileFeignClient profileClient;

  @InjectMocks
  private BillingServiceImpl billingService;

  @Captor
  private ArgumentCaptor<Invoice> invoiceCaptor;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(billingService, "self", billingService);
  }

  @Test
  @DisplayName("Deve gerar nova fatura quando não existir uma prévia para a consulta")
  void generateInvoiceForAppointment_ShouldCreate_WhenNotExists() {
    Long appointmentId = 100L;
    String patientId = "10";
    String doctorId = "5";

    when(invoiceRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.empty());

    billingService.generateInvoiceForAppointment(appointmentId, patientId, doctorId);

    verify(invoiceRepository).save(invoiceCaptor.capture());
    Invoice savedInvoice = invoiceCaptor.getValue();

    assertEquals(appointmentId, savedInvoice.getAppointmentId());
    assertEquals(new BigDecimal("200.00"), savedInvoice.getTotalAmount());
    assertEquals(InvoiceStatus.PENDING, savedInvoice.getStatus());
  }

  @Test
  @DisplayName("Não deve gerar fatura duplicada para a mesma consulta (Idempotência)")
  void generateInvoiceForAppointment_ShouldNotCreate_WhenAlreadyExists() {
    Long appointmentId = 100L;

    // simula concorrência/reprocessamento: já existe fatura para o appointment
    when(invoiceRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(new Invoice()));

    billingService.generateInvoiceForAppointment(appointmentId, "10", "5");

    verify(invoiceRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve bloquear tentativa de pagamento de fatura já paga pelo paciente")
  void payInvoice_ShouldThrowException_WhenAlreadyPaid() {
    Long invoiceId = 123L;
    Invoice existingInvoice = new Invoice();
    existingInvoice.setId(invoiceId);
    existingInvoice.setPatientPaidAt(LocalDateTime.now()); // marca como já pago

    when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(existingInvoice));

    InvalidOperationException exception = assertThrows(InvalidOperationException.class, () -> {
      billingService.payInvoice(invoiceId);
    });

    assertEquals("Esta fatura já foi paga pelo paciente.", exception.getMessage());
    verify(invoiceRepository, never()).save(any());
  }

  @Test
  @DisplayName("Deve processar pagamento do paciente e atualizar status para PAID se não houver pendência de seguro")
  void payInvoice_ShouldProcessPayment_WhenValidAndNoInsurancePending() {
    Long invoiceId = 123L;
    Invoice existingInvoice = new Invoice();
    existingInvoice.setId(invoiceId);
    existingInvoice.setPatientPayable(new BigDecimal("150.00"));
    existingInvoice.setInsuranceCovered(BigDecimal.ZERO);
    existingInvoice.setStatus(InvoiceStatus.PENDING);

    when(invoiceRepository.findById(invoiceId)).thenReturn(Optional.of(existingInvoice));
    when(invoiceRepository.save(any(Invoice.class))).thenAnswer(i -> i.getArguments()[0]);

    Invoice result = billingService.payInvoice(invoiceId);

    assertNotNull(result.getPatientPaidAt());
    // como insuranceCovered é ZERO, o checkFinalize deve mudar o status global para PAID
    assertEquals(InvoiceStatus.PAID, result.getStatus());
    assertNotNull(result.getPaidAt());

    verify(invoiceRepository).save(existingInvoice);
  }
}
import { useState } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { cn, formatCurrency } from "@/utils/utils";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import {
  ShoppingBag,
  Stethoscope,
  Loader2,
  CheckCircle2,
  Clock,
  AlertCircle,
  Download,
} from "lucide-react";
import type { Invoice } from "@/types/billing.types";
import { BillingService } from "@/services";
import { toast } from "sonner";

interface InvoicesListProps {
  invoices: Invoice[];
  onPay: (id: string) => void;
  isPaying: string | null;
}

export function InvoicesList({ invoices, onPay, isPaying }: InvoicesListProps) {
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  const handleDownloadPdf = async (id: string) => {
    try {
      setDownloadingId(id);
      await BillingService.downloadInvoicePdf(id);
      toast.success("Fatura baixada com sucesso!");
    } catch (error) {
      console.error(error);
      toast.error("Erro ao baixar fatura.");
    } finally {
      setDownloadingId(null);
    }
  };

  const getStatusBadge = (inv: Invoice) => {
    if (inv.status === "PAID") {
      return (
        <Badge className="bg-green-600 hover:bg-green-700 gap-1">
          <CheckCircle2 className="h-3 w-3" />
          Concluído
        </Badge>
      );
    }

    if (inv.patientPaidAt && inv.status === "INSURANCE_PENDING") {
      return (
        <Badge
          variant="secondary"
          className="gap-1 bg-blue-100 text-blue-800 hover:bg-blue-200"
        >
          <Clock className="h-3 w-3" />
          Aguardando Convênio
        </Badge>
      );
    }

    if (inv.status === "CANCELLED") {
      return <Badge variant="destructive">Cancelado</Badge>;
    }

    // se não pagou a parte do paciente e tem valor a pagar
    if (!inv.patientPaidAt && inv.patientPayable > 0) {
      return (
        <Badge
          variant="outline"
          className="border-orange-500 text-orange-600 bg-orange-50 gap-1"
        >
          <AlertCircle className="h-3 w-3" />
          Pagamento Pendente
        </Badge>
      );
    }

    return <Badge variant="outline">{inv.status}</Badge>;
  };

  return (
    <div className="rounded-md border bg-white shadow-sm">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Origem</TableHead>
            <TableHead>Data</TableHead>
            <TableHead>Total</TableHead>
            <TableHead className="hidden md:table-cell">Convênio</TableHead>
            <TableHead>Sua Parte</TableHead>
            <TableHead>Status</TableHead>
            <TableHead className="text-right">Ações</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {invoices.length === 0 ? (
            <TableRow>
              <TableCell
                colSpan={7}
                className="text-center py-12 text-muted-foreground"
              >
                Nenhuma fatura encontrada.
              </TableCell>
            </TableRow>
          ) : (
            invoices.map((inv) => {
              const needsPatientPayment =
                !inv.patientPaidAt && inv.patientPayable > 0;
              const isFullyPaid = inv.status === "PAID";

              const dateToDisplay = inv.createdAt
                ? new Date(inv.createdAt)
                : new Date(inv.issuedAt);

              return (
                <TableRow key={inv.id}>
                  <TableCell className="font-medium">
                    {inv.pharmacySaleId ? (
                      <div className="flex items-center gap-2 text-emerald-700 dark:text-emerald-400">
                        <div className="p-2 bg-emerald-100 rounded-full">
                          <ShoppingBag className="h-4 w-4" />
                        </div>
                        <div className="flex flex-col">
                          <span>Farmácia</span>
                          <span className="text-xs text-muted-foreground font-normal">
                            Venda #{inv.pharmacySaleId}
                          </span>
                        </div>
                      </div>
                    ) : (
                      <div className="flex items-center gap-2 text-blue-700 dark:text-blue-400">
                        <div className="p-2 bg-blue-100 rounded-full">
                          <Stethoscope className="h-4 w-4" />
                        </div>
                        <div className="flex flex-col">
                          <span>Consulta</span>
                          <span className="text-xs text-muted-foreground font-normal">
                            Ref. #{inv.appointmentId}
                          </span>
                        </div>
                      </div>
                    )}
                  </TableCell>

                  <TableCell>
                    {format(dateToDisplay, "dd/MM/yyyy", { locale: ptBR })}
                  </TableCell>

                  <TableCell className="text-muted-foreground">
                    {formatCurrency(inv.totalAmount)}
                  </TableCell>

                  <TableCell className="hidden md:table-cell text-green-600 font-medium">
                    {inv.insuranceCovered > 0 ? (
                      <span>-{formatCurrency(inv.insuranceCovered)}</span>
                    ) : (
                      <span className="text-gray-300">-</span>
                    )}
                  </TableCell>

                  <TableCell>
                    <div className="flex flex-col">
                      <span
                        className={cn(
                          "font-bold",
                          inv.patientPaidAt
                            ? "text-green-600 line-through decoration-1 opacity-70"
                            : "text-black",
                        )}
                      >
                        {formatCurrency(inv.patientPayable)}
                      </span>
                    </div>
                  </TableCell>

                  <TableCell>{getStatusBadge(inv)}</TableCell>

                  <TableCell className="text-right">
                    <div className="flex items-center justify-end gap-2">
                      <Button
                        size="icon"
                        variant="ghost"
                        className="h-8 w-8 text-muted-foreground hover:text-primary"
                        title="Baixar Fatura (PDF)"
                        onClick={() => handleDownloadPdf(String(inv.id))}
                        disabled={downloadingId === String(inv.id)}
                      >
                        {downloadingId === String(inv.id) ? (
                          <Loader2 className="h-4 w-4 animate-spin" />
                        ) : (
                          <Download className="h-4 w-4" />
                        )}
                      </Button>

                      {needsPatientPayment && inv.status !== "CANCELLED" ? (
                        <Button
                          size="sm"
                          onClick={() => onPay(String(inv.id))}
                          disabled={!!isPaying}
                          className="bg-blue-600 hover:bg-blue-700 text-white"
                        >
                          {isPaying === String(inv.id) ? (
                            <Loader2 className="h-4 w-4 animate-spin mr-2" />
                          ) : null}
                          Pagar
                        </Button>
                      ) : inv.pharmacySaleId && isFullyPaid ? (
                        <span className="text-xs text-muted-foreground bg-gray-100 px-2 py-1 rounded">
                          Pago na Loja
                        </span>
                      ) : (
                        <Button
                          size="sm"
                          variant="ghost"
                          disabled
                          className="opacity-50"
                        >
                          {isFullyPaid
                            ? "Liquidado"
                            : inv.patientPaidAt
                              ? "Processando"
                              : "-"}
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              );
            })
          )}
        </TableBody>
      </Table>
    </div>
  );
}

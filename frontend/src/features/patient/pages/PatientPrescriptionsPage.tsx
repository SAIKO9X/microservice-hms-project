import { useState } from "react";
import { useMyPrescriptionsHistory } from "@/services/queries/appointment-queries";
import { downloadPrescriptionPdf } from "@/services/appointment";
import { Link } from "react-router";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import {
  ArrowLeft,
  Pill,
  Calendar,
  FileText,
  AlertCircle,
  ChevronLeft,
  ChevronRight,
  Clock,
  CheckCircle,
  Ban,
  Download,
  Loader2,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import { format, addDays } from "date-fns";
import { ptBR } from "date-fns/locale";
import { cn } from "@/utils/utils";
import { toast } from "sonner";

const getStatusConfig = (status: string) => {
  switch (status) {
    case "ISSUED":
      return {
        label: "Disponível",
        icon: Clock,
        className:
          "bg-blue-100 text-blue-700 hover:bg-blue-200 border-blue-200",
      };
    case "DISPENSED":
      return {
        label: "Aviada (Comprada)",
        icon: CheckCircle,
        className:
          "bg-green-100 text-green-700 hover:bg-green-200 border-green-200",
      };
    case "CANCELLED":
      return {
        label: "Cancelada",
        icon: Ban,
        className: "bg-red-100 text-red-700 hover:bg-red-200 border-red-200",
      };
    default:
      return {
        label: status,
        icon: AlertCircle,
        className: "bg-gray-100 text-gray-700",
      };
  }
};

export const PatientPrescriptionsPage = () => {
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  const { data: prescriptionsPage, isLoading } = useMyPrescriptionsHistory(
    page,
    pageSize,
  );

  const prescriptions = prescriptionsPage?.content || [];

  const handleDownloadPdf = async (id: number) => {
    try {
      setDownloadingId(id);
      await downloadPrescriptionPdf(id);
      toast.success("Download iniciado!");
    } catch (error) {
      console.error(error);
      toast.error("Erro ao baixar o PDF da receita.");
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="container mx-auto py-8 space-y-6">
      <div className="space-y-4">
        <Button asChild variant="ghost" size="sm" className="gap-2">
          <Link to="/patient/dashboard">
            <ArrowLeft className="h-4 w-4" />
            Voltar ao Dashboard
          </Link>
        </Button>

        <div className="flex items-start gap-4">
          <div className="p-3 bg-primary/10 rounded-lg">
            <FileText className="h-8 w-8 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold">Minhas Prescrições</h1>
            <p className="text-muted-foreground mt-1">
              Histórico completo de todos os seus tratamentos médicos
            </p>
          </div>
        </div>
      </div>

      {!isLoading && prescriptions.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  Total de Prescrições
                </p>
                <FileText className="h-4 w-4 text-muted-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {prescriptionsPage?.totalElements || prescriptions.length}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  Medicamentos (Pág.)
                </p>
                <Pill className="h-4 w-4 text-muted-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {prescriptions.reduce((acc, p) => acc + p.medicines.length, 0)}
              </p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="pb-3">
              <div className="flex items-center justify-between">
                <p className="text-sm text-muted-foreground">
                  Mais Recente (Pág.)
                </p>
                <Calendar className="h-4 w-4 text-muted-foreground" />
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-2xl font-bold">
                {prescriptions[0]?.createdAt
                  ? format(new Date(prescriptions[0].createdAt), "dd/MM/yy", {
                      locale: ptBR,
                    })
                  : "-"}
              </p>
            </CardContent>
          </Card>
        </div>
      )}

      <Card>
        <CardHeader>
          <h2 className="text-xl font-semibold">Histórico de Prescrições</h2>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              <Skeleton className="h-20 w-full" />
              <Skeleton className="h-20 w-full" />
              <Skeleton className="h-20 w-full" />
            </div>
          ) : (!prescriptions || prescriptions.length === 0) && page === 0 ? (
            // só mostra o estado vazio total se for a primeira página e não tiver nada
            <div className="flex flex-col items-center justify-center py-12 text-center">
              <div className="p-4 bg-muted rounded-full mb-4">
                <AlertCircle className="h-8 w-8 text-muted-foreground" />
              </div>
              <h3 className="text-lg font-semibold mb-2">
                Nenhuma prescrição encontrada
              </h3>
              <p className="text-sm text-muted-foreground max-w-md">
                Você ainda não possui prescrições médicas registradas no
                sistema. Elas aparecerão aqui após suas consultas.
              </p>
            </div>
          ) : (
            <div className="space-y-6">
              {prescriptions && prescriptions.length > 0 ? (
                <Accordion type="single" collapsible className="w-full">
                  {prescriptions.map((p) => {
                    const statusConfig = getStatusConfig(p.status);
                    const StatusIcon = statusConfig.icon;

                    return (
                      <AccordionItem key={p.id} value={`item-${p.id}`}>
                        <AccordionTrigger className="hover:no-underline">
                          <div className="flex items-center justify-between w-full pr-4">
                            <div className="flex items-center gap-4">
                              <div className="p-2 bg-primary/10 rounded-lg">
                                <Calendar className="h-4 w-4 text-primary" />
                              </div>
                              <div className="text-left">
                                <p className="font-semibold">
                                  {format(
                                    new Date(p.createdAt),
                                    "dd 'de' MMMM 'de' yyyy",
                                    { locale: ptBR },
                                  )}
                                </p>
                                <div className="flex items-center gap-2 mt-1">
                                  <p className="text-sm text-muted-foreground">
                                    {format(new Date(p.createdAt), "HH:mm", {
                                      locale: ptBR,
                                    })}
                                  </p>

                                  <Badge
                                    variant="outline"
                                    className={cn(
                                      "gap-1 ml-2",
                                      statusConfig.className,
                                    )}
                                  >
                                    <StatusIcon className="h-3 w-3" />
                                    {statusConfig.label}
                                  </Badge>
                                </div>
                              </div>
                            </div>

                            <div className="flex items-center gap-2 ml-auto mr-2">
                              <Badge variant="secondary">
                                {p.medicines.length}{" "}
                                {p.medicines.length === 1 ? "item" : "itens"}
                              </Badge>
                            </div>
                          </div>
                        </AccordionTrigger>

                        <AccordionContent className="pt-4">
                          <div className="flex flex-col space-y-4">
                            <div className="flex justify-end border-b pb-4 mb-2">
                              <Button
                                variant="outline"
                                size="sm"
                                className="gap-2 text-primary border-primary/20 hover:bg-primary/5"
                                onClick={() => handleDownloadPdf(p.id)}
                                disabled={downloadingId === p.id}
                              >
                                {downloadingId === p.id ? (
                                  <Loader2 className="h-4 w-4 animate-spin" />
                                ) : (
                                  <Download className="h-4 w-4" />
                                )}
                                Baixar PDF da Receita
                              </Button>
                            </div>

                            {p.status === "DISPENSED" && (
                              <div className="p-3 bg-green-50 border border-green-200 rounded text-sm text-green-800 flex items-center gap-2">
                                <CheckCircle className="h-4 w-4" />
                                Esta receita já foi utilizada e os medicamentos
                                dispensados.
                              </div>
                            )}

                            <div className="space-y-4 pl-4">
                              <div className="space-y-3">
                                <h4 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                                  Medicamentos Prescritos
                                </h4>
                                {p.medicines.map((med, index) => (
                                  <div
                                    key={index}
                                    className="flex items-start gap-3 p-4 bg-muted/50 rounded-lg border border-border"
                                  >
                                    <div className="p-2 bg-green-100 dark:bg-green-900/20 rounded-lg">
                                      <Pill className="h-5 w-5 text-green-600 dark:text-green-400" />
                                    </div>
                                    <div className="flex-1 space-y-1">
                                      <p className="font-semibold text-base">
                                        {med.name}
                                      </p>
                                      <div className="flex flex-wrap gap-2 text-sm text-muted-foreground">
                                        <span className="flex items-center gap-1">
                                          <strong>Dosagem:</strong> {med.dosage}
                                        </span>
                                        <span>•</span>
                                        <span className="flex items-center gap-1">
                                          <strong>Frequência:</strong>{" "}
                                          {med.frequency}
                                        </span>
                                        <span>•</span>
                                        <span className="flex items-center gap-1">
                                          <strong>Duração:</strong>{" "}
                                          {med.duration} dias
                                        </span>
                                        <span className="flex items-center gap-1 text-amber-600 dark:text-amber-400">
                                          <AlertCircle className="h-3 w-3" />
                                          <strong>Válido até:</strong>{" "}
                                          {format(
                                            addDays(new Date(p.createdAt), 30),
                                            "dd/MM/yyyy",
                                            { locale: ptBR },
                                          )}
                                        </span>
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>

                              {p.notes && (
                                <div className="p-4 bg-blue-50 dark:bg-blue-900/10 border border-blue-200 dark:border-blue-900/20 rounded-lg">
                                  <div className="flex gap-2">
                                    <FileText className="h-5 w-5 text-blue-600 dark:text-blue-400 flex-shrink-0 mt-0.5" />
                                    <div>
                                      <p className="font-semibold text-sm text-blue-900 dark:text-blue-100 mb-1">
                                        Observações do Médico
                                      </p>
                                      <p className="text-sm text-blue-800 dark:text-blue-200">
                                        {p.notes}
                                      </p>
                                    </div>
                                  </div>
                                </div>
                              )}
                            </div>
                          </div>
                        </AccordionContent>
                      </AccordionItem>
                    );
                  })}
                </Accordion>
              ) : (
                <div className="flex flex-col items-center justify-center py-12 text-center text-muted-foreground">
                  Nenhuma prescrição nesta página.
                </div>
              )}

              <div className="flex items-center justify-end space-x-2 py-4">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage((old) => Math.max(0, old - 1))}
                  disabled={page === 0 || isLoading}
                >
                  <ChevronLeft className="h-4 w-4" />
                  Anterior
                </Button>
                <div className="text-sm text-muted-foreground">
                  Página {page + 1} de{" "}
                  {Math.max(1, prescriptionsPage?.totalPages || 1)}
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() =>
                    setPage((old) =>
                      !prescriptionsPage ||
                      old >= prescriptionsPage.totalPages - 1
                        ? old
                        : old + 1,
                    )
                  }
                  disabled={
                    !prescriptionsPage ||
                    page >= prescriptionsPage.totalPages - 1 ||
                    prescriptions.length === 0 || // desabilita o "Próxima" se a página atual já estiver vazia
                    isLoading
                  }
                >
                  Próxima
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};

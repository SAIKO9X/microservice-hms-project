import { useState } from "react";
import { Plus, CalendarDays } from "lucide-react";
import {
  useAppointmentsWithDoctorNames,
  useCancelAppointment,
  useCreateAppointment,
} from "@/services/queries/appointment-queries";
import { DataTable } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import { columns } from "@/features/patient/components/appointmentsColumns";
import type { AppointmentFormData } from "@/schemas/appointment.schema";
import { CustomNotification } from "@/components/notifications/CustomNotification";
import { CreateAppointmentDialog } from "../components/CreateAppointmentDialog";
import { useNavigate } from "react-router";

const STATS = [
  {
    status: "SCHEDULED",
    label: "Agendadas",
    color: "text-blue-600",
    bg: "bg-blue-50 dark:bg-blue-950/30",
    border: "border-blue-100 dark:border-blue-900",
  },
  {
    status: "COMPLETED",
    label: "Concluídas",
    color: "text-green-600",
    bg: "bg-green-50 dark:bg-green-950/30",
    border: "border-green-100 dark:border-green-900",
  },
  {
    status: "CANCELED",
    label: "Canceladas",
    color: "text-red-600",
    bg: "bg-red-50 dark:bg-red-950/30",
    border: "border-red-100 dark:border-red-900",
  },
  {
    status: null,
    label: "Total",
    color: "text-zinc-500",
    bg: "bg-muted",
    border: "border-border",
  },
];

export const PatientAppointmentsPage = () => {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [notification, setNotification] = useState<{
    show: boolean;
    variant: "success" | "error" | "info";
    title: string;
    description?: string;
  }>({
    show: false,
    variant: "info",
    title: "",
  });

  const navigate = useNavigate();

  const {
    data: appointments,
    isLoading,
    isError,
    error,
  } = useAppointmentsWithDoctorNames();
  const createAppointmentMutation = useCreateAppointment();
  const cancelAppointmentMutation = useCancelAppointment();

  const handleCreateAppointment = async (data: AppointmentFormData) => {
    try {
      await createAppointmentMutation.mutateAsync(data);
      setNotification({
        show: true,
        variant: "success",
        title: "Consulta agendada com sucesso!",
        description:
          "Sua consulta foi agendada. Você receberá uma confirmação em breve.",
      });
      setIsDialogOpen(false);
    } catch (error: any) {
      let errorMessage =
        "Houve um problema ao agendar sua consulta. Tente novamente.";
      if (error?.response?.status === 409) {
        errorMessage = "Este horário já está ocupado. Escolha outro horário.";
      } else if (error?.response?.status === 404) {
        errorMessage =
          "Médico não encontrado. Atualize a página e tente novamente.";
      } else if (error?.response?.data?.message) {
        errorMessage = error.response.data.message;
      }
      setNotification({
        show: true,
        variant: "error",
        title: "Erro ao agendar consulta",
        description: errorMessage,
      });
    }
  };

  const handleCancelAppointment = async (appointmentId: number) => {
    try {
      await cancelAppointmentMutation.mutateAsync(appointmentId);
      setNotification({
        show: true,
        variant: "success",
        title: "Consulta cancelada com sucesso!",
      });
    } catch (err: any) {
      setNotification({
        show: true,
        variant: "error",
        title: "Erro ao cancelar consulta",
        description: err.message || "Não foi possível cancelar a consulta.",
      });
    }
  };

  const dismissNotification = () => {
    setNotification((prev) => ({ ...prev, show: false }));
  };

  if (isLoading) {
    return (
      <div className="container mx-auto py-6 space-y-6">
        <div className="flex items-center justify-between">
          <div className="space-y-2">
            <Skeleton className="h-8 w-48" />
            <Skeleton className="h-4 w-64" />
          </div>
          <Skeleton className="h-10 w-36" />
        </div>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-20 w-full rounded-lg" />
          ))}
        </div>
        <Skeleton className="h-64 w-full rounded-lg" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="container mx-auto py-6">
        <div className="flex items-center justify-center h-64">
          <div className="text-center space-y-4">
            <div className="text-destructive text-lg font-semibold">
              Erro ao carregar consultas
            </div>
            <p className="text-muted-foreground">
              {error instanceof Error
                ? error.message
                : "Ocorreu um erro inesperado"}
            </p>
            <Button variant="outline" onClick={() => window.location.reload()}>
              Tentar novamente
            </Button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="container mx-auto py-6 space-y-6">
      {notification.show && (
        <CustomNotification
          variant={notification.variant}
          title={notification.title}
          description={notification.description}
          onDismiss={dismissNotification}
          autoHide
          autoHideDelay={5000}
        />
      )}

      {/* Header */}
      <div className="flex flex-col space-y-4 md:flex-row md:items-center md:justify-between md:space-y-0">
        <div className="flex items-center gap-3">
          <div className="p-2 bg-primary/10 rounded-lg">
            <CalendarDays className="h-6 w-6 text-primary" />
          </div>
          <div>
            <h1 className="text-3xl font-bold text-foreground">
              Minhas Consultas
            </h1>
            <p className="text-muted-foreground">
              Gerencie e acompanhe suas consultas médicas
            </p>
          </div>
        </div>
        <Button
          onClick={() => setIsDialogOpen(true)}
          className="w-full md:w-auto"
        >
          <Plus className="mr-2 h-4 w-4" />
          Agendar Consulta
        </Button>
      </div>

      {/* Stats cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {STATS.map(({ status, label, color, bg, border }) => (
          <div
            key={label}
            className={`${bg} ${border} p-4 rounded-lg border shadow-sm`}
          >
            <div className={`text-2xl font-bold ${color}`}>
              {status
                ? (appointments?.filter((a) => a.status === status).length ?? 0)
                : (appointments?.length ?? 0)}
            </div>
            <div className="text-sm text-muted-foreground mt-0.5">{label}</div>
          </div>
        ))}
      </div>

      {/* Table */}
      <div className="bg-card rounded-lg border shadow-sm">
        <DataTable
          columns={columns({
            handleCancelAppointment,
            handleViewDetails: (id) => navigate(`/patient/appointments/${id}`),
          })}
          data={appointments || []}
          emptyMessage="Nenhuma consulta encontrada"
          emptyDescription="Agende sua primeira consulta clicando no botão acima"
        />
      </div>

      <CreateAppointmentDialog
        open={isDialogOpen}
        onOpenChange={setIsDialogOpen}
        onSubmit={handleCreateAppointment}
        isPending={createAppointmentMutation.isPending}
      />
    </div>
  );
};

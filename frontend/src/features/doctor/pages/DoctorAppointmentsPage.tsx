import { useState } from "react";
import {
  useDoctorAppointmentDetails,
  useCancelAppointment,
  useCompleteAppointment,
} from "@/services/queries/appointment-queries";
import { DataTable } from "@/components/ui/data-table";
import { Button } from "@/components/ui/button";
import { CustomNotification } from "@/components/notifications/CustomNotification";
import { columns } from "@/features/doctor/components/columns";

export const DoctorAppointmentsPage = () => {
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

  const {
    data: appointments,
    isLoading,
    isError,
    error,
  } = useDoctorAppointmentDetails();
  const completeAppointmentMutation = useCompleteAppointment();
  const cancelAppointmentMutation = useCancelAppointment();

  const handleCompleteAppointment = async (
    appointmentId: number,
    notes: string,
  ) => {
    try {
      await completeAppointmentMutation.mutateAsync({
        id: appointmentId,
        notes,
      });
      setNotification({
        show: true,
        variant: "success",
        title: "Consulta finalizada com sucesso!",
      });
    } catch (error: any) {
      setNotification({
        show: true,
        variant: "error",
        title: "Erro ao finalizar consulta",
        description: error.message || "Ocorreu um erro inesperado",
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
      <div className="container mx-auto py-6">
        <p className="text-muted-foreground mt-4">
          Carregando suas consultas...
        </p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="container mx-auto py-6 text-center">
        <CustomNotification
          variant="error"
          title={error?.message || "Erro ao carregar as consultas"}
        />
        <Button onClick={() => window.location.reload()} className="mt-4">
          Tentar Novamente
        </Button>
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

      <div className="flex flex-col md:flex-row md:items-center md:justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">
            Minhas Consultas
          </h1>
          <p className="text-muted-foreground">
            Visualize e gerencie as consultas agendadas com você.
          </p>
        </div>
      </div>

      <div className="bg-card rounded-lg border shadow-sm">
        <DataTable
          columns={columns({
            handleCompleteAppointment,
            handleCancelAppointment,
          })}
          data={appointments || []}
        />
      </div>
    </div>
  );
};

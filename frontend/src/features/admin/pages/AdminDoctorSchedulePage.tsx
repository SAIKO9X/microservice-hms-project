import { useParams, useNavigate } from "react-router";
import { useDoctorById } from "@/services/queries/profile-queries";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowLeft, Calendar } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { DataTable } from "@/components/ui/data-table";
import { useAdminDoctorAppointments } from "@/services/queries/admin-queries";

import { columns } from "../components/dashboard/doctorAppointmentColumns";

export const AdminDoctorSchedulePage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const doctorProfileId = Number(id);

  const { data: doctor, isLoading: isLoadingDoctor } =
    useDoctorById(doctorProfileId);

  const { data: appointments, isLoading: isLoadingAppointments } =
    useAdminDoctorAppointments(doctor?.userId);

  const isLoading = isLoadingDoctor || isLoadingAppointments;

  // a agenda mostra tudo o que NÃO está "Concluído"
  const doctorSchedule = appointments?.filter(
    (app) => app.status !== "COMPLETED",
  );

  return (
    <div className="container mx-auto py-8 space-y-6">
      <div className="flex items-center gap-4">
        <Button variant="outline" size="icon" onClick={() => navigate(-1)}>
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          {isLoadingDoctor ? (
            <Skeleton className="h-9 w-64" />
          ) : (
            <h1 className="text-3xl font-bold">Agenda de {doctor?.name}</h1>
          )}
          <p className="text-muted-foreground">
            Visualize as consultas agendadas, canceladas ou pendentes.
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Calendar className="h-5 w-5" />
            Consultas Agendadas
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-64 w-full" />
          ) : (
            <DataTable columns={columns} data={doctorSchedule || []} />
          )}
        </CardContent>
      </Card>
    </div>
  );
};

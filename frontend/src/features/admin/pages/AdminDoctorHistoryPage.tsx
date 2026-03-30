import { useParams, Link } from "react-router";
import { useDoctorById } from "@/services/queries/profile-queries";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ArrowLeft, History } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { DataTable } from "@/components/ui/data-table";
import { useAdminDoctorAppointments } from "@/services/queries/admin-queries";
import { columns } from "../components/dashboard/doctorAppointmentColumns";

export const AdminDoctorHistoryPage = () => {
  const { id } = useParams<{ id: string }>();
  const doctorProfileId = Number(id);

  const { data: doctor, isLoading: isLoadingDoctor } =
    useDoctorById(doctorProfileId);

  const { data: appointments, isLoading: isLoadingAppointments } =
    useAdminDoctorAppointments(doctor?.userId);

  const isLoading = isLoadingDoctor || isLoadingAppointments;

  const doctorHistory = appointments?.filter(
    (app) => app.status === "COMPLETED"
  );

  return (
    <div className="container mx-auto py-8 space-y-6">
      <div className="flex items-center gap-4">
        <Button asChild variant="outline" size="icon">
          <Link to={`/admin/users/doctor/${doctorProfileId}`}>
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          {isLoadingDoctor ? (
            <Skeleton className="h-9 w-64" />
          ) : (
            <h1 className="text-3xl font-bold">
              Histórico de {doctor?.name}
            </h1>
          )}
          <p className="text-muted-foreground">
            Visualize o histórico de consultas concluídas.
          </p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <History className="h-5 w-5" />
            Consultas Concluídas
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <Skeleton className="h-64 w-full" />
          ) : (
            <DataTable columns={columns} data={doctorHistory || []} />
          )}
        </CardContent>
      </Card>
    </div>
  );
};

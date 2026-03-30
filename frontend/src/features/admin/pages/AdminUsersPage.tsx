import { useState, useMemo } from "react";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Search, Phone, Calendar, Plus } from "lucide-react";
import {
  useAllPatients,
  useAllDoctors,
} from "@/services/queries/profile-queries";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import type { PatientProfile } from "@/types/patient.types";
import type { DoctorProfile } from "@/types/doctor.types";
import {
  doctorColumns,
  patientColumns,
} from "@/features/admin/components/users/userColumns";
import { useNavigate } from "react-router";
import { AddEditUserDialog } from "../components/users/AddEditUserDialog";
import { useAllUsers } from "@/services/queries/admin-queries";
import {
  isValidNotification,
  type ActionNotification,
} from "@/types/notification.types";
import { CustomNotification } from "@/components/notifications/CustomNotification";
import { DataList } from "@/components/shared/DataList";

const PatientCard = ({ patient }: { patient: PatientProfile }) => {
  const navigate = useNavigate();
  return (
    <Card className="hover:shadow-lg transition-shadow h-full flex flex-col justify-between">
      <div>
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
                <span className="text-lg font-semibold text-primary">
                  {patient.name?.charAt(0).toUpperCase() || "?"}
                </span>
              </div>
              <div>
                <h3 className="font-semibold text-lg">{patient.name}</h3>
                <p className="text-sm text-muted-foreground">
                  CPF: {patient.cpf || "Não informado"}
                </p>
              </div>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {patient.dateOfBirth && (
            <div className="flex items-center gap-2 text-sm">
              <Calendar className="h-4 w-4 text-muted-foreground" />
              <span>
                {format(new Date(patient.dateOfBirth), "dd/MM/yyyy", {
                  locale: ptBR,
                })}
              </span>
            </div>
          )}
          {patient.phoneNumber && (
            <div className="flex items-center gap-2 text-sm">
              <Phone className="h-4 w-4 text-muted-foreground" />
              <span>{patient.phoneNumber}</span>
            </div>
          )}
        </CardContent>
      </div>
      <CardContent className="pt-0">
        <Button
          variant="outline"
          className="w-full"
          onClick={() => navigate(`/admin/users/patient/${patient.id}`)}
        >
          Ver Detalhes
        </Button>
      </CardContent>
    </Card>
  );
};

const DoctorCard = ({ doctor }: { doctor: DoctorProfile }) => {
  const navigate = useNavigate();
  return (
    <Card className="hover:shadow-lg transition-shadow h-full flex flex-col justify-between">
      <div>
        <CardHeader className="pb-3">
          <div className="flex items-start justify-between">
            <div className="flex items-center gap-3">
              <div className="h-12 w-12 rounded-full bg-primary/10 flex items-center justify-center">
                <span className="text-lg font-semibold text-primary">
                  {doctor.name?.charAt(0).toUpperCase() || "?"}
                </span>
              </div>
              <div>
                <h3 className="font-semibold text-lg">{doctor.name}</h3>
                <p className="text-sm text-muted-foreground">
                  CRM: {doctor.crmNumber || "Não informado"}
                </p>
              </div>
            </div>
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {doctor.specialization && (
            <div className="flex items-center gap-2 text-sm">
              <span className="font-medium">Especialidade:</span>
              <span className="text-muted-foreground">
                {doctor.specialization}
              </span>
            </div>
          )}
          {doctor.phoneNumber && (
            <div className="flex items-center gap-2 text-sm">
              <Phone className="h-4 w-4 text-muted-foreground" />
              <span>{doctor.phoneNumber}</span>
            </div>
          )}
        </CardContent>
      </div>
      <CardContent className="pt-0">
        <Button
          variant="outline"
          className="w-full"
          onClick={() => navigate(`/admin/users/doctor/${doctor.id}`)}
        >
          Ver Detalhes
        </Button>
      </CardContent>
    </Card>
  );
};

type PatientWithDetails = PatientProfile & { email?: string; active?: boolean };
type DoctorWithDetails = DoctorProfile & { email?: string; active?: boolean };

export default function AdminUsersPage() {
  const [searchPatients, setSearchPatients] = useState("");
  const [searchDoctors, setSearchDoctors] = useState("");
  const [isDialogOpen, setDialogOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<
    PatientWithDetails | DoctorWithDetails | null
  >(null);
  const [editingUserType, setEditingUserType] = useState<"patient" | "doctor">(
    "patient",
  );
  const [notification, setNotification] = useState<ActionNotification | null>(
    null,
  );

  const { data: patients, isLoading: isLoadingPatients } = useAllPatients();
  const { data: doctors, isLoading: isLoadingDoctors } = useAllDoctors();
  const { data: users, isLoading: isLoadingUsers } = useAllUsers();

  const handleCreateUser = () => {
    setEditingUser(null);
    setEditingUserType("patient");
    setDialogOpen(true);
  };

  const handleEditPatient = (patient: PatientWithDetails) => {
    setEditingUser(patient);
    setEditingUserType("patient");
    setDialogOpen(true);
  };

  const handleEditDoctor = (doctor: DoctorWithDetails) => {
    setEditingUser(doctor);
    setEditingUserType("doctor");
    setDialogOpen(true);
  };

  const patientsWithDetails = useMemo<PatientWithDetails[]>(() => {
    if (
      !patients ||
      !Array.isArray(patients) ||
      !users ||
      !Array.isArray(users)
    )
      return [];

    return patients.map((patient: PatientProfile) => {
      const user = users.find((u) => u.id === patient.userId);
      return {
        ...patient,
        email: user?.email || "N/A",
        active: user?.active ?? false,
      };
    });
  }, [patients, users]);

  const doctorsWithDetails = useMemo<DoctorWithDetails[]>(() => {
    if (!doctors || !Array.isArray(doctors) || !users || !Array.isArray(users))
      return [];

    return doctors.map((doctor: DoctorProfile) => {
      const user = users.find((u) => u.id === doctor.userId);
      return {
        ...doctor,
        email: user?.email || "N/A",
        active: user?.active ?? false,
      };
    });
  }, [doctors, users]);

  const filteredPatients = useMemo<PatientWithDetails[]>(() => {
    if (!patientsWithDetails) return [];
    const searchTerm = searchPatients.toLowerCase();
    return patientsWithDetails.filter(
      (patient) =>
        patient.name?.toLowerCase().includes(searchTerm) ||
        patient.email?.toLowerCase().includes(searchTerm) ||
        patient.cpf?.replace(/\D/g, "").includes(searchTerm.replace(/\D/g, "")),
    );
  }, [patientsWithDetails, searchPatients]);

  const filteredDoctors = useMemo<DoctorWithDetails[]>(() => {
    if (!doctorsWithDetails) return [];
    const searchTerm = searchDoctors.toLowerCase();
    return doctorsWithDetails.filter(
      (doctor) =>
        doctor.name?.toLowerCase().includes(searchTerm) ||
        doctor.email?.toLowerCase().includes(searchTerm) ||
        doctor.crmNumber?.toLowerCase().includes(searchTerm) ||
        doctor.specialization?.toLowerCase().includes(searchTerm),
    );
  }, [doctorsWithDetails, searchDoctors]);

  const isLoading = isLoadingPatients || isLoadingDoctors || isLoadingUsers;

  return (
    <div className="container mx-auto py-8 space-y-6">
      <div className="fixed top-24 right-4 z-50 w-full max-w-sm">
        {isValidNotification(notification) && (
          <CustomNotification
            key={Date.now()}
            variant={notification.variant}
            title={notification.title}
            description={notification.description}
            onDismiss={() => setNotification(null)}
            autoHide
            autoHideDelay={5000}
          />
        )}
      </div>

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold">Gestão de Utilizadores</h1>
          <p className="text-muted-foreground">
            Visualize todos os pacientes e médicos do sistema.
          </p>
        </div>
        <Button onClick={handleCreateUser}>
          <Plus className="h-4 w-4 mr-2" />
          Criar Utilizador
        </Button>
      </div>

      <Tabs defaultValue="patients" className="space-y-6">
        <TabsList>
          <TabsTrigger value="patients">
            Pacientes ({filteredPatients?.length || 0})
          </TabsTrigger>
          <TabsTrigger value="doctors">
            Médicos ({filteredDoctors?.length || 0})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="patients">
          <DataList
            data={filteredPatients}
            isLoading={isLoading}
            renderCard={(patient) => (
              <PatientCard key={patient.id} patient={patient} />
            )}
            toolbar={
              <div className="relative flex-1 max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Pesquisar por nome ou CPF..."
                  value={searchPatients}
                  onChange={(e) => setSearchPatients(e.target.value)}
                  className="pl-9"
                />
              </div>
            }
            columns={patientColumns({
              onEdit: handleEditPatient,
              setNotification: setNotification,
            })}
            emptyMessage="Nenhum paciente encontrado."
          />
        </TabsContent>

        <TabsContent value="doctors">
          <DataList
            data={filteredDoctors}
            isLoading={isLoading}
            renderCard={(doctor) => (
              <DoctorCard key={doctor.id} doctor={doctor} />
            )}
            toolbar={
              <div className="relative flex-1 max-w-sm">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="Pesquisar por nome, CRM ou especialidade..."
                  value={searchDoctors}
                  onChange={(e) => setSearchDoctors(e.target.value)}
                  className="pl-9"
                />
              </div>
            }
            columns={doctorColumns({
              onEdit: handleEditDoctor,
              setNotification: setNotification,
            })}
            emptyMessage="Nenhum médico encontrado."
          />
        </TabsContent>
      </Tabs>

      <AddEditUserDialog
        open={isDialogOpen}
        onOpenChange={setDialogOpen}
        user={editingUser}
        userType={editingUserType}
        setNotification={setNotification}
      />
    </div>
  );
}

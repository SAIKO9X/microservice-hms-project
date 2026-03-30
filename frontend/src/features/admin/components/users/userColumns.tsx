import type { ColumnDef } from "@tanstack/react-table";
import {
  MoreHorizontal,
  ArrowUpDown,
  Pencil,
  User,
  HeartPulse,
  Calendar,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Badge } from "@/components/ui/badge";
import { format } from "date-fns";
import { ptBR } from "date-fns/locale";
import type { PatientProfile } from "@/types/patient.types";
import type { DoctorProfile } from "@/types/doctor.types";
import { useUpdateUserStatus } from "@/services/queries/admin-queries";
import {
  createErrorNotification,
  createSuccessNotification,
  type ActionNotification,
} from "@/types/notification.types";
import { getErrorMessage } from "@/utils/utils";
import { useNavigate } from "react-router";

const StatusBadge = ({ isActive }: { isActive: boolean }) => {
  return (
    <Badge
      variant={isActive ? "secondary" : "destructive"}
      className={
        isActive ? "bg-green-100 text-green-800" : "bg-red-100 text-red-800"
      }
    >
      {isActive ? "Ativo" : "Inativo"}
    </Badge>
  );
};

type PatientWithDetails = PatientProfile & { email?: string; active?: boolean };
type DoctorWithDetails = DoctorProfile & { email?: string; active?: boolean };

interface PatientColumnsOptions {
  onEdit: (patient: PatientWithDetails) => void;
  setNotification: (notification: ActionNotification | null) => void;
}

// --- Colunas para Pacientes ---
export const patientColumns = ({
  onEdit,
  setNotification,
}: PatientColumnsOptions): ColumnDef<PatientWithDetails>[] => [
  {
    accessorKey: "name",
    header: ({ column }) => (
      <Button
        variant="ghost"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Nome <ArrowUpDown className="ml-2 h-4 w-4" />
      </Button>
    ),
    cell: ({ row }) => (
      <div className="flex items-center gap-3">
        <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
          <span className="text-sm font-semibold text-primary">
            {row.getValue<string>("name").charAt(0).toUpperCase()}
          </span>
        </div>
        <span className="font-medium">{row.getValue("name")}</span>
      </div>
    ),
  },
  {
    accessorKey: "email",
    header: ({ column }) => (
      <Button
        variant="ghost"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Email <ArrowUpDown className="ml-2 h-4 w-4" />
      </Button>
    ),
  },
  {
    accessorKey: "cpf",
    header: "CPF",
  },
  {
    accessorKey: "dateOfBirth",
    header: ({ column }) => (
      <Button
        variant="ghost"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Data de Nascimento <ArrowUpDown className="ml-2 h-4 w-4" />
      </Button>
    ),
    cell: ({ row }) => {
      const date = row.getValue<string>("dateOfBirth");
      if (!date) return <span className="text-muted-foreground">-</span>;
      return format(new Date(date), "dd/MM/yyyy", { locale: ptBR });
    },
  },
  {
    accessorKey: "phoneNumber",
    header: "Telefone",
    cell: ({ row }) => {
      const phone = row.getValue<string>("phoneNumber");
      return phone || <span className="text-muted-foreground">-</span>;
    },
  },
  {
    accessorKey: "bloodGroup",
    header: "Tipo Sanguíneo",
    cell: ({ row }) => {
      const bloodGroup = row.getValue<string>("bloodGroup");
      if (!bloodGroup) return <span className="text-muted-foreground">-</span>;
      return (
        <Badge
          variant="outline"
          className="bg-red-50 text-red-700 border-red-200"
        >
          {bloodGroup}
        </Badge>
      );
    },
  },
  {
    accessorKey: "gender",
    header: "Gênero",
    cell: ({ row }) => {
      const gender = row.getValue<string>("gender");
      const genderLabels: Record<string, string> = {
        MALE: "Masculino",
        FEMALE: "Feminino",
        OTHER: "Outro",
      };
      return gender ? (
        genderLabels[gender] || gender
      ) : (
        <span className="text-muted-foreground">-</span>
      );
    },
  },
  {
    accessorKey: "active",
    header: "Status",
    cell: ({ row }) => <StatusBadge isActive={row.getValue("active")} />,
  },
  {
    id: "actions",
    cell: ({ row }) => {
      const patient = row.original as PatientWithDetails;
      const navigate = useNavigate();
      const { mutate: updateUserStatus, isPending } = useUpdateUserStatus();

      const handleToggleStatus = () => {
        const newStatus = !patient.active;
        updateUserStatus(
          { userId: patient.userId, active: newStatus },
          {
            onSuccess: () => {
              setNotification(
                createSuccessNotification(
                  `Utilizador ${
                    newStatus ? "ativado" : "desativado"
                  } com sucesso!`,
                ),
              );
            },
            onError: (error: any) => {
              setNotification(
                createErrorNotification(
                  "Erro ao atualizar status",
                  getErrorMessage(error),
                ),
              );
            },
          },
        );
      };

      return (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="h-8 w-8 p-0">
              <span className="sr-only">Abrir menu</span>
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>

          <DropdownMenuContent align="end">
            <DropdownMenuLabel>Ações</DropdownMenuLabel>

            <DropdownMenuItem
              onClick={() => navigate(`/admin/users/patient/${patient.id}`)}
            >
              <User className="mr-2 h-4 w-4" />
              Ver Perfil Completo
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() =>
                navigate(`/admin/users/patient/${patient.id}/history`)
              }
            >
              <HeartPulse className="mr-2 h-4 w-4" />
              Histórico Médico
            </DropdownMenuItem>

            <DropdownMenuSeparator />

            <DropdownMenuItem onClick={() => onEdit(patient)}>
              <Pencil className="mr-2 h-4 w-4" />
              Editar Perfil
            </DropdownMenuItem>

            <DropdownMenuSeparator />

            <DropdownMenuItem
              onClick={handleToggleStatus}
              disabled={isPending}
              className="text-destructive"
            >
              {isPending
                ? "A atualizar..."
                : patient.active
                  ? "Desativar"
                  : "Ativar"}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      );
    },
  },
];

// --- Colunas para Médicos ---
interface DoctorColumnsOptions {
  onEdit: (doctor: DoctorWithDetails) => void;
  setNotification: (notification: ActionNotification | null) => void;
}

export const doctorColumns = ({
  onEdit,
  setNotification,
}: DoctorColumnsOptions): ColumnDef<DoctorWithDetails>[] => [
  {
    accessorKey: "name",
    header: ({ column }) => (
      <Button
        variant="ghost"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Nome <ArrowUpDown className="ml-2 h-4 w-4" />
      </Button>
    ),
    cell: ({ row }) => (
      <div className="flex items-center gap-3">
        <div className="h-8 w-8 rounded-full bg-primary/10 flex items-center justify-center">
          <span className="text-sm font-semibold text-primary">
            {row.getValue<string>("name").charAt(0).toUpperCase()}
          </span>
        </div>
        <span className="font-medium">{row.getValue("name")}</span>
      </div>
    ),
  },
  {
    accessorKey: "email",
    header: ({ column }) => (
      <Button
        variant="ghost"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Email <ArrowUpDown className="ml-2 h-4 w-4" />
      </Button>
    ),
  },
  {
    accessorKey: "crmNumber",
    header: "CRM",
  },
  {
    accessorKey: "specialization",
    header: ({ column }) => (
      <Button
        variant="ghost"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Especialidade <ArrowUpDown className="ml-2 h-4 w-4" />
      </Button>
    ),
    cell: ({ row }) => {
      const spec = row.getValue<string>("specialization");
      if (!spec) return <span className="text-muted-foreground">-</span>;
      return (
        <Badge
          variant="outline"
          className="bg-blue-50 text-blue-700 border-blue-200"
        >
          {spec}
        </Badge>
      );
    },
  },
  {
    accessorKey: "department",
    header: "Departamento",
    cell: ({ row }) => {
      const dept = row.getValue<string>("department");
      return dept || <span className="text-muted-foreground">-</span>;
    },
  },
  {
    accessorKey: "phoneNumber",
    header: "Telefone",
    cell: ({ row }) => {
      const phone = row.getValue<string>("phoneNumber");
      return phone || <span className="text-muted-foreground">-</span>;
    },
  },
  {
    accessorKey: "yearsOfExperience",
    header: ({ column }) => (
      <Button
        variant="ghost"
        onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
      >
        Experiência <ArrowUpDown className="ml-2 h-4 w-4" />
      </Button>
    ),
    cell: ({ row }) => {
      const years = row.getValue<number>("yearsOfExperience");
      if (!years) return <span className="text-muted-foreground">-</span>;
      return `${years} anos`;
    },
  },
  {
    accessorKey: "active",
    header: "Status",
    cell: ({ row }) => <StatusBadge isActive={row.getValue("active")} />,
  },
  {
    id: "actions",
    cell: ({ row }) => {
      const doctor = row.original as DoctorWithDetails;
      const navigate = useNavigate();
      const { mutate: updateUserStatus, isPending } = useUpdateUserStatus();

      const handleToggleStatus = () => {
        const newStatus = !doctor.active;
        updateUserStatus(
          { userId: doctor.userId, active: newStatus },
          {
            onSuccess: () => {
              setNotification(
                createSuccessNotification(
                  `Utilizador ${
                    newStatus ? "ativado" : "desativado"
                  } com sucesso!`,
                ),
              );
            },
            onError: (error: any) => {
              setNotification(
                createErrorNotification(
                  "Erro ao atualizar status",
                  getErrorMessage(error),
                ),
              );
            },
          },
        );
      };

      return (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" className="h-8 w-8 p-0">
              <span className="sr-only">Abrir menu</span>
              <MoreHorizontal className="h-4 w-4" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuLabel>Ações</DropdownMenuLabel>

            <DropdownMenuItem
              onClick={() => navigate(`/admin/users/doctor/${doctor.id}`)}
            >
              <User className="mr-2 h-4 w-4" />
              Ver Perfil Completo
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() =>
                navigate(`/admin/users/doctor/${doctor.id}/schedule`)
              }
            >
              <Calendar className="mr-2 h-4 w-4" />
              Ver Agenda
            </DropdownMenuItem>

            <DropdownMenuItem onClick={() => onEdit(doctor)}>
              <Pencil className="mr-2 h-4 w-4" />
              Editar Perfil
            </DropdownMenuItem>

            <DropdownMenuSeparator />

            <DropdownMenuItem
              onClick={handleToggleStatus}
              disabled={isPending}
              className="text-destructive"
            >
              {isPending
                ? "A atualizar..."
                : doctor.active
                  ? "Desativar"
                  : "Ativar"}
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      );
    },
  },
];
